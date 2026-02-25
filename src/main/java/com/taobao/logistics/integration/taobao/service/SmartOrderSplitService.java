package com.taobao.logistics.integration.taobao.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能拆单服务 - 递进式拆单逻辑
 * 
 * 拆单逻辑说明：
 * 1. 三行明细ABC：ABC → (A,BC)|(AB,C) → (A,B,C)
 * 2. 四行明细ABCD：ABCD → (A,BCD)|(B,ACD)|(C,ABD)|(D,ABC)|(AB,CD)|(AC,BD)|(AD,BC) → (A,B,CD)|(A,C,BD)|(A,D,BC)|(B,C,AD)|(B,D,AC)|(C,D,AB) → (A,B,C,D)
 * 
 * 按照拆分规则最少的原则排序显示
 */
@Slf4j
@Service
public class SmartOrderSplitService {
    
    @Autowired
    private TbqtyServices tbqtyServices;
    
    /**
     * 智能递进式拆单分配
     * @param orderId 订单ID
     * @return 生成HTML格式的分配结果
     */
    public String smartProgressiveSplitAllocation(Long orderId) {
        try {
            // 获取订单明细
            JSONArray orderDetails = tbqtyServices.getOrderDetails(orderId);
            // 获取满足条件的门店库存
            JSONArray storeInventory = tbqtyServices.getStoreInventoryDetails(orderId);
            
            if (orderDetails.isEmpty()) {
                return generateErrorHtml("未找到订单明细数据", orderId);
            }
            
            if (storeInventory.isEmpty()) {
                return generateErrorHtml("没有门店有足够的库存满足订单需求", orderId);
            }
            
            // 实现递进式拆单逻辑
            String result = performProgressiveSplit(orderId, orderDetails, storeInventory);
            
            log.info("智能递进式拆单分配完成，订单ID: {}", orderId);
            return result;
            
        } catch (Exception e) {
            log.error("智能拆单分配时发生异常，订单ID: {}", orderId, e);
            return generateErrorHtml("系统异常: " + e.getMessage(), orderId);
        }
    }
    
    /**
     * 执行递进式拆单分析
     */
    private String performProgressiveSplit(Long orderId, JSONArray orderDetails, JSONArray storeInventory) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<title>智能递进式拆单结果</title>")
            .append("<style>")
            .append("body{font-family:'Microsoft YaHei',Arial,sans-serif;margin:20px;background:#f5f5f5;}")
            .append(".container{max-width:1400px;margin:0 auto;background:white;padding:25px;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1);}")
            .append(".header{text-align:center;margin-bottom:30px;border-bottom:2px solid #4CAF50;padding-bottom:20px;}")
            .append(".order-info{background:#e6f3ff;padding:15px;margin:15px 0;border-radius:5px;border-left:4px solid #2196F3;}")
            .append(".split-level{margin:20px 0;padding:15px;border-radius:6px;}")
            .append(".level-1{background:#e8f5e8;border-left:4px solid #4CAF50;}")
            .append(".level-2{background:#fff8e1;border-left:4px solid #FF9800;}")
            .append(".level-3{background:#ffebee;border-left:4px solid #F44336;}")
            .append(".combination{background:#f9f9f9;margin:10px 0;padding:10px;border-radius:4px;}")
            .append(".item-grid{display:grid;grid-template-columns:repeat(3, 1fr);gap:15px;margin:10px 0;}")
            .append(".item-card{background:#ffffff;border:1px solid #ddd;border-radius:6px;padding:12px;box-shadow:0 2px 4px rgba(0,0,0,0.1);transition:transform 0.2s;}")
            .append(".item-card:hover{transform:translateY(-2px);box-shadow:0 4px 8px rgba(0,0,0,0.15);}")
            .append(".item-header{font-weight:bold;color:#333;margin-bottom:8px;font-size:14px;border-bottom:1px solid #eee;padding-bottom:5px;}")
            .append(".item-detail{margin:4px 0;font-size:13px;color:#666;}")
            .append(".store-section{margin:15px 0;padding:15px;background:#fafafa;border-radius:5px;border-left:3px solid #007bff;}")
            .append("@media (max-width: 1200px) { .item-grid { grid-template-columns: repeat(3, 1fr); } }")
            .append("@media (max-width: 900px) { .item-grid { grid-template-columns: repeat(2, 1fr); } }")
            .append("@media (max-width: 600px) { .item-grid { grid-template-columns: 1fr; } }")
            .append("</style></head><body>")
            .append("<div class='container'>")
            .append("<div class='header'><h1>🎯 智能递进式拆单系统</h1></div>")
            .append("<div class='order-info'>")
            .append("<strong>📦 订单ID:</strong> ").append(orderId)
            .append(" | <strong>📋 订单明细数:</strong> ").append(orderDetails.size())
            .append(" | <strong>🔍 分析时间:</strong> ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</div>");
        
