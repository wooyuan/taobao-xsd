package com.taobao.logistics.controller;

import com.taobao.api.ApiException;
import com.taobao.logistics.integration.taobao.service.*;
import com.taobao.logistics.utils.AjaxResult;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流系统重定向控制器
 * 处理授权回调、门店初始化、库存更新、订单获取等业务
 * 
 * @author ShiShiDaWei
 * @version 1.0
 * @since 2021/8/13
 */
@Slf4j
@RestController
@RequestMapping(value = "/code")
public class RedirectController {

    // 日期格式化常量
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private StoresQueryServices storesQueryServices;
    
    @Autowired
    private StoreScrollQueryServices storeScrollQueryServices;
    
    @Autowired
    private MerchantAdjustServices merchantAdjustServices;
    
    @Autowired
    private SoldIncrementGetServices soldIncrementGetServices;
    
    @Autowired
    private RefundsReceiveGetServices refundsReceiveGetServices;
    
    @Autowired
    private OrderAllocationService orderAllocationService;
    
    @Autowired
    private SmartOrderSplitService smartOrderSplitService;

    /**
     * 处理授权回调，获取物流编码
     * 
     * @param errorDescription 错误描述
     * @param state 状态参数
     * @param code 授权码
     * @param key 密钥
     * @return HTML响应
     */
    @GetMapping(value = "/findxsd", produces = MediaType.TEXT_HTML_VALUE + ";charset=utf-8")
    public String getLogisticsCodexsd(
            @RequestParam(required = false) String errorDescription,
            @RequestParam(value = "state", defaultValue = "") String state,
            @RequestParam(value = "code", defaultValue = "") String code,
            @RequestParam(required = false) String key) {
        
        log.info("授权回调请求 - errorDescription: {}, state: {}, code: {}, key: {}", 
                errorDescription, state, code, key);
        
        if (!StringUtils.hasText(code)) {
            log.warn("授权失败，未获取到授权码");
            return "<h1>授权失败 - 无法获取授权码</h1><p>错误信息: " + 
                   (errorDescription != null ? errorDescription : "未知错误") + "</p>";
        }
        
        log.info("授权成功，获取到授权码: {}", code);
        return "<h1>授权成功</h1><p>授权码: " + code + "</p>";
    }

    /**
     * 初始化门店和商品数据
     * 
     * @return 操作结果
     */
@RequestMapping(value = "/Storeadd", method = {RequestMethod.GET, RequestMethod.POST}, produces = "application/json;charset=utf-8")
public AjaxResult initStoreData() {
        try {
            log.info("开始初始化门店数据");
            storesQueryServices.completeStoreFetchProcess();
            log.info("门店初始化完毕");
            
            log.info("开始初始化商品资料");
            storeScrollQueryServices.fetchAndSaveAllItems();
            log.info("商品资料初始化完毕");
            
            return AjaxResult.success("门店和商品数据初始化成功");
        } catch (Exception e) {
            log.error("初始化门店数据失败", e);
            if (e instanceof ApiException) {
                return AjaxResult.error("初始化失败: " + e.getMessage());
            }
            return AjaxResult.error("系统错误，请稍后重试");
        }
    }
    /**
     * 批量更新门店库存
     * 
     * @return 操作结果
     */
    @RequestMapping(value = "/Updateqtyc", produces = "text/html;charset=utf-8")
    public AjaxResult batchUpdateInventory() {
        try {
            log.info("开始批量更新门店库存");
            merchantAdjustServices.updateInventory();
            log.info("批量库存更新完成");
            
            return AjaxResult.success("库存批量更新成功");
        } catch (Exception e) {
            log.error("批量更新库存失败", e);
            if (e instanceof ApiException) {
                return AjaxResult.error("更新失败: " + e.getMessage());
            }
            return AjaxResult.error("系统错误，请稍后重试");
        }
    }


    /**
     * 库存更新请求DTO
     */
    public static class InventoryUpdateRequest {
        @NotBlank(message = "ID不能为空")
        private String id;
        
        @NotBlank(message = "门店ID不能为空")
        private String storeId;
        
        @NotNull(message = "库存数量不能为空")
        @Positive(message = "库存数量必须为正数")
        private Long qty;
        
        @NotNull(message = "商品ID不能为空")
        @Positive(message = "商品ID必须为正数")
        private Long itemId;
        
