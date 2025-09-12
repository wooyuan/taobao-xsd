package com.taobao.logistics.integration.taobao.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.internal.util.StringUtils;
import com.taobao.api.request.TradesSoldGetRequest;
import com.taobao.api.response.TradesSoldGetResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 淘宝小时达订单获取服务
 */
@Slf4j
@Service
public class SoldgetServices {
    @Autowired
    private JdbcTemplate jdbcTemplate;


    private static final String URL = "http://gw.api.taobao.com/router/rest";

    // MERGE INTO SQL语句
    private static final String MERGE_ORDER_SQL = "MERGE INTO TAOBAO_ORDERS t " +
            "USING (SELECT ? as OID, ? as TID, ? as STATUS, ? as PAYMENT, ? as TOTAL_FEE, ? as DISCOUNT_FEE, " +
            "? as ADJUST_FEE, ? as NUM, ? as NUM_IID, ? as TITLE, ? as PRICE, ? as SKU_PROPERTIES_NAME, " +
            "? as OUTER_IID, ? as OUTER_SKU_ID, ? as REFUND_STATUS, ? as LOGISTICS_COMPANY, ? as INVOICE_NO, " +
            "? as CONSIGN_TIME, ? as END_TIME, ? as NO_SHIPPING, ? as OAID, ? as BUYER_RATE, ? as SELLER_RATE, " +
            "? as SELLER_TYPE, ? as SHIPPING_TYPE, ? as STORE_CODE, ? as CID, ? as IS_DAIXIAO, ? as IS_IDLE, " +
            "? as PART_MJZ_DISCOUNT, ? as DIVIDE_ORDER_FEE, ? as PIC_PATH, ? as SKU_ID, ? as PROMISE_COLLECT_TIME, " +
            "? as REFUND_ID FROM dual) s " +
            "ON (t.OID = s.OID) " +
            "WHEN MATCHED THEN " +
            "  UPDATE SET " +
            "    t.TID = s.TID, " +
            "    t.STATUS = s.STATUS, " +
            "    t.PAYMENT = s.PAYMENT, " +
            "    t.TOTAL_FEE = s.TOTAL_FEE, " +
            "    t.DISCOUNT_FEE = s.DISCOUNT_FEE, " +
            "    t.ADJUST_FEE = s.ADJUST_FEE, " +
            "    t.NUM = s.NUM, " +
            "    t.NUM_IID = s.NUM_IID, " +
            "    t.TITLE = s.TITLE, " +
            "    t.PRICE = s.PRICE, " +
            "    t.SKU_PROPERTIES_NAME = s.SKU_PROPERTIES_NAME, " +
            "    t.OUTER_IID = s.OUTER_IID, " +
            "    t.OUTER_SKU_ID = s.OUTER_SKU_ID, " +
            "    t.REFUND_STATUS = s.REFUND_STATUS, " +
            "    t.LOGISTICS_COMPANY = s.LOGISTICS_COMPANY, " +
            "    t.INVOICE_NO = s.INVOICE_NO, " +
            "    t.CONSIGN_TIME = s.CONSIGN_TIME, " +
            "    t.END_TIME = s.END_TIME, " +
            "    t.NO_SHIPPING = s.NO_SHIPPING, " +
            "    t.OAID = s.OAID, " +
            "    t.BUYER_RATE = s.BUYER_RATE, " +
            "    t.SELLER_RATE = s.SELLER_RATE, " +
            "    t.SELLER_TYPE = s.SELLER_TYPE, " +
            "    t.SHIPPING_TYPE = s.SHIPPING_TYPE, " +
            "    t.STORE_CODE = s.STORE_CODE, " +
            "    t.CID = s.CID, " +
            "    t.IS_DAIXIAO = s.IS_DAIXIAO, " +
            "    t.IS_IDLE = s.IS_IDLE, " +
            "    t.PART_MJZ_DISCOUNT = s.PART_MJZ_DISCOUNT, " +
            "    t.DIVIDE_ORDER_FEE = s.DIVIDE_ORDER_FEE, " +
            "    t.PIC_PATH = s.PIC_PATH, " +
            "    t.SKU_ID = s.SKU_ID, " +
            "    t.PROMISE_COLLECT_TIME = s.PROMISE_COLLECT_TIME, " +
            "    t.REFUND_ID = s.REFUND_ID, " +
            "    t.MODIFIED_TIME = SYSTIMESTAMP " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (ID,TID, OID, STATUS, PAYMENT, TOTAL_FEE, DISCOUNT_FEE, ADJUST_FEE, NUM, NUM_IID, " +
            "          TITLE, PRICE, SKU_PROPERTIES_NAME, OUTER_IID, OUTER_SKU_ID, REFUND_STATUS, " +
            "          LOGISTICS_COMPANY, INVOICE_NO, CONSIGN_TIME, END_TIME, NO_SHIPPING, OAID, " +
            "          BUYER_RATE, SELLER_RATE, SELLER_TYPE, SHIPPING_TYPE, STORE_CODE, CID, " +
            "          IS_DAIXIAO, IS_IDLE, PART_MJZ_DISCOUNT, DIVIDE_ORDER_FEE, PIC_PATH, SKU_ID, " +
            "          PROMISE_COLLECT_TIME, REFUND_ID, CREATED_TIME, MODIFIED_TIME) " +
            "  VALUES (get_sequences('TAOBAO_ORDERS'),s.TID, s.OID, s.STATUS, s.PAYMENT, s.TOTAL_FEE, s.DISCOUNT_FEE, s.ADJUST_FEE, " +
            "          s.NUM, s.NUM_IID, s.TITLE, s.PRICE, s.SKU_PROPERTIES_NAME, s.OUTER_IID, s.OUTER_SKU_ID, " +
            "          s.REFUND_STATUS, s.LOGISTICS_COMPANY, s.INVOICE_NO, s.CONSIGN_TIME, s.END_TIME, " +
            "          s.NO_SHIPPING, s.OAID, s.BUYER_RATE, s.SELLER_RATE, s.SELLER_TYPE, s.SHIPPING_TYPE, " +
            "          s.STORE_CODE, s.CID, s.IS_DAIXIAO, s.IS_IDLE, s.PART_MJZ_DISCOUNT, s.DIVIDE_ORDER_FEE, " +
            "          s.PIC_PATH, s.SKU_ID, s.PROMISE_COLLECT_TIME, s.REFUND_ID, SYSTIMESTAMP, SYSTIMESTAMP)";

    /**
     * 获取并存储订单数据
     */
    public void fetchAndStoreOrders() throws ApiException {
        int pageNo = 1;
        boolean hasNext = true;
        int totalOrders = 0;

        TaobaoClient client = new DefaultTaobaoClient(URL, LogisticsConfig.XSDAPP_KEY, LogisticsConfig.XSDAPP_SECRET);
        String sessionKey = LogisticsConfig.SESSIONKEY;

        while (hasNext) {
            log.info("正在获取第 {} 页订单数据", pageNo);

            TradesSoldGetRequest req = buildRequest(pageNo);
            TradesSoldGetResponse rsp = client.execute(req, sessionKey);

            if (!rsp.isSuccess()) {
                log.error("获取订单数据失败: {}", rsp.getSubMsg());
                throw new ApiException(rsp.getSubMsg());
            }

            JSONObject responseJson = JSON.parseObject(rsp.getBody());
            JSONObject tradesResponse = responseJson.getJSONObject("trades_sold_get_response");

            if (tradesResponse == null) {
                log.error("响应格式不正确: {}", rsp.getBody());
                break;
            }

            hasNext = tradesResponse.getBooleanValue("has_next");
            JSONObject trades = tradesResponse.getJSONObject("trades");

            if (trades != null) {
                List<JSONObject> orderList = parseOrders(trades);
                totalOrders += orderList.size();

                if (!orderList.isEmpty()) {
                    batchMergeOrders(orderList);
                    log.info("已成功存储 {} 条订单数据", orderList.size());
                }
            }

            pageNo++;

            // 添加短暂延迟，避免API限制
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("线程被中断", e);
            }
        }

        log.info("订单数据获取完成，共获取 {} 条订单", totalOrders);
    }