        // 构建明细标签和条码映射
        Map<Long, String> itemLabels = new HashMap<>();
        Map<Long, String> itemBarcodes = new HashMap<>();
        for (int i = 0; i < orderDetails.size(); i++) {
            JSONObject item = orderDetails.getJSONObject(i);
            String label = String.valueOf((char) ('A' + i));
            Long itemId = item.getLong("id");
            itemLabels.put(itemId, label);
            // 添加条码号映射
            String barcodeNo = item.getString("barcode_no");
            itemBarcodes.put(itemId, barcodeNo != null ? barcodeNo : "N/A");
        }
        
        // 构建门店库存映射和条码信息
        Map<Long, Set<Long>> storeItemMap = new HashMap<>();
        Map<Long, String> storeNameMap = new HashMap<>();
        Map<Long, String> storeBarcodeMap = new HashMap<>(); // 存储门店库存中的条码信息
        
        for (int i = 0; i < storeInventory.size(); i++) {
            JSONObject inventory = storeInventory.getJSONObject(i);
            Long storeId = inventory.getLong("c_store_id");
            Long orderItemId = inventory.getLong("order_item_id");
            String storeName = inventory.getString("store_name");
            String barcodeNo = inventory.getString("barcode_no");
            
            storeItemMap.computeIfAbsent(storeId, k -> new HashSet<>()).add(orderItemId);
            storeNameMap.put(storeId, storeName);
            // 为每个订单明细存储条码号
            if (barcodeNo != null) {
                storeBarcodeMap.put(orderItemId, barcodeNo);
            }
        }
        
        Set<Long> allItemIds = itemLabels.keySet();
        
        // 生成递进式拆分方案（优化版）
        int totalItems = orderDetails.size();
        boolean foundSolution = false;
        Set<Long> excludedStores = new HashSet<>(); // 需要排除的门店集合
        
