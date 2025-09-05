package com.taobao.logistics.service;

import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.internal.util.StringUtils;
import com.taobao.api.request.TradeFullinfoGetRequest;
import com.taobao.api.request.TradesSoldGetRequest;
import com.taobao.api.response.TradeFullinfoGetResponse;
import com.taobao.api.response.TradesSoldGetResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
//import java.net.URISyntaxException;

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

    public JSONObject printjson( ) throws ApiException {
        String appkey = LogisticsConfig.XSDAPP_KEY;
        String secret = LogisticsConfig.XSDAPP_SECRET;
        String sessionKey = LogisticsConfig.SESSIONKEY;
        TaobaoClient client = new DefaultTaobaoClient(url, LogisticsConfig.XSDAPP_KEY, LogisticsConfig.XSDAPP_SECRET);

        TradeFullinfoGetRequest req = new TradeFullinfoGetRequest();
        req.setFields("tid,type,status,payment,orders,promotion_details");
        req.setTid(2899536456494937165L);
        req.setIncludeOaid("include_oaid");
        TradeFullinfoGetResponse rsp = client.execute(req, sessionKey);
        System.out.println(rsp.getBody());


        return null ;
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
