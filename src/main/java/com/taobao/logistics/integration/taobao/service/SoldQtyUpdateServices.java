package com.taobao.logistics.integration.taobao.service;

import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.internal.util.StringUtils;
import com.taobao.api.request.TradesSoldGetRequest;
import com.taobao.api.response.TradesSoldGetResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 *
 *  淘宝小时达订单获取
 */

@Slf4j
@Service
public class SoldQtyUpdateServices {
    String url = "http://gw.api.taobao.com/router/rest";

    public JSONObject printjson( ) throws ApiException {
        String appkey = LogisticsConfig.XSDAPP_KEY;
        String secret = LogisticsConfig.XSDAPP_SECRET;
        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
        TradesSoldGetRequest req = new TradesSoldGetRequest();
        req.setFields("tid,type,status,payment,orders,rx_audit_status");
        req.setStartCreated(StringUtils.parseDateTime("2025-08-01 00:00:00"));
        req.setEndCreated(StringUtils.parseDateTime("2025-08-26 23:59:59"));
        req.setStatus("TRADE_FINISHED");

        req.setPageNo(1L);
        req.setPageSize(40L);
        req.setUseHasNext(true);

        String sessionKey = LogisticsConfig.SESSIONKEY;
        TradesSoldGetResponse rsp = client.execute(req, sessionKey);
        System.out.println(rsp.getBody());
        return null ;
    }


    //更新一本铺库存数据
    public static void main(String[] args) throws  ApiException {

        try{
            SoldQtyUpdateServices n =new SoldQtyUpdateServices();
            n.printjson();
        } catch (Exception e) {
            System.out.println("测试"+e.toString());
        }


    }
}