        for (int splitLevel = 1; splitLevel <= totalItems; splitLevel++) {
            List<List<Set<Long>>> splitCombinations = generateSplitCombinations(allItemIds, splitLevel);
            List<String> validSolutions = new ArrayList<>();
            
            // 根据拆分级别选择不同的门店范围
            Map<Long, Set<Long>> availableStoreItemMap;
            if (splitLevel == 1) {
                // 第一级别：使用所有门店
                availableStoreItemMap = storeItemMap;
            } else {
                // 后续级别：排除已能单独满足的门店
                availableStoreItemMap = filterExcludedStores(storeItemMap, excludedStores);
                log.info("拆分级别 {}: 排除 {} 个单门店全量满足的门店，剩余 {} 个门店参与计算", 
                    splitLevel, excludedStores.size(), availableStoreItemMap.size());
            }
            
            for (List<Set<Long>> combination : splitCombinations) {
                List<String> storeAssignments = findStoreAssignments(combination, availableStoreItemMap, 
                    storeNameMap, itemLabels, itemBarcodes, orderDetails);
                if (!storeAssignments.isEmpty()) {
                    validSolutions.addAll(storeAssignments);
                }
            }
            
            if (!validSolutions.isEmpty()) {
                foundSolution = true;
                String levelClass = "level-" + Math.min(splitLevel, 3);
                String levelDesc = getLevelDescription(splitLevel, totalItems);
                
                html.append("<div class='split-level ").append(levelClass).append("'>")
                    .append("<h3>").append(levelDesc).append(" (拆").append(splitLevel).append("次)</h3>")
                    .append("<p>找到 <strong>").append(validSolutions.size()).append("</strong> 种解决方案");
                
                // 显示优化信息
                if (splitLevel > 1 && !excludedStores.isEmpty()) {
                    html.append("（已排除 ").append(excludedStores.size()).append(" 个单门店全量满足的门店）");
                }
                html.append("</p>");
                
                // 限制显示前5种方案
                int displayCount = Math.min(5, validSolutions.size());
                for (int i = 0; i < displayCount; i++) {
                    html.append("<div class='combination'>")
                        .append("<strong>方案 ").append(i + 1).append(":</strong> ")
                        .append(validSolutions.get(i))
                        .append("</div>");
                }
                
                if (displayCount < validSolutions.size()) {
                    html.append("<p>... 还有 ").append(validSolutions.size() - displayCount).append(" 种方案（已省略显示）</p>");
                }
                
                html.append("</div>");
                
                // 如果是第一级别（单门店），记录能够全量满足的门店ID，供后续级别排除使用
                if (splitLevel == 1) {
                    excludedStores.addAll(findFullSatisfactionStores(allItemIds, storeItemMap));
                    log.info("发现 {} 个单门店全量满足的门店: {}", excludedStores.size(), excludedStores);
                    break; // 找到最优解，停止搜索
                }
            }
        }
        
        if (!foundSolution) {
            html.append("<div class='split-level' style='background:#ffebee;color:#c62828;'>")
                .append("<h3>❌ 未找到可行拆单方案</h3>")
                .append("<p>很抱歉，系统未能找到任何门店组合能够满足当前订单的拆单需求。</p>")
                .append("</div>");
        }
        
