package com.taobao.logistics.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaXsdStoresQueryRequest;
import com.taobao.api.response.AlibabaXsdStoresQueryResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 淘宝小时达门店查询服务
 * 优化后的版本支持获取所有分页数据并存储到Oracle数据库
 *
 */
@Slf4j
@Service
public class StoresQueryServices {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String URL = "http://gw.api.taobao.com/router/rest";
    private static final String SESSION_KEY = "6102912579c7e2db03f113a1f0f73b8ae8ea04b348cd2bd1757633411";

    @Autowired
    private LogisticsConfig logisticsConfig;

    /**
     * 获取所有门店数据
     * @return 包含所有门店的JSON数组
     */
    public JSONArray getAllStores() throws ApiException {
        String appkey = logisticsConfig.XSDAPP_KEY;
        String secret = logisticsConfig.XSDAPP_SECRET;

        TaobaoClient client = new DefaultTaobaoClient(URL, appkey, secret);
        List<JSONObject> allStores = new ArrayList<>();
        String scrollId = null;
        int totalStores = 0;
        int fetchedStores = 0;

        log.info("开始获取淘宝小时达门店数据");

        do {
            AlibabaXsdStoresQueryRequest req = new AlibabaXsdStoresQueryRequest();
            AlibabaXsdStoresQueryRequest.XsdStoreQueryRequest obj1 =
                    new AlibabaXsdStoresQueryRequest.XsdStoreQueryRequest();

            obj1.setPageSize(20L);
            if (scrollId != null) {
                obj1.setScrollId(scrollId);
            }

            req.setXsdStoreQueryReqeust(obj1);
            AlibabaXsdStoresQueryResponse rsp = client.execute(req, SESSION_KEY);

            if (!rsp.isSuccess()) {
                log.error("API请求失败: {}", rsp.getSubMsg());
                throw new ApiException(rsp.getSubMsg());
            }

            // 解析响应
            String responseBody = rsp.getBody();
            JSONObject responseJson = JSON.parseObject(responseBody);
            JSONObject result = responseJson.getJSONObject("alibaba_xsd_stores_query_response")
                    .getJSONObject("result");

            // 首次请求获取总门店数
            if (totalStores == 0) {
                totalStores = result.getIntValue("total");
                log.info("总门店数: {}", totalStores);
            }

            // 获取本次返回的门店列表
            JSONObject storeList = result.getJSONObject("store_list");
            if (storeList != null) {
                JSONArray stores = storeList.getJSONArray("xsd_store_result_d_t_o");
                if (stores != null && !stores.isEmpty()) {
                    for (int i = 0; i < stores.size(); i++) {
                        allStores.add(stores.getJSONObject(i));
                    }
                    fetchedStores += stores.size();
                    log.info("已获取 {}/{} 条门店数据", fetchedStores, totalStores);
                }
            }

            // 获取下一页的scroll_id
            scrollId = result.getString("scroll_id");

            // 添加延迟避免API限制
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("线程被中断", e);
            }

        } while (scrollId != null && !scrollId.isEmpty() && fetchedStores < totalStores);

        log.info("门店数据获取完成，共获取 {} 条数据", allStores.size());

