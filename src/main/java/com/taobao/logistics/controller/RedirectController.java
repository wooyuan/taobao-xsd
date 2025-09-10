package com.taobao.logistics.controller;

import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.logistics.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * Created by ShiShiDaWei on 2021/8/13.
 */
@Slf4j
@RestController
@RequestMapping(value = "/code")
public class RedirectController {

   

    @RequestMapping(value = "/findxsd", produces = "text/html;charset=utf-8")
    public String getLogisticsCodexsd(String errorDescription, @RequestParam(value = "state", defaultValue = "") String state,
                                   @RequestParam(value = "code", defaultValue = "") String code, String key) {
        System.out.println("errorDescription = [" + errorDescription + "], state = [" + state + "], code = [" + code + "], key = [" + key + "]");
        boolean b = StringUtils.hasText(code);
        System.out.println("b = " + b);
        return "<h1>无法获取code ErrorMsg:" + errorDescription + "</h1>";
    }

    @RequestMapping(value = "/Storeadd", produces = "text/html;charset=utf-8")
    public String Storeadd(String errorDescription) throws ApiException {
        StoresQueryServices storequery=new StoresQueryServices();
        storequery.completeStoreFetchProcess();
        log.info("门店初始化完毕");
        StoreScrollQueryServices service=new StoreScrollQueryServices();
        service.fetchAndSaveAllItems();
        log.info("商品资料初始化完毕");
        return "OK";
    }
    //更新门店库存
    @RequestMapping(value = "/Updateqtyc", produces = "text/html;charset=utf-8")
    public String Updateqtyc(String errorDescription) throws ApiException {
        MerchantAdjustServices service = new MerchantAdjustServices();
        service.updateInventory();
        log.info("库存更新结束");
        return "OK";
    }


    // 接口请求接收后更新库存
    @PostMapping(value = "/Updateqty", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateQty(@RequestBody Map<String, Object> requestData) {
        try {
            // 提取data字段
            Map<String, Object> data = (Map<String, Object>) requestData.get("data");

            // 解析所需字段
            String id = String.valueOf(data.get("ID"));
            String storeId = String.valueOf(data.get("PLATFORM_SHOP_ID"));
            Long qty = Long.valueOf(data.get("QTY").toString());

            // 解析PLATFORM_SHOP_ID并分割
            String platformShopId = String.valueOf(data.get("PRODUCT_SPEC_ID"));
            String[] shopIdParts = platformShopId.split(",");
            Long itemId = Long.valueOf(shopIdParts[0]);
            Long skuId = Long.valueOf(shopIdParts[1]);
            // 调用库存更新服务
             MerchantAdjustServices service = new MerchantAdjustServices();
             service.updateInventorybyid(id, storeId, qty, itemId, skuId);

            log.info("库存更新完成，参数：id={}, storeId={}, qty={}, itemId={}, skuId={}",
                    id, storeId, qty, itemId, skuId);

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("库存更新失败，请求数据：{}", requestData, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("处理失败: " + e.getMessage());
        }
    }






    @RequestMapping(value = "/Getorder", produces = "text/html;charset=utf-8")
    public String Getorder(String errorDescription) throws ApiException {

        LocalDate today = LocalDate.now();
        // 获取当天的开始时间 (00:00:00)
        LocalDateTime startOfDay = today.atStartOfDay();
        // 获取当天的结束时间 (23:59:59)
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 格式化为字符串
        String startTimeStr = startOfDay.format(formatter);
        String endTimeStr = endOfDay.format(formatter);

        startTimeStr="2025-08-28 00:00:00";
        endTimeStr="2025-08-28 23:59:59";
        //订单生成
        SoldIncrementGetServices serviceorder = new SoldIncrementGetServices();
        serviceorder.fetchAndSaveAllData(startTimeStr,endTimeStr);
        //退单
        RefundsReceiveGetServices service = new RefundsReceiveGetServices();
        service.pullAllRefunds(startTimeStr,endTimeStr);


        return "OK";
    }
 
    //转发 服务器内部
    @RequestMapping("/forward")
    public String forword(){
        return "forward:/index_taobao";
    }

    //重定向 浏览器自动发起对跳转目标的请求
    @RequestMapping("redirect")
    public String redirect(){
        return "redirect:/index_taobao";
    }

    public static void main(String[] args) {
        System.out.println(LocalDateTime.ofInstant(Instant.ofEpochSecond(1629120264), ZoneId.systemDefault()));
        System.out.println(LocalDateTime.now());
        System.out.println(LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        System.out.println(new Date());



    }



}
