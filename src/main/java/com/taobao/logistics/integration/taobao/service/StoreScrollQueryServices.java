package com.taobao.logistics.integration.taobao.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaXsdItemStoreScrollQueryRequest;
import com.taobao.api.response.AlibabaXsdItemStoreScrollQueryResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 拉取淘宝小时达门店商品资料
 */
@Slf4j
@Service
public class StoreScrollQueryServices {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String URL = "http://gw.api.taobao.com/router/rest";
    private static final String SESSION_KEY = LogisticsConfig.SESSIONKEY;
    private static final Long STORE_ID = 1157535260L;
    private static final Long PAGE_SIZE = 50L; // 每页大小，可根据需要调整

    /**
     * 拉取所有商品数据并存入数据库
     */
    @Transactional
    public void fetchAndSaveAllItems() throws ApiException {
        String scrollId = null;
        boolean hasMore = true;
        int totalProcessed = 0;

        TaobaoClient client = new DefaultTaobaoClient(URL, LogisticsConfig.XSDAPP_KEY, LogisticsConfig.XSDAPP_SECRET);

        while (hasMore) {
            AlibabaXsdItemStoreScrollQueryRequest req = createRequest(scrollId);
            AlibabaXsdItemStoreScrollQueryResponse rsp = client.execute(req, SESSION_KEY);

            if (!rsp.isSuccess()) {
                log.error("API调用失败: {}", rsp.getBody());
                throw new ApiException("API调用失败: " + rsp.getBody());
            }

            JSONObject responseJson = JSON.parseObject(rsp.getBody());
            JSONObject result = responseJson.getJSONObject("alibaba_xsd_item_store_scroll_query_response")
                    .getJSONObject("result");

            // 解析商品数据
            List<ItemSku> itemSkus = parseItemData(result);

            // 批量合并到数据库
            if (!itemSkus.isEmpty()) {
                batchMergeItemSkus(itemSkus);
                totalProcessed += itemSkus.size();
                log.info("已处理 {} 条SKU记录", totalProcessed);
            }

            // 检查是否还有更多数据
            scrollId = result.getString("scroll_id");
            hasMore = scrollId != null && !scrollId.trim().isEmpty() && itemSkus.size() == PAGE_SIZE;
        }

        log.info("数据拉取完成，共处理 {} 条SKU记录", totalProcessed);
    }

    /**
     * 创建API请求
     */
    private AlibabaXsdItemStoreScrollQueryRequest createRequest(String scrollId) {
        AlibabaXsdItemStoreScrollQueryRequest req = new AlibabaXsdItemStoreScrollQueryRequest();
        AlibabaXsdItemStoreScrollQueryRequest.XsdItemScrollQueryRequest obj1 =
                new AlibabaXsdItemStoreScrollQueryRequest.XsdItemScrollQueryRequest();

        obj1.setStoreId(STORE_ID);
        obj1.setPageSize(PAGE_SIZE);

        if (scrollId != null) {
            obj1.setScrollId(scrollId);
        }

        req.setXsdItemScrollQueryRequest(obj1);
        return req;
    }

    /**
     * 解析商品数据
     */
    private List<ItemSku> parseItemData(JSONObject result) {
        List<ItemSku> itemSkus = new ArrayList<>();

        if (result != null && result.getBooleanValue("success")) {
            JSONObject items = result.getJSONObject("items");
            if (items != null) {
                JSONArray itemArray = items.getJSONArray("xsd_item_result_d_t_o");
                if (itemArray != null && !itemArray.isEmpty()) {
                    for (int i = 0; i < itemArray.size(); i++) {
                        JSONObject item = itemArray.getJSONObject(i);
                        String itemId = item.getString("item_id");
                        String outerItemId = item.getString("outer_item_id");
                        Integer onlineSaleFlag = item.getInteger("online_sale_flag");
                        String storeId = item.getString("store_id");
                        Integer prepareTime = item.getInteger("prepare_time");

                        // 处理SKU列表
                        JSONObject skuList = item.getJSONObject("sku_list");
                        if (skuList != null) {
                            JSONArray skuArray = skuList.getJSONArray("xsd_sku_result_d_t_o");
                            if (skuArray != null && !skuArray.isEmpty()) {
                                for (int j = 0; j < skuArray.size(); j++) {
                                    JSONObject sku = skuArray.getJSONObject(j);
                                    String skuId = sku.getString("sku_id");
                                    String outerSkuId = sku.getString("outer_sku_id");
                                    ItemSku itemSku = new ItemSku();
                                    itemSku.setItemId(itemId);
                                    itemSku.setOuterItemId(outerItemId);
                                    itemSku.setOnlineSaleFlag(onlineSaleFlag);
                                    itemSku.setStoreId(storeId);
                                    itemSku.setPrepareTime(prepareTime);
                                    itemSku.setSkuId(skuId);
                                    itemSku.setOuterSkuId(outerSkuId);
                                    itemSkus.add(itemSku);
                                }
                            }
                        }
                    }
                }
            }
        }

        return itemSkus;
    }