        html.append("</div></body></html>");
        return html.toString();
    }
    
    /**
     * 生成指定级别的拆分组合
     */
    private List<List<Set<Long>>> generateSplitCombinations(Set<Long> itemIds, int splitLevel) {
        List<List<Set<Long>>> combinations = new ArrayList<>();
        List<Long> itemList = new ArrayList<>(itemIds);
        
        if (splitLevel == 1) {
            // 单个组合：所有明细在一个组
            combinations.add(Arrays.asList(new HashSet<>(itemIds)));
        } else if (splitLevel == itemIds.size()) {
            // 每个明细一个组
            List<Set<Long>> individualGroups = itemList.stream()
                .map(item -> new HashSet<>(Arrays.asList(item)))
                .collect(Collectors.toList());
            combinations.add(individualGroups);
        } else if (splitLevel == 2 && itemIds.size() >= 2) {
            // 两个组的组合：(A,BCD), (AB,CD), (ABC,D) 等
            for (int i = 1; i < itemList.size(); i++) {
                Set<Long> group1 = new HashSet<>(itemList.subList(0, i));
                Set<Long> group2 = new HashSet<>(itemList.subList(i, itemList.size()));
                combinations.add(Arrays.asList(group1, group2));
            }
        }
        // 可以扩展更复杂的组合逻辑
        
        return combinations;
    }
    
    /**
     * 为拆分组合查找门店分配方案（增强版，包含条码信息）
     */
    private List<String> findStoreAssignments(List<Set<Long>> combination, 
                                            Map<Long, Set<Long>> storeItemMap, 
                                            Map<Long, String> storeNameMap,
                                            Map<Long, String> itemLabels,
                                            Map<Long, String> itemBarcodes,
                                            JSONArray orderDetails) {
        List<String> assignments = new ArrayList<>();
        
        // 为每个分组找到能满足的门店
        List<List<Long>> groupStoreOptions = new ArrayList<>();
        
        for (Set<Long> group : combination) {
            List<Long> capableStores = storeItemMap.entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(group))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            if (capableStores.isEmpty()) {
                return new ArrayList<>(); // 无法满足此拆分组合
            }
            
            groupStoreOptions.add(capableStores);
        }
        
        // 生成门店分配的组合（简化版：只取每个组的第一个可用门店）
        List<Long> assignment = groupStoreOptions.stream()
            .map(options -> options.get(0))
            .collect(Collectors.toList());
        
        // 检查是否有重复门店
        Set<Long> uniqueStores = new HashSet<>(assignment);
        if (uniqueStores.size() == assignment.size()) {
            // 生成分配描述（增强版，包含条码信息）
            StringBuilder desc = new StringBuilder();
            for (int i = 0; i < combination.size(); i++) {
                if (i > 0) desc.append(" + ");
                
                Set<Long> group = combination.get(i);
                // 生成包含条码信息的组标签
                String groupLabel = group.stream()
                    .map(itemId -> {
                        String label = itemLabels.get(itemId);
                        String barcode = itemBarcodes.get(itemId);
                        
                        // 从orderDetails中获取库存数量
                        Integer qty = 0;
                        for (int j = 0; j < orderDetails.size(); j++) {
                            JSONObject orderItem = orderDetails.getJSONObject(j);
                            if (orderItem.getLong("id").equals(itemId)) {
                                qty = orderItem.getInteger("qty");
                                break;
                            }
                        }
                        
                        return "<span style='display:inline-block; margin:2px; padding:2px 6px; background:#e3f2fd; border-radius:3px; border-left:2px solid #2196F3;'>" +
                               "📦 " + label + "(" + (barcode != null ? barcode : "N/A") + ")" + 
                               " <span style='color:#FF5722; font-weight:bold; background:#fff3e0; padding:1px 4px; border-radius:2px; font-size:12px;'>[" + qty + "]</span>" +
                               "</span>";
                    })
                    .collect(Collectors.joining(" "));
                
                Long storeId = assignment.get(i);
                String storeName = storeNameMap.get(storeId);
                
                desc.append("[").append(groupLabel).append("]→").append(storeName).append("(").append(storeId).append(")");
            }
            assignments.add(desc.toString());
        }
        
        return assignments;
    }
    
    /**
     * 查找能够单门店全量满足的门店ID集合
     */
    private Set<Long> findFullSatisfactionStores(Set<Long> allItemIds, Map<Long, Set<Long>> storeItemMap) {
        return storeItemMap.entrySet().stream()
            .filter(entry -> entry.getValue().containsAll(allItemIds))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
    
    /**
     * 过滤排除指定的门店，返回新的门店映射
     */
    private Map<Long, Set<Long>> filterExcludedStores(Map<Long, Set<Long>> originalStoreItemMap, Set<Long> excludedStores) {
        if (excludedStores.isEmpty()) {
            return originalStoreItemMap;
        }
        
        return originalStoreItemMap.entrySet().stream()
            .filter(entry -> !excludedStores.contains(entry.getKey()))
            .collect(Collectors.toMap(
                Map.Entry::getKey, 
                Map.Entry::getValue
            ));
    }
    
    /**
     * 获取级别描述
     */
    private String getLevelDescription(int level, int totalItems) {
        if (level == 1) {
            return "🏆 最优方案：单门店全量满足";
        } else if (level == totalItems) {
            return "📋 最细拆分：每个明细独立发货";
        } else {
            return "🔄 组合方案：" + level + "个分组发货";
        }
    }
    
    /**
     * 生成错误信息HTML
     */
    private String generateErrorHtml(String errorMessage, Long orderId) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>智能拆单结果</title></head><body>" +
               "<h2>智能拆单结果 - 订单ID: " + orderId + "</h2>" +
               "<div style='color:red;'>❌ 错误: " + errorMessage + "</div></body></html>";
    }
}