        @NotNull(message = "SKU ID不能为空")
        @Positive(message = "SKU ID必须为正数")
        private Long skuId;
        
        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStoreId() { return storeId; }
        public void setStoreId(String storeId) { this.storeId = storeId; }
        public Long getQty() { return qty; }
        public void setQty(Long qty) { this.qty = qty; }
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }
    }
    
    /**
     * 根据ID更新指定库存
     * 
     * @param requestData 库存更新请求数据
     * @return 操作结果
     */
    @PostMapping(value = "/Updateqty", produces = MediaType.APPLICATION_JSON_VALUE)
    public AjaxResult updateInventoryById(@RequestBody Map<String, Object> requestData) {
        try {
            log.info("收到库存更新请求: {}", requestData);
            
            // 参数校验和提取
            InventoryUpdateRequest request = parseInventoryRequest(requestData);
            
            // 调用库存更新服务
            merchantAdjustServices.updateInventorybyid(
                    request.getId(), 
                    request.getStoreId(), 
                    request.getQty(), 
                    request.getItemId(), 
                    request.getSkuId());

            log.info("库存更新完成 - ID: {}, 门店: {}, 数量: {}, 商品ID: {}, SKU ID: {}",
                    request.getId(), request.getStoreId(), request.getQty(), 
                    request.getItemId(), request.getSkuId());

            return AjaxResult.success("库存更新成功").put("updatedId", request.getId());
            
        } catch (IllegalArgumentException e) {
            log.warn("库存更新参数错误: {}", e.getMessage());
            return AjaxResult.error("参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("库存更新失败，请求数据: {}", requestData, e);
            return AjaxResult.error("库存更新失败，请稍后重试");
        }
    }
    
    /**
     * 解析库存更新请求参数
     */
    private InventoryUpdateRequest parseInventoryRequest(Map<String, Object> requestData) {
        try {
            // 提取data字段
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) requestData.get("data");
            if (data == null) {
                throw new IllegalArgumentException("请求数据中缺少data字段");
            }

            InventoryUpdateRequest request = new InventoryUpdateRequest();
            
            // 解析所需字段
            Object idObj = data.get("ID");
            if (idObj == null) {
                throw new IllegalArgumentException("ID字段不能为空");
            }
            request.setId(String.valueOf(idObj));
            
            Object storeIdObj = data.get("PLATFORM_SHOP_ID");
            if (storeIdObj == null) {
                throw new IllegalArgumentException("PLATFORM_SHOP_ID字段不能为空");
            }
            request.setStoreId(String.valueOf(storeIdObj));
            
            Object qtyObj = data.get("QTY");
            if (qtyObj == null) {
                throw new IllegalArgumentException("QTY字段不能为空");
            }
            request.setQty(Long.valueOf(qtyObj.toString()));

            // 解析PRODUCT_SPEC_ID并分割
            Object productSpecIdObj = data.get("PRODUCT_SPEC_ID");
            if (productSpecIdObj == null) {
                throw new IllegalArgumentException("PRODUCT_SPEC_ID字段不能为空");
            }
            
            String productSpecId = String.valueOf(productSpecIdObj);
            String[] idParts = productSpecId.split(",");
            if (idParts.length != 2) {
                throw new IllegalArgumentException("PRODUCT_SPEC_ID格式错误，应为 'itemId,skuId'");
            }
            
            request.setItemId(Long.valueOf(idParts[0].trim()));
            request.setSkuId(Long.valueOf(idParts[1].trim()));
            
            return request;
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数字格式错误: " + e.getMessage());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("数据类型错误: " + e.getMessage());
        }
    }



    /**
     * 获取订单数据（包括订单和退款）
     * 
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @return 操作结果
     */
    @RequestMapping(value = "/Getorder", produces = "text/html;charset=utf-8")
    public AjaxResult syncOrderData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            // 处理日期参数
            LocalDate queryDate = LocalDate.now();
            if (StringUtils.hasText(startDate)) {
                queryDate = LocalDate.parse(startDate);
            }
            
            // 如果没有指定结束日期，使用开始日期
            LocalDate endDateParsed = queryDate;
            if (StringUtils.hasText(endDate)) {
                endDateParsed = LocalDate.parse(endDate);
            }
            
            // 构建时间范围
            String startTimeStr = queryDate.atStartOfDay().format(DATE_TIME_FORMATTER);
            String endTimeStr = endDateParsed.atTime(LocalTime.MAX).format(DATE_TIME_FORMATTER);
            
            log.info("开始同步订单数据，时间范围: {} 至 {}", startTimeStr, endTimeStr);
            
            // 同步订单数据
            log.info("开始获取增量订单数据");
            soldIncrementGetServices.fetchAndSaveAllData(startTimeStr, endTimeStr);
            log.info("增量订单数据获取完成");
            
            // 同步退款数据
            log.info("开始获取退款数据");
            refundsReceiveGetServices.pullAllRefunds(startTimeStr, endTimeStr);
            log.info("退款数据获取完成");
            
            return AjaxResult.success("订单数据同步成功")
                    .put("syncPeriod", startTimeStr + " 至 " + endTimeStr)
                    .put("syncTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    
        } catch (Exception e) {
            log.error("订单数据同步失败", e);
            return AjaxResult.error("订单数据同步失败: " + e.getMessage());
        }
    }
 
    // /**
    //  * 智能订单分配接口
    //  * 
    //  * @param orderId 订单ID
    //  * @return HTML格式的分配结果
    //  */
    // @GetMapping(value = "/orderAllocation", produces = MediaType.TEXT_HTML_VALUE + ";charset=utf-8")
    // public String smartOrderAllocation(@RequestParam("orderId") Long orderId) {
    //     try {
    //         log.info("开始进行订单智能分配，订单ID: {}", orderId);
            
    //         if (orderId == null || orderId <= 0) {
    //             return generateErrorHtml("订单ID无效", orderId);
    //         }
            
    //         String result = orderAllocationService.smartOrderAllocation(orderId);
            
    //         log.info("订单分配完成，订单ID: {}", orderId);
    //         return result;
            
    //     } catch (Exception e) {
    //         log.error("订单分配失败，订单ID: {}", orderId, e);
    //         return generateErrorHtml("系统异常: " + e.getMessage(), orderId);
    //     }
    // }


      /**
     * 智能订单分配接口
     * 
     * @param orderId 订单ID
     * @return HTML格式的分配结果
     */
    @GetMapping(value = "/orderAllocation", produces = MediaType.TEXT_HTML_VALUE + ";charset=utf-8")
    public String smartOrderAllocation(@RequestParam("orderId") Long orderId) {
        try {
            log.info("开始进行订单智能分配，订单ID: {}", orderId);
            
            if (orderId == null || orderId <= 0) {
                return generateErrorHtml("订单ID无效", orderId);
            }
            
            String result = orderAllocationService.smartOrderAllocation(orderId);
            
            log.info("订单分配完成，订单ID: {}", orderId);
            return result;
            
        } catch (Exception e) {
            log.error("订单分配失败，订单ID: {}", orderId, e);
            return generateErrorHtml("系统异常: " + e.getMessage(), orderId);
        }
    }
    
    /**
     * 智能递进式拆单分配接口
     * 实现您要求的递进式拆单逻辑：
     * - 三行明细ABC：ABC → (A,BC)|(AB,C) → (A,B,C)
     * - 四行明细ABCD：ABCD → (A,BCD)|(B,ACD)|(C,ABD)|(D,ABC)|(AB,CD)|(AC,BD)|(AD,BC) → ... → (A,B,C,D)
     * - 按拆分规则最少的原则排序显示
     * 
     * @param orderId 订单ID
     * @return HTML格式的递进式拆单分配结果
     */
    @GetMapping(value = "/smartSplit", produces = MediaType.TEXT_HTML_VALUE + ";charset=utf-8")
    @ResponseBody
    public String smartProgressiveSplitAllocation(@RequestParam("orderId") Long orderId) {
        try {
            log.info("开始进行智能递进式拆单分配，订单ID: {}", orderId);
            
            if (orderId == null || orderId <= 0) {
                return generateErrorHtml("订单ID无效", orderId);
            }
            
            String result = smartOrderSplitService.smartProgressiveSplitAllocation(orderId);
            
            log.info("智能递进式拆单分配完成，订单ID: {}", orderId);
            return result;
            
        } catch (Exception e) {
            log.error("智能递进式拆单分配失败，订单ID: {}", orderId, e);
            return generateErrorHtml("系统异常: " + e.getMessage(), orderId);
        }
    }
    
    
    /**
     * 生成错误页面HTML
     */
    private String generateErrorHtml(String errorMessage, Long orderId) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<title>订单分配结果</title>")
            .append("<style>body{font-family:Arial,sans-serif;margin:20px;}")
            .append(".error{color:red;background:#ffe6e6;padding:10px;border:1px solid #ff0000;}</style>")
            .append("</head><body>")
            .append("<h2>订单分配结果 - 订单ID: ").append(orderId != null ? orderId : "无效").append("</h2>")
            .append("<div class='error'>错误: ").append(errorMessage).append("</div>")
            .append("</body></html>");
        return html.toString();
    }

    /**
     * 转发到淘宝首页（服务器内部转发）
     * 
     * @return 转发路径
     */
    @RequestMapping("/forward")
    public String forward() {
        log.info("执行服务器内部转发到淘宝首页");
        return "forward:/index_taobao";
    }

    /**
     * 重定向到淘宝首页（浏览器重定向）
     * 
     * @return 重定向路径
     */
    @RequestMapping("redirect")
    public String redirect() {
        log.info("执行浏览器重定向到淘宝首页");
        return "redirect:/index_taobao";
    }
    

    
    /**
     * 测试方法（仅用于开发调试）
     * 注意：生产环境应移除此方法
     */
    public static void main(String[] args) {
        // 时间戳转换测试
        System.out.println("时间戳 1629120264 转换: " + 
                LocalDateTime.ofInstant(Instant.ofEpochSecond(1629120264), ZoneId.systemDefault()));
        System.out.println("当前时间: " + LocalDateTime.now());
        System.out.println("当前时间戳(毫秒): " + 
                LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        System.out.println("当前Date: " + new java.util.Date());
    }



}
