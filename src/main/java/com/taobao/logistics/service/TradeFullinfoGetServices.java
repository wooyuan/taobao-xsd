package com.taobao.logistics.service;

import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;

import com.taobao.api.request.TradeFullinfoGetRequest;
import com.taobao.api.response.TradeFullinfoGetResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 *  淘宝小时达订单获取
 */

@Slf4j
@Service
public class TradeFullinfoGetServices {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    String url = "http://gw.api.taobao.com/router/rest";

    public JSONObject printjson() throws ApiException {
        String appkey = LogisticsConfig.XSDAPP_KEY;
        String secret = LogisticsConfig.XSDAPP_SECRET;
        String sessionKey = LogisticsConfig.SESSIONKEY;
        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);

        TradeFullinfoGetRequest req = new TradeFullinfoGetRequest();
        req.setFields("tid,type,status,payment,orders,promotion_details");
        req.setTid(2899536456494937165L);
        req.setIncludeOaid("include_oaid");
        TradeFullinfoGetResponse rsp = client.execute(req, sessionKey);
        
        log.info("淘宝订单详情响应: {}", rsp.getBody());
        
        // 解析并存储订单数据到数据库
        if (rsp.isSuccess()) {
            JSONObject responseJson = JSONObject.parseObject(rsp.getBody());
            saveTradeInfo(responseJson);
        } else {
            log.error("获取订单详情失败: {}", rsp.getSubMsg());
        }
        
        return JSONObject.parseObject(rsp.getBody());
    }
    
    /**
     * 保存订单信息到数据库
     */
    private void saveTradeInfo(JSONObject responseJson) {
        try {
            JSONObject tradeResponse = responseJson.getJSONObject("trade_fullinfo_get_response");
            if (tradeResponse != null) {
                JSONObject trade = tradeResponse.getJSONObject("trade");
                if (trade != null) {
                    String sql = "INSERT INTO TRADE_FULLINFO (TID, TYPE, STATUS, PAYMENT, RESPONSE_DATA, CREATE_TIME) " +
                               "VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP)";
                    
                    jdbcTemplate.update(sql,
                        trade.getString("tid"),
                        trade.getString("type"),
                        trade.getString("status"),
                        trade.getBigDecimal("payment"),
                        responseJson.toJSONString()
                    );
                    
                    log.info("订单详情已保存到数据库, TID: {}", trade.getString("tid"));
                }
            }
        } catch (Exception e) {
            log.error("保存订单详情到数据库时发生异常: {}", e.getMessage(), e);
        }
    }


    //获取一本铺历史订单
    public static void main(String[] args) throws  ApiException {

        try{
            TradeFullinfoGetServices n =new TradeFullinfoGetServices();
            n.printjson();
        } catch (Exception e) {
            System.out.println("测试"+e.toString());
        }


    }
}
