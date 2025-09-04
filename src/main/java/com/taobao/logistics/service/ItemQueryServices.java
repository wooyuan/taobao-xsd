package com.taobao.logistics.service;

import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaXsdItemQueryRequest;
import com.taobao.api.request.AlibabaXsdItemStoreScrollQueryRequest;
import com.taobao.api.response.AlibabaXsdItemQueryResponse;
import com.taobao.api.response.AlibabaXsdItemStoreScrollQueryResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
//import java.net.URISyntaxException;

/**
 *
 *  淘宝小时达订单获取
 */

@Slf4j
@Service
public class ItemQueryServices {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    String url = "http://gw.api.taobao.com/router/rest";

    public JSONObject printjson( ) throws ApiException {
        String appkey = LogisticsConfig.XSDAPP_KEY;
        String secret = LogisticsConfig.XSDAPP_SECRET;
        String sessionKey = "6102912579c7e2db03f113a1f0f73b8ae8ea04b348cd2bd1757633411";

        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
        AlibabaXsdItemQueryRequest req = new AlibabaXsdItemQueryRequest();
        AlibabaXsdItemQueryRequest.XsdItemQueryRequest obj1 = new AlibabaXsdItemQueryRequest.XsdItemQueryRequest();
        obj1.setStoreId(1157535260L);
        List<AlibabaXsdItemQueryRequest.XsdItemQueryDTO> list3 = new ArrayList<AlibabaXsdItemQueryRequest.XsdItemQueryDTO>();
        AlibabaXsdItemQueryRequest.XsdItemQueryDTO obj4 = new AlibabaXsdItemQueryRequest.XsdItemQueryDTO();
        list3.add(obj4);
        obj4.setItemId(853354219555L);
        obj1.setItems(list3);
        req.setXsdItemQueryRequest(obj1);
        AlibabaXsdItemQueryResponse rsp = client.execute(req, sessionKey);
        System.out.println(rsp.getBody());
        return null ;
    }


    //查询商品
    public static void main(String[] args) throws  ApiException {

        try{
            ItemQueryServices n =new ItemQueryServices();
            n.printjson();
        } catch (Exception e) {
            System.out.println("测试"+e.toString());
        }


    }
}
