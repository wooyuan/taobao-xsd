package com.taobao.logistics.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.internal.util.StringUtils;
import com.taobao.api.request.RefundsReceiveGetRequest;
import com.taobao.api.response.RefundsReceiveGetResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

@Slf4j
@Service
public class RefundsReceiveGetServices {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String URL = "http://gw.api.taobao.com/router/rest";
    private static final String FIELDS = "refund_id,tid,oid,title,buyer_nick,seller_nick,total_fee,status,created,refund_fee,refund_phase,dispute_type,sku,outer_id,num,buyer_open_uid,ouid";

    public void pullAllRefunds(String startday,String endday ) throws ApiException {
        TaobaoClient client = new DefaultTaobaoClient(URL, LogisticsConfig.XSDAPP_KEY, LogisticsConfig.XSDAPP_SECRET);
        String sessionKey = LogisticsConfig.SESSIONKEY;

        int pageNo = 1;
        boolean hasNext = true;
        final int pageSize = 100; // 每页大小，最大100

        while (hasNext) {
            RefundsReceiveGetRequest req = new RefundsReceiveGetRequest();
            req.setFields(FIELDS);
            req.setStartModified(StringUtils.parseDateTime(startday));
            req.setEndModified(StringUtils.parseDateTime(endday));
            req.setPageNo((long) pageNo);
            req.setPageSize((long) pageSize);
            req.setUseHasNext(true);

            RefundsReceiveGetResponse rsp = client.execute(req, sessionKey);
            String responseBody = rsp.getBody();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            JSONObject responseObj = jsonResponse.getJSONObject("refunds_receive_get_response");

            if (responseObj != null) {
                hasNext = responseObj.getBooleanValue("has_next");
                JSONArray refundsArray = responseObj.getJSONObject("refunds").getJSONArray("refund");

                if (refundsArray != null && !refundsArray.isEmpty()) {
                    batchInsertRefunds(refundsArray);
                    log.info("插入第{}页数据，共{}条", pageNo, refundsArray.size());
                }
                pageNo++;
            } else {
                hasNext = false;
                log.error("API返回异常: {}", responseBody);
            }
        }
    }

    @Transactional
    public void batchInsertRefunds(JSONArray refundsArray) {
        String sql = "MERGE INTO tb_refunds t " +
                "USING (SELECT ? refund_id, ? tid, ? oid, ? title, ? buyer_nick, ? seller_nick, " +
                "? total_fee, ? status, ? created, ? refund_fee, ? refund_phase, ? dispute_type, " +
                "? sku, ? outer_id, ? num, ? buyer_open_uid, ? ouid FROM dual) s " +
                "ON (t.refund_id = s.refund_id) " +
                "WHEN MATCHED THEN UPDATE SET " +
                "t.tid = s.tid, t.oid = s.oid, t.title = s.title, " +
                "t.buyer_nick = s.buyer_nick, t.seller_nick = s.seller_nick, " +
                "t.total_fee = s.total_fee, t.status = s.status, t.created = s.created, " +
                "t.refund_fee = s.refund_fee, t.refund_phase = s.refund_phase, " +
                "t.dispute_type = s.dispute_type, t.sku = s.sku, t.outer_id = s.outer_id, " +
                "t.num = s.num, t.buyer_open_uid = s.buyer_open_uid, t.ouid = s.ouid, " +
                "t.last_update_time = SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (" +
                "refund_id, tid, oid, title, buyer_nick, seller_nick, total_fee, " +
                "status, created, refund_fee, refund_phase, dispute_type, sku, " +
                "outer_id, num, buyer_open_uid, ouid, last_update_time) " +
                "VALUES (s.refund_id, s.tid, s.oid, s.title, s.buyer_nick, " +
                "s.seller_nick, s.total_fee, s.status, s.created, s.refund_fee, " +
                "s.refund_phase, s.dispute_type, s.sku, s.outer_id, s.num, " +
                "s.buyer_open_uid, s.ouid, SYSTIMESTAMP)";

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
                JSONObject refund = refundsArray.getJSONObject(i);
                ps.setString(1, refund.getString("refund_id"));
                ps.setString(2, refund.getString("tid"));
                ps.setString(3, refund.getString("oid"));
                ps.setString(4, refund.getString("title"));
                ps.setString(5, refund.getString("buyer_nick"));
                ps.setString(6, refund.getString("seller_nick"));
                ps.setBigDecimal(7, refund.getBigDecimal("total_fee"));
                ps.setString(8, refund.getString("status"));
                ps.setTimestamp(9, Timestamp.valueOf(refund.getString("created")));
                ps.setBigDecimal(10, refund.getBigDecimal("refund_fee"));
                ps.setString(11, refund.getString("refund_phase"));
                ps.setString(12, refund.getString("dispute_type"));
                ps.setString(13, refund.getString("sku"));
                ps.setString(14, refund.getString("outer_id"));
                ps.setInt(15, refund.getIntValue("num"));
                ps.setString(16, refund.getString("buyer_open_uid"));
                ps.setString(17, refund.getString("ouid"));
            }

            @Override
            public int getBatchSize() {
                return refundsArray.size();
            }
        });
    }

    public static void main(String[] args) {
        try {
            // 需要配置Spring容器获取JdbcTemplate
            // ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
             RefundsReceiveGetServices service = new RefundsReceiveGetServices();

             service.pullAllRefunds("2025-09-02 00:00:00","2025-09-02 23:59:59");
        } catch (Exception e) {
            log.error("拉取数据失败", e);
        }
    }
}