        // 修复：将List<JSONObject>转换为JSONArray
        JSONArray resultArray = new JSONArray();
        resultArray.addAll(allStores);
        return resultArray;
    }
    /**
     * 更新门店对应伯俊门店ID
     */
    public void updateStoresToOracle() {
        String sql = "update   xsd_stores b set b.c_store_id= (\n" +
                "select id from c_store@bj_70 a where a.name=b.store_name)\n" +
                "where b.c_store_id is null" ;
       SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setUrl("jdbc:oracle:thin:@10.100.21.151:1521/orcl");
        dataSource.setUsername("neands3");
        dataSource.setPassword("abc123");
        // 创建JdbcTemplate实例
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        int[] updateCounts = jdbcTemplate.batchUpdate(sql);
         }
    /**
     * 将门店数据保存到Oracle数据库
     */
    public void saveStoresToOracle(JSONArray stores) {
        if (stores == null || stores.isEmpty()) {
            log.info("没有门店数据需要保存");
            return;
        }

        // 先检查表是否存在，不存在则创建
        //createTableIfNotExists();

        String sql = "MERGE INTO xsd_stores s " +
                "USING (SELECT ? as store_id, ? as store_name, ? as address, ? as city, " +
                "? as category_name, ? as business_day, ? as business_time, ? as lat, ? as lng, " +
                "? as contact, ? as phone, ? as status FROM dual) t " +
                "ON (s.store_id = t.store_id) " +
                "WHEN MATCHED THEN " +
                "  UPDATE SET s.store_name = t.store_name, s.address = t.address, s.city = t.city, " +
                "  s.category_name = t.category_name, s.business_day = t.business_day, " +
                "  s.business_time = t.business_time, s.lat = t.lat, s.lng = t.lng, " +
                "  s.contact = t.contact, s.phone = t.phone, s.status = t.status, " +
                "  s.update_time = SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN " +
                "  INSERT (id,store_id, store_name, address, city, category_name, business_day, " +
                "  business_time, lat, lng, contact, phone, status, create_time, update_time) " +
                "  VALUES (get_sequences('xsd_stores'), t.store_id, t.store_name, t.address, t.city, t.category_name, " +
                "  t.business_day, t.business_time, t.lat, t.lng, t.contact, t.phone, " +
                "  t.status, SYSTIMESTAMP, SYSTIMESTAMP)";

        try {
            SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
            dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
            dataSource.setUrl("jdbc:oracle:thin:@10.100.21.151:1521/orcl");
            dataSource.setUsername("neands3");
            dataSource.setPassword("abc123");
            // 创建JdbcTemplate实例
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            int[] updateCounts = jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    JSONObject store = stores.getJSONObject(i);

                    // 添加空值检查
                    if (store == null) {
                        log.warn("第 {} 个门店数据为空，跳过", i);
                        // 设置默认值或跳过
                        return;
                    }

                    // 安全获取字段值，避免空指针
                    String storeId = store.getString("store_id");
                    String storeName = store.getString("store_name");
                    String address = store.getString("address");
                    String city = store.getString("city");
                    String categoryName = store.getString("category_name");
                    String businessDay = store.getString("business_day");
                    String businessTime = store.getString("business_time");
                    String latStr = store.getString("lat");
                    String lngStr = store.getString("lng");
                    String contact = store.getString("contact");
                    String phone = store.getString("phone");
                    Integer status = store.getInteger("status");

                    // 设置参数，处理可能的空值
                    ps.setString(1, storeId != null ? storeId : "");
                    ps.setString(2, storeName != null ? storeName : "");
                    ps.setString(3, address != null ? address : "");
                    ps.setString(4, city != null ? city : "");
                    ps.setString(5, categoryName != null ? categoryName : "");
                    ps.setString(6, businessDay != null ? businessDay : "");
                    ps.setString(7, businessTime != null ? businessTime : "");

                    // 处理可能为空的经纬度
                    if (latStr != null && !latStr.isEmpty()) {
                        try {
                            ps.setBigDecimal(8, new BigDecimal(latStr));
                        } catch (NumberFormatException e) {
                            log.warn("纬度格式错误: {}", latStr);
                            ps.setNull(8, java.sql.Types.DECIMAL);
                        }
                    } else {
                        ps.setNull(8, java.sql.Types.DECIMAL);
                    }

                    if (lngStr != null && !lngStr.isEmpty()) {
                        try {
                            ps.setBigDecimal(9, new BigDecimal(lngStr));
                        } catch (NumberFormatException e) {
                            log.warn("经度格式错误: {}", lngStr);
                            ps.setNull(9, java.sql.Types.DECIMAL);
                        }
                    } else {
                        ps.setNull(9, java.sql.Types.DECIMAL);
                    }

                    ps.setString(10, contact != null ? contact : "");
                    ps.setString(11, phone != null ? phone : "");
                    ps.setInt(12, status != null ? status : -1); // 使用-1表示未知状态
                }

                @Override
                public int getBatchSize() {
                    return stores.size();
                }
            });

            log.info("成功保存 {} 条门店数据到Oracle数据库", updateCounts.length);
        } catch (Exception e) {
            log.error("保存数据到Oracle数据库时发生异常", e);
            // 可以添加重试机制或更详细的错误处理
        }
    }

    /**
     * 创建表（如果不存在）
     */
    private void createTableIfNotExists() {
        try {
            String checkTableSql = "SELECT COUNT(*) FROM user_tables WHERE table_name = 'XSD_STORES'";
            Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (count != null && count == 0) {
                // 表不存在，创建表
                String createTableSql = "CREATE TABLE xsd_stores (" +
                        "id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "store_id VARCHAR2(50) NOT NULL UNIQUE, " +
                        "store_name VARCHAR2(200), " +
                        "address VARCHAR2(500), " +
                        "city VARCHAR2(100), " +
                        "category_name VARCHAR2(100), " +
                        "business_day VARCHAR2(50), " +
                        "business_time VARCHAR2(50), " +
                        "lat NUMBER(10, 6), " +
                        "lng NUMBER(10, 6), " +
                        "contact VARCHAR2(50), " +
                        "phone VARCHAR2(50), " +
                        "status NUMBER(2), " +
                        "create_time TIMESTAMP DEFAULT SYSTIMESTAMP, " +
                        "update_time TIMESTAMP DEFAULT SYSTIMESTAMP" +
                        ")";

                jdbcTemplate.execute(createTableSql);

                // 创建索引以提高查询性能
                jdbcTemplate.execute("CREATE INDEX idx_xsd_stores_city ON xsd_stores(city)");
                jdbcTemplate.execute("CREATE INDEX idx_xsd_stores_status ON xsd_stores(status)");

                log.info("成功创建XSD_STORES表及相关索引");
            }
        } catch (Exception e) {
            log.warn("检查或创建表时发生异常: {}", e.getMessage());
            // 可能是权限问题或表已存在但名称大小写不同
            // 可以尝试直接创建表，如果已存在会抛出异常
        }
    }

    /**
     * 完整的门店数据获取流程
     */
    public void completeStoreFetchProcess() {
        try {
            JSONArray allStores = getAllStores();
            if (allStores != null && !allStores.isEmpty()) {
                log.info("开始保存 {} 条门店数据到数据库", allStores.size());
                saveStoresToOracle(allStores);
                log.info("门店数据获取和保存流程完成");
                updateStoresToOracle();
                log.info("门店与伯俊门店ID关联已做刷新完成");
            } else {
                log.info("未获取到门店数据，跳过保存步骤");
            }
        } catch (ApiException e) {
            log.error("获取门店数据时发生API异常", e);
        } catch (Exception e) {
            log.error("获取门店数据时发生未知异常", e);
        }
    }

    // 测试方法 - 添加更详细的调试信息
    public static void main(String[] args) {
        try {
            StoresQueryServices service = new StoresQueryServices();
//            SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
//            dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
//            dataSource.setUrl("jdbc:oracle:thin:@10.100.21.181:1521/orcl");
//            dataSource.setUsername("neands3");
//            dataSource.setPassword("abc123");
//
//            // 创建JdbcTemplate实例
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            // 测试数据库连接
//            try {
//                String testSql = "SELECT 1 FROM DUAL";
//                Integer result = jdbcTemplate.queryForObject(testSql, Integer.class);
//                log.info("数据库连接测试成功: {}", result);
//            } catch (Exception e) {
//                log.error("数据库连接测试失败", e);
//                return;
//            }

            // 执行完整流程
            service.completeStoreFetchProcess();
        } catch (Exception e) {
            log.error("测试失败", e);
            e.printStackTrace();
        }
    }
}