    /**
     * 构建请求对象
     */
    private TradesSoldGetRequest buildRequest(int pageNo) {
        TradesSoldGetRequest req = new TradesSoldGetRequest();
        req.setFields("tid,type,status,payment,orders,no_shipping,oaid");
        req.setStartCreated(StringUtils.parseDateTime("2025-08-27 00:00:00"));
        req.setEndCreated(StringUtils.parseDateTime("2025-08-28 23:59:59"));
        req.setType("fixed");
        req.setPageNo((long) pageNo);
        req.setPageSize(40L);
        req.setUseHasNext(true);
        return req;
    }

    /**
     * 解析订单数据
     */
    private List<JSONObject> parseOrders(JSONObject trades) {
        List<JSONObject> orderList = new ArrayList<>();
        JSONArray tradeArray = trades.getJSONArray("trade");

        if (tradeArray == null || tradeArray.isEmpty()) {
            return orderList;
        }

        for (int i = 0; i < tradeArray.size(); i++) {
            JSONObject trade = tradeArray.getJSONObject(i);
            JSONObject orders = trade.getJSONObject("orders");

            if (orders != null) {
                JSONArray orderArray = orders.getJSONArray("order");

                for (int j = 0; j < orderArray.size(); j++) {
                    JSONObject order = orderArray.getJSONObject(j);
                    // 添加父订单信息到子订单
                    order.put("parent_tid", trade.getString("tid"));
                    order.put("parent_status", trade.getString("status"));
                    order.put("parent_payment", trade.getString("payment"));
                    order.put("no_shipping", trade.getString("no_shipping"));
                    order.put("oaid", trade.getString("oaid"));

                    orderList.add(order);
                }
            }
        }

        return orderList;
    }

