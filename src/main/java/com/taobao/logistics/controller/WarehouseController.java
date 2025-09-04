package com.taobao.logistics.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.taobao.api.request.CainiaoWaybillIiGetRequest;
import com.taobao.logistics.entity.Address;
import com.taobao.logistics.entity.WarehouseShip;
import com.taobao.logistics.service.OrderWaybillServices;
import com.taobao.logistics.service.WarehouseServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ShiShiDaWei on 2021/9/29.
 电子面单接口，获取电子面单后返回电子面单单号
 */
@Slf4j
@Controller
@RequestMapping(value = "/entrepot")
public class WarehouseController {


    @Autowired
    private WarehouseServices warehouseServices;

    @Autowired
    private OrderWaybillServices orderWaybillServices;


    @PostMapping(value = "/getWaybillCode")
    @ResponseBody
    Map<String, Object> getWaybillCode(@RequestBody String data) {
        //http://cainiao.enjoyme.cn:9213/code/login/ZTO/55156/140449/mday旗舰店/0/15055190645/32976438
        //System.out.println("data = [" + data + "]");
        JSONObject jsonObject = JSONObject.parseObject(data);
        String docNo = jsonObject.getString("docNo");
        String logisticCode = jsonObject.getString("logisticCode");
        Integer storeId = jsonObject.getInteger("storeId");
        Integer items = jsonObject.getInteger("goodsNum");
        Map<String, Object> map = new HashMap<>(5);
        if (!StringUtils.hasLength(docNo) || !StringUtils.hasLength(logisticCode) || null == storeId) {
            map.put("code", -1);
            map.put("data", "");
            map.put("errMsg", docNo+","+logisticCode+","+storeId);
            return map;
        }
        Map<String, Object> waybillMap;
        try {
            Address recipientAddress = jsonObject.getObject("recipientAddress", new TypeReference<Address>(){});
            JSONArray items2 = jsonObject.getJSONArray("items");
            List<CainiaoWaybillIiGetRequest.Item> goodsList = items2.toJavaList(CainiaoWaybillIiGetRequest.Item.class);
            if (goodsList.size() == 0) {
                CainiaoWaybillIiGetRequest.Item item = new CainiaoWaybillIiGetRequest.Item();
                item.setName("纪念日百货");
                item.setCount(1L);
                goodsList.add(item);
            }
            waybillMap = orderWaybillServices.getWaybill(docNo, items, logisticCode, storeId, recipientAddress, goodsList);
        } catch (Exception e) {
            log.error("data={}", data.toString());
          //  log.error("getWaybillCode Exception:{}\n{}", e.getMessage(), e.getLocalizedMessage());
            map.put("code", -1);
            map.put("data","error");
            map.put("errMsg", " error");
            return map;
        }
        Integer code = (Integer) waybillMap.get("code");
        if (code == -1) {
            return waybillMap;
        }
        WarehouseShip warehouseShip = (WarehouseShip) waybillMap.get("data");
        String waybillCode = warehouseShip.getWaybillCode();
        map.put("code", 200);
        map.put("data", waybillCode);
        map.put("errMsg", "SUCCESS");
        return map;
    }


    @GetMapping(value = "getPrint")
    ModelAndView getPrint(String waybillCode, HttpServletResponse response) {
        response.setHeader("Content-Security-Policy", "upgrade-insecure-requests");
        ModelAndView print = new ModelAndView();
        if (!StringUtils.hasLength(waybillCode)) {
            print.setViewName("err");
            print.addObject("msg", "waybillCode不正确！");
            return print;
        }
        WarehouseShip printInfo;
        try {
            printInfo = warehouseServices.getPrintInfo(waybillCode);
        } catch (Exception e) {
            e.printStackTrace();
            print.setViewName("err");
            print.addObject("msg", "解析JSON异常！");
            return print;
        }
        if (null == printInfo) {
            print.setViewName("err");
            print.addObject("msg", "无数据！！！");
            return print;
        }
        print.addObject("printmessage", printInfo.getPrintDate());
        print.setViewName("print_test");
        return print;
    }




}
