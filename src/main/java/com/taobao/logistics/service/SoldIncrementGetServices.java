package com.taobao.logistics.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.internal.util.StringUtils;
import com.taobao.api.request.TradesSoldIncrementGetRequest;
import com.taobao.api.response.TradesSoldIncrementGetResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SoldIncrementGetServices {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    String url = "http://gw.api.taobao.com/router/rest";

    // 主订单MERGE SQL
    private static final String MERGE_MAIN_ORDERS_SQL =
            "MERGE INTO main_orders mo " +
                    "USING (SELECT ? tid, ? no_shipping, ? num, ? oaid, ? pay_time, ? payment, " +
                    "? receiver_address, ? receiver_city, ? receiver_district, ? receiver_mobile, " +
                    "? receiver_name, ? receiver_state, ? receiver_town, ? receiver_zip, ? status, ? order_type FROM dual) new_data " +
                    "ON (mo.tid = new_data.tid) " +
                    "WHEN MATCHED THEN UPDATE SET " +
                    "mo.no_shipping = new_data.no_shipping, " +
                    "mo.num = new_data.num, " +
                    "mo.oaid = new_data.oaid, " +
                    "mo.pay_time = new_data.pay_time, " +
                    "mo.payment = new_data.payment, " +
                    "mo.receiver_address = new_data.receiver_address, " +
                    "mo.receiver_city = new_data.receiver_city, " +
                    "mo.receiver_district = new_data.receiver_district, " +
                    "mo.receiver_mobile = new_data.receiver_mobile, " +
                    "mo.receiver_name = new_data.receiver_name, " +
                    "mo.receiver_state = new_data.receiver_state, " +
                    "mo.receiver_town = new_data.receiver_town, " +
                    "mo.receiver_zip = new_data.receiver_zip, " +
                    "mo.status = new_data.status, " +
                    "mo.order_type = new_data.order_type " +
                    "WHEN NOT MATCHED THEN INSERT (" +
                    "tid, no_shipping, num, oaid, pay_time, payment, receiver_address, " +
                    "receiver_city, receiver_district, receiver_mobile, receiver_name, " +
                    "receiver_state, receiver_town, receiver_zip, status, order_type) VALUES (" +
                    "new_data.tid, new_data.no_shipping, new_data.num, new_data.oaid, new_data.pay_time, " +
                    "new_data.payment, new_data.receiver_address, new_data.receiver_city, " +
                    "new_data.receiver_district, new_data.receiver_mobile, new_data.receiver_name, " +
                    "new_data.receiver_state, new_data.receiver_town, new_data.receiver_zip, " +
                    "new_data.status, new_data.order_type)";

    // 子订单MERGE SQL
    private static final String MERGE_ORDER_ITEMS_SQL =
            "MERGE INTO order_items oi " +
                    "USING (SELECT ? oid, ? tid, ? adjust_fee, ? buyer_rate, ? cid, ? discount_fee, " +
                    "? divide_order_fee, ? end_time, ? is_daixiao, ? is_idle, ? num, ? num_iid, " +
                    "? outer_iid, ? outer_sku_id, ? part_mjz_discount, ? payment, ? pic_path, " +
                    "? price, ? refund_id, ? refund_status, ? seller_rate, ? seller_type, " +
                    "? sku_id, ? sku_properties_name, ? status, ? store_code, ? title, ? total_fee FROM dual) new_data " +
                    "ON (oi.oid = new_data.oid) " +
                    "WHEN MATCHED THEN UPDATE SET " +
                    "oi.tid = new_data.tid, " +
                    "oi.adjust_fee = new_data.adjust_fee, " +
                    "oi.buyer_rate = new_data.buyer_rate, " +
                    "oi.cid = new_data.cid, " +
                    "oi.discount_fee = new_data.discount_fee, " +
                    "oi.divide_order_fee = new_data.divide_order_fee, " +
                    "oi.end_time = new_data.end_time, " +
                    "oi.is_daixiao = new_data.is_daixiao, " +
                    "oi.is_idle = new_data.is_idle, " +
                    "oi.num = new_data.num, " +
                    "oi.num_iid = new_data.num_iid, " +
                    "oi.outer_iid = new_data.outer_iid, " +
                    "oi.outer_sku_id = new_data.outer_sku_id, " +
                    "oi.part_mjz_discount = new_data.part_mjz_discount, " +
                    "oi.payment = new_data.payment, " +
                    "oi.pic_path = new_data.pic_path, " +
                    "oi.price = new_data.price, " +
                    "oi.refund_id = new_data.refund_id, " +
                    "oi.refund_status = new_data.refund_status, " +
                    "oi.seller_rate = new_data.seller_rate, " +
                    "oi.seller_type = new_data.seller_type, " +
                    "oi.sku_id = new_data.sku_id, " +
                    "oi.sku_properties_name = new_data.sku_properties_name, " +
                    "oi.status = new_data.status, " +
                    "oi.store_code = new_data.store_code, " +
                    "oi.title = new_data.title, " +
                    "oi.total_fee = new_data.total_fee " +
                    "WHEN NOT MATCHED THEN INSERT (" +
                    "oid, tid, adjust_fee, buyer_rate, cid, discount_fee, divide_order_fee, " +
                    "end_time, is_daixiao, is_idle, num, num_iid, outer_iid, outer_sku_id, " +
                    "part_mjz_discount, payment, pic_path, price, refund_id, refund_status, " +
                    "seller_rate, seller_type, sku_id, sku_properties_name, status, store_code, " +
                    "title, total_fee) VALUES (" +
                    "new_data.oid, new_data.tid, new_data.adjust_fee, new_data.buyer_rate, " +
                    "new_data.cid, new_data.discount_fee, new_data.divide_order_fee, " +
                    "new_data.end_time, new_data.is_daixiao, new_data.is_idle, new_data.num, " +
                    "new_data.num_iid, new_data.outer_iid, new_data.outer_sku_id, " +
                    "new_data.part_mjz_discount, new_data.payment, new_data.pic_path, " +
                    "new_data.price, new_data.refund_id, new_data.refund_status, " +
                    "new_data.seller_rate, new_data.seller_type, new_data.sku_id, " +
                    "new_data.sku_properties_name, new_data.status, new_data.store_code, " +
                    "new_data.title, new_data.total_fee)";

    public void fetchAndSaveAllData(String startday,String endday ) throws ApiException {
        String appkey = LogisticsConfig.XSDAPP_KEY;
        String secret = LogisticsConfig.XSDAPP_SECRET;
        TaobaoClient client = new DefaultTaobaoClient(url, LogisticsConfig.XSDAPP_KEY, LogisticsConfig.XSDAPP_SECRET);
        String sessionKey = "61003252b2d1f519c7c62e17ab11c588e1672a9bf76cb631757633411";

        long pageNo = 1L;
        boolean hasNext = true;

        while (hasNext) {
            TradesSoldIncrementGetRequest req = new TradesSoldIncrementGetRequest();
            req.setFields("tid,type,status,payment,orders,rx_audit_status,pay_time,receiver_name,receiver_state,receiver_city,receiver_district,receiver_town,receiver_address,receiver_zip,receiver_mobile,receiver_phone,num");
            req.setStartModified(StringUtils.parseDateTime(startday));
            req.setEndModified(StringUtils.parseDateTime(endday));
            req.setPageNo(pageNo);
            req.setPageSize(100L); // 使用最大页大小减少请求次数
            req.setUseHasNext(true);

            TradesSoldIncrementGetResponse rsp = client.execute(req, sessionKey);
            String responseBody = rsp.getBody();
            log.info("Page {} response: {}", pageNo, responseBody);

            // 解析响应并保存数据
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            JSONObject responseObj = jsonResponse.getJSONObject("trades_sold_increment_get_response");

            if (responseObj != null) {
                hasNext = responseObj.getBooleanValue("has_next");
                saveResponseData(responseObj);
            } else {
                hasNext = false;
                log.error("Invalid response format: {}", responseBody);
            }

            pageNo++;

            // 添加短暂延迟避免API限制
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted during sleep", e);
            }
        }
    }

    private void saveResponseData(JSONObject responseObj) {
        JSONObject trades = responseObj.getJSONObject("trades");
        if (trades == null) {
            return;
        }

        JSONArray tradeArray = trades.getJSONArray("trade");
        if (tradeArray == null || tradeArray.isEmpty()) {
            return;
        }

        List<Object[]> mainOrderBatch = new ArrayList<>();
        List<Object[]> orderItemBatch = new ArrayList<>();

        for (int i = 0; i < tradeArray.size(); i++) {
            JSONObject trade = tradeArray.getJSONObject(i);

            // 安全地获取字段值，使用optXXX方法避免字段不存在时抛出异常
            String tid = trade.getString("tid");
            Boolean noShipping = trade.getBoolean("no_shipping"); // 可能为null
            Integer num = trade.getInteger("num"); // 可能为null
            String oaid = trade.getString("oaid");
            String payTimeStr = trade.getString("pay_time");
            Number payment = trade.getBigDecimal("payment"); // 使用Number类型更安全
            String receiverAddress = trade.getString("receiver_address");
            String receiverCity = trade.getString("receiver_city");
            String receiverDistrict = trade.getString("receiver_district");
            String receiverMobile = trade.getString("receiver_mobile");
            String receiverName = trade.getString("receiver_name");
            String receiverState = trade.getString("receiver_state");
            String receiverTown = trade.getString("receiver_town");
            String receiverZip = trade.getString("receiver_zip");
            String status = trade.getString("status");
            String type = trade.getString("type");

            // 准备主订单数据
            Object[] mainOrderParams = {
                    tid,
                    noShipping != null ? (noShipping ? 1 : 0) : null,
                    num,
                    oaid,
                    parseDateTime(payTimeStr),
                    payment,
                    receiverAddress,
                    receiverCity,
                    receiverDistrict,
                    receiverMobile,
                    receiverName,
                    receiverState,
                    receiverTown,
                    receiverZip,
                    status,
                    type
            };
            mainOrderBatch.add(mainOrderParams);

            // 处理子订单
            JSONObject orders = trade.getJSONObject("orders");
            if (orders != null) {
                JSONArray orderArray = orders.getJSONArray("order");
                if (orderArray != null) {
                    for (int j = 0; j < orderArray.size(); j++) {
                        JSONObject order = orderArray.getJSONObject(j);

                        // 安全地获取子订单字段值
                        String oid = order.getString("oid");
                        Number adjustFee = order.getBigDecimal("adjust_fee");
                        Boolean buyerRate = order.getBoolean("buyer_rate");
                        Long cid = order.getLong("cid");
                        Number discountFee = order.getBigDecimal("discount_fee");
                        Number divideOrderFee = order.getBigDecimal("divide_order_fee");
                        String endTimeStr = order.getString("end_time");
                        Boolean isDaixiao = order.getBoolean("is_daixiao");
                        String isIdle = order.getString("is_idle");
                        Integer orderNum = order.getInteger("num");
                        Long numIid = order.getLong("num_iid");
                        String outerIid = order.getString("outer_iid");
                        String outerSkuId = order.getString("outer_sku_id");
                        Number partMjzDiscount = order.getBigDecimal("part_mjz_discount");
                        Number orderPayment = order.getBigDecimal("payment");
                        String picPath = order.getString("pic_path");
                        Number price = order.getBigDecimal("price");
                        String refundId = order.getString("refund_id");
                        String refundStatus = order.getString("refund_status");
                        Boolean sellerRate = order.getBoolean("seller_rate");
                        String sellerType = order.getString("seller_type");
                        String skuId = order.getString("sku_id");
                        String skuPropertiesName = order.getString("sku_properties_name");
                        String orderStatus = order.getString("status");
                        String storeCode = order.getString("store_code");
                        String title = order.getString("title");
                        Number totalFee = order.getBigDecimal("total_fee");

                        Object[] orderItemParams = {
                                oid,
                                tid,
                                adjustFee,
                                buyerRate != null ? (buyerRate ? 1 : 0) : null,
                                cid,
                                discountFee,
                                divideOrderFee,
                                parseDateTime(endTimeStr),
                                isDaixiao != null ? (isDaixiao ? 1 : 0) : null,
                                isIdle,
                                orderNum,
                                numIid,
                                outerIid,
                                outerSkuId,
                                partMjzDiscount,
                                orderPayment,
                                picPath,
                                price,
                                refundId,
                                refundStatus,
                                sellerRate != null ? (sellerRate ? 1 : 0) : null,
                                sellerType,
                                skuId,
                                skuPropertiesName,
                                orderStatus,
                                storeCode,
                                title,
                                totalFee
                        };
                        orderItemBatch.add(orderItemParams);
                    }
                }
            }
        }
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setUrl("jdbc:oracle:thin:@10.100.21.151:1521/orcl");
        dataSource.setUsername("neands3");
        dataSource.setPassword("abc123");
        // 创建JdbcTemplate实例
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        // 批量保存主订单
        jdbcTemplate.batchUpdate(MERGE_MAIN_ORDERS_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] params = mainOrderBatch.get(i);
                setMainOrderParameters(ps, params);
            }

            @Override
            public int getBatchSize() {
                return mainOrderBatch.size();
            }
        });

        // 批量保存子订单
        jdbcTemplate.batchUpdate(MERGE_ORDER_ITEMS_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] params = orderItemBatch.get(i);
                setOrderItemParameters(ps, params);
            }

            @Override
            public int getBatchSize() {
                return orderItemBatch.size();
            }
        });

        log.info("Saved {} main orders and {} order items", mainOrderBatch.size(), orderItemBatch.size());
    }

    private void setMainOrderParameters(PreparedStatement ps, Object[] params) throws SQLException {
        int index = 1;
        ps.setString(index++, (String) params[0]); // tid

        // no_shipping (可能为null)
        if (params[1] != null) {
            ps.setInt(index++, (Integer) params[1]);
        } else {
            ps.setNull(index++, Types.INTEGER);
        }

        // num (可能为null)
        if (params[2] != null) {
            ps.setInt(index++, (Integer) params[2]);
        } else {
            ps.setNull(index++, Types.INTEGER);
        }

        ps.setString(index++, (String) params[3]); // oaid

        // pay_time (可能为null)
        if (params[4] != null) {
            ps.setTimestamp(index++, (Timestamp) params[4]);
        } else {
            ps.setNull(index++, Types.TIMESTAMP);
        }

        // payment (可能为null)
        if (params[5] != null) {
            ps.setBigDecimal(index++, (java.math.BigDecimal) params[5]);
        } else {
            ps.setNull(index++, Types.DECIMAL);
        }

        ps.setString(index++, (String) params[6]); // receiver_address
        ps.setString(index++, (String) params[7]); // receiver_city
        ps.setString(index++, (String) params[8]); // receiver_district
        ps.setString(index++, (String) params[9]); // receiver_mobile
        ps.setString(index++, (String) params[10]); // receiver_name
        ps.setString(index++, (String) params[11]); // receiver_state
        ps.setString(index++, (String) params[12]); // receiver_town
        ps.setString(index++, (String) params[13]); // receiver_zip
        ps.setString(index++, (String) params[14]); // status
        ps.setString(index++, (String) params[15]); // type
    }

    private void setOrderItemParameters(PreparedStatement ps, Object[] params) throws SQLException {
        int index = 1;
        ps.setString(index++, (String) params[0]); // oid
        ps.setString(index++, (String) params[1]); // tid

        // adjust_fee (可能为null)
        if (params[2] != null) {
            ps.setBigDecimal(index++, (java.math.BigDecimal) params[2]);
        } else {
            ps.setNull(index++, Types.DECIMAL);
        }

        // buyer_rate (可能为null)
        if (params[3] != null) {
            ps.setInt(index++, (Integer) params[3]);
        } else {
            ps.setNull(index++, Types.INTEGER);
        }

        // cid (可能为null)
        if (params[4] != null) {
            ps.setLong(index++, (Long) params[4]);
        } else {
            ps.setNull(index++, Types.BIGINT);
        }

        // discount_fee (可能为null)
        if (params[5] != null) {
            ps.setBigDecimal(index++, (java.math.BigDecimal) params[5]);
        } else {
            ps.setNull(index++, Types.DECIMAL);
        }

        // divide_order_fee (可能为null)
        if (params[6] != null) {
            ps.setBigDecimal(index++, (java.math.BigDecimal) params[6]);
        } else {
            ps.setNull(index++, Types.DECIMAL);
        }

        // end_time (可能为null)
        if (params[7] != null) {
            ps.setTimestamp(index++, (Timestamp) params[7]);
        } else {
            ps.setNull(index++, Types.TIMESTAMP);
        }

        // is_daixiao (可能为null)
        if (params[8] != null) {
            ps.setInt(index++, (Integer) params[8]);
        } else {
            ps.setNull(index++, Types.INTEGER);
        }

        ps.setString(index++, (String) params[9]); // is_idle

        // num (可能为null)
        if (params[10] != null) {
            ps.setInt(index++, (Integer) params[10]);
        } else {
            ps.setNull(index++, Types.INTEGER);
        }

        // num_iid (可能为null)
        if (params[11] != null) {
            ps.setLong(index++, (Long) params[11]);
        } else {
            ps.setNull(index++, Types.BIGINT);
        }

        ps.setString(index++, (String) params[12]); // outer_iid
        ps.setString(index++, (String) params[13]); // outer_sku_id

        // part_mjz_discount (可能为null)
        if (params[14] != null) {
            ps.setBigDecimal(index++, (java.math.BigDecimal) params[14]);
        } else {
            ps.setNull(index++, Types.DECIMAL);
        }

        // payment (可能为null)
        if (params[15] != null) {
            ps.setBigDecimal(index++, (java.math.BigDecimal) params[15]);
        } else {
            ps.setNull(index++, Types.DECIMAL);
        }

        ps.setString(index++, (String) params[16]); // pic_path

        // price (可能为null)
        if (params[17] != null) {
            ps.setBigDecimal(index++, (java.math.BigDecimal) params[17]);
        } else {
            ps.setNull(index++, Types.DECIMAL);
        }

        ps.setString(index++, (String) params[18]); // refund_id
        ps.setString(index++, (String) params[19]); // refund_status

        // seller_rate (可能为null)
        if (params[20] != null) {
            ps.setInt(index++, (Integer) params[20]);
        } else {
            ps.setNull(index++, Types.INTEGER);
        }

        ps.setString(index++, (String) params[21]); // seller_type
        ps.setString(index++, (String) params[22]); // sku_id
        ps.setString(index++, (String) params[23]); // sku_properties_name
        ps.setString(index++, (String) params[24]); // status
        ps.setString(index++, (String) params[25]); // store_code
        ps.setString(index++, (String) params[26]); // title

        // total_fee (可能为null)
        if (params[27] != null) {
            ps.setBigDecimal(index++, (java.math.BigDecimal) params[27]);
        } else {
            ps.setNull(index++, Types.DECIMAL);
        }
    }

    private Timestamp parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Timestamp.valueOf(dateTimeStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid date time format: {}", dateTimeStr, e);
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            // 这里需要初始化Spring容器并获取Bean
            // 简化示例，实际使用时需要配置Spring上下文
            SoldIncrementGetServices service = new SoldIncrementGetServices();
            service.fetchAndSaveAllData("2025-08-26 00:00:00","2025-08-26 23:59:59");
        } catch (Exception e) {
            log.error("Error fetching and saving data", e);
        }
    }
}