    /**
     * 批量合并订单数据到Oracle（使用MERGE INTO）
     */
    private void batchMergeOrders(List<JSONObject> orderList) {
        // StoresQueryServices service = new StoresQueryServices();
        // SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        // dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        // dataSource.setUrl("jdbc:oracle:thin:@10.100.21.151:1521/orcl");
        // dataSource.setUsername("neands3");
        // dataSource.setPassword("abc123");
        // 创建JdbcTemplate实例
        //JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.batchUpdate(MERGE_ORDER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                JSONObject order = orderList.get(i);
                int paramIndex = 1;

                // 设置USING子句中的参数
                ps.setString(paramIndex++, order.getString("oid")); // OID
                ps.setString(paramIndex++, order.getString("parent_tid")); // TID
                ps.setString(paramIndex++, order.getString("status")); // STATUS
                ps.setBigDecimal(paramIndex++, order.getBigDecimal("payment")); // PAYMENT
                ps.setBigDecimal(paramIndex++, order.getBigDecimal("total_fee")); // TOTAL_FEE
                ps.setBigDecimal(paramIndex++, order.getBigDecimal("discount_fee")); // DISCOUNT_FEE
                ps.setBigDecimal(paramIndex++, order.getBigDecimal("adjust_fee")); // ADJUST_FEE
                ps.setInt(paramIndex++, order.getIntValue("num")); // NUM
                ps.setLong(paramIndex++, order.getLongValue("num_iid")); // NUM_IID
                ps.setString(paramIndex++, order.getString("title")); // TITLE
                ps.setBigDecimal(paramIndex++, order.getBigDecimal("price")); // PRICE
                ps.setString(paramIndex++, order.getString("sku_properties_name")); // SKU_PROPERTIES_NAME
                ps.setString(paramIndex++, order.getString("outer_iid")); // OUTER_IID
                ps.setString(paramIndex++, order.getString("outer_sku_id")); // OUTER_SKU_ID
                ps.setString(paramIndex++, order.getString("refund_status")); // REFUND_STATUS
                ps.setString(paramIndex++, order.getString("logistics_company")); // LOGISTICS_COMPANY
                ps.setString(paramIndex++, order.getString("invoice_no")); // INVOICE_NO

                // 处理时间字段
                String consignTime = order.getString("consign_time");
                if (consignTime != null) {
                    ps.setTimestamp(paramIndex++, Timestamp.valueOf(consignTime));
                } else {
                    ps.setNull(paramIndex++, java.sql.Types.TIMESTAMP);
                }

                String endTime = order.getString("end_time");
                if (endTime != null) {
                    ps.setTimestamp(paramIndex++, Timestamp.valueOf(endTime));
                } else {
                    ps.setNull(paramIndex++, java.sql.Types.TIMESTAMP);
                }

                ps.setString(paramIndex++, order.getString("no_shipping")); // NO_SHIPPING
                ps.setString(paramIndex++, order.getString("oaid")); // OAID
                ps.setString(paramIndex++, order.getString("buyer_rate")); // BUYER_RATE
                ps.setString(paramIndex++, order.getString("seller_rate")); // SELLER_RATE
                ps.setString(paramIndex++, order.getString("seller_type")); // SELLER_TYPE
                ps.setString(paramIndex++, order.getString("shipping_type")); // SHIPPING_TYPE
                ps.setString(paramIndex++, order.getString("store_code")); // STORE_CODE
                ps.setLong(paramIndex++, order.getLongValue("cid")); // CID
                ps.setString(paramIndex++, order.getString("is_daixiao")); // IS_DAIXIAO
                ps.setString(paramIndex++, order.getString("is_idle")); // IS_IDLE
                ps.setBigDecimal(paramIndex++, order.getBigDecimal("part_mjz_discount")); // PART_MJZ_DISCOUNT
                ps.setBigDecimal(paramIndex++, order.getBigDecimal("divide_order_fee")); // DIVIDE_ORDER_FEE
                ps.setString(paramIndex++, order.getString("pic_path")); // PIC_PATH
                ps.setString(paramIndex++, order.getString("sku_id")); // SKU_ID
                ps.setString(paramIndex++, order.getString("promise_collect_time")); // SKU_ID

                /*String promiseCollectTime = order.getString("promise_collect_time");
                if (promiseCollectTime != null) {
                    ps.setTimestamp(paramIndex++, Timestamp.valueOf(promiseCollectTime));
                } else {
                    ps.setNull(paramIndex++, java.sql.Types.TIMESTAMP);
                }*/

                ps.setString(paramIndex, order.getString("refund_id")); // REFUND_ID
            }

            @Override
            public int getBatchSize() {
                return orderList.size();
            }
        });
    }

    /**
     * 获取一本铺历史订单
     */
    public static void main(String[] args) throws ApiException {
        // 此方法现在需要依赖Spring上下文，建议通过单元测试或Controller调用
        System.out.println("请通过Spring容器调用fetchAndStoreOrders方法");
        SoldgetServices service = new SoldgetServices();
        service.fetchAndStoreOrders();
    }
}