    /**
     * 批量合并SKU数据（使用MERGE INTO）
     */
    private void batchMergeItemSkus(List<ItemSku> itemSkus) {
        String sql = "MERGE INTO xsd_item_sku t " +
                "USING (SELECT ? AS outer_sku_id, ? AS sku_id, ? AS outer_item_id, " +
                "? AS item_id, ? AS online_sale_flag, ? AS store_id, ? AS prepare_time FROM dual) s " +
                "ON (t.sku_id = s.sku_id) " +
                "WHEN MATCHED THEN " +
                "  UPDATE SET t.outer_sku_id = s.outer_sku_id, " +
                "             t.outer_item_id = s.outer_item_id, " +
                "             t.item_id = s.item_id, " +
                "             t.online_sale_flag = s.online_sale_flag, " +
                "             t.store_id = s.store_id, " +
                "             t.prepare_time = s.prepare_time, " +
                "             t.update_time = SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN " +
                "  INSERT (id,outer_sku_id, sku_id, outer_item_id, item_id, online_sale_flag, store_id, prepare_time, create_time, update_time) " +
                "  VALUES (get_sequences('xsd_item_sku'),s.outer_sku_id, s.sku_id, s.outer_item_id, s.item_id, s.online_sale_flag, s.store_id, s.prepare_time, SYSTIMESTAMP, SYSTIMESTAMP)";
        // SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        // dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        // dataSource.setUrl("jdbc:oracle:thin:@10.100.21.151:1521/orcl");
        // dataSource.setUsername("neands3");
        // dataSource.setPassword("abc123");
        // // 创建JdbcTemplate实例
        // JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                ItemSku itemSku = itemSkus.get(i);
                ps.setString(1, itemSku.getOuterSkuId());
                ps.setString(2, itemSku.getSkuId());
                ps.setString(3, itemSku.getOuterItemId());
                ps.setString(4, itemSku.getItemId());
                ps.setInt(5, itemSku.getOnlineSaleFlag());
                ps.setString(6, itemSku.getStoreId());
                ps.setInt(7, itemSku.getPrepareTime());
            }

            @Override
            public int getBatchSize() {
                return itemSkus.size();
            }
        });
    }

    /**
     * SKU数据实体类
     */
    private static class ItemSku {
        private String outerSkuId;
        private String skuId;
        private String outerItemId;
        private String itemId;
        private Integer onlineSaleFlag;
        private String storeId;
        private Integer prepareTime;

        // getter和setter方法
        public String getOuterSkuId() { return outerSkuId; }
        public void setOuterSkuId(String outerSkuId) { this.outerSkuId = outerSkuId; }

        public String getSkuId() { return skuId; }
        public void setSkuId(String skuId) { this.skuId = skuId; }

        public String getOuterItemId() { return outerItemId; }
        public void setOuterItemId(String outerItemId) { this.outerItemId = outerItemId; }

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public Integer getOnlineSaleFlag() { return onlineSaleFlag; }
        public void setOnlineSaleFlag(Integer onlineSaleFlag) { this.onlineSaleFlag = onlineSaleFlag; }

        public String getStoreId() { return storeId; }
        public void setStoreId(String storeId) { this.storeId = storeId; }

        public Integer getPrepareTime() { return prepareTime; }
        public void setPrepareTime(Integer prepareTime) { this.prepareTime = prepareTime; }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            // 这里需要配置Spring上下文，实际使用时可以通过Spring容器获取Bean
             StoreScrollQueryServices service = new StoreScrollQueryServices();
             service.fetchAndSaveAllItems();
           // System.out.println("请配置Spring上下文后使用");
        } catch (Exception e) {
            log.error("数据拉取失败", e);
        }
    }
}