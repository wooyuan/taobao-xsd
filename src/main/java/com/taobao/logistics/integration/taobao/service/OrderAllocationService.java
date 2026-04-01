package com.taobao.logistics.integration.taobao.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 订单智能分配服务
 * 根据门店库存情况智能分配订单到最优门店组合
 */
@Slf4j
@Service
public class OrderAllocationService {
    
    @Autowired
    private TbqtyServices tbqtyServices;
    
    /**
     * 安全地将Object转换为List<JSONObject>
     * @param obj 待转换的对象
     * @return 转换后的List<JSONObject>，如果转换失败则返回空列表
     */
    @SuppressWarnings("unchecked")
    private List<JSONObject> safeCastToListJSONObject(Object obj) {
        if (obj instanceof List) {
            try {
                return (List<JSONObject>) obj;
            } catch (ClassCastException e) {
                log.warn("类型转换失败: 无法将对象转换为List<JSONObject>", e);
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
    
    /**
     * 智能订单分配 - 根据库存情况智能分配订单到门店
     * 按照拆单次数从少到多排序显示结果
     * @param orderId 订单ID
     * @return 生成HTML格式的分配结果
     */
    public String smartOrderAllocation(Long orderId) {
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
            
            // 构建数据结构便于分析
            Map<Long, JSONObject> orderItemMap = new HashMap<>();
            for (int i = 0; i < orderDetails.size(); i++) {
                JSONObject item = orderDetails.getJSONObject(i);
                orderItemMap.put(item.getLong("id"), item);
            }
            
            // 按门店分组库存数据
            Map<Long, Map<String, Object>> storeMap = new HashMap<>();
            Map<Long, Set<Long>> storeItemMap = new HashMap<>();
            
            for (int i = 0; i < storeInventory.size(); i++) {
                JSONObject inventory = storeInventory.getJSONObject(i);
                Long storeId = inventory.getLong("c_store_id");
                Long orderItemId = inventory.getLong("order_item_id");
                
                if (!storeMap.containsKey(storeId)) {
                    Map<String, Object> storeInfo = new HashMap<>();
                    storeInfo.put("store_name", inventory.getString("store_name"));
                    storeInfo.put("items", new ArrayList<JSONObject>());
                    storeMap.put(storeId, storeInfo);
                    storeItemMap.put(storeId, new HashSet<>());
                }
                
                List<JSONObject> items = safeCastToListJSONObject(storeMap.get(storeId).get("items"));
                items.add(inventory);
                storeItemMap.get(storeId).add(orderItemId);
            }
            
            // 执行全面分配分析 - 获取所有可能的分配方案
            AllocationResult result = performComprehensiveAllocation(orderItemMap, storeMap, storeItemMap);
            
            // 生成HTML结果 - 按拆单次数排序显示
            return generateOptimizedAllocationHtml(result, orderId, orderDetails.size());
            
        } catch (Exception e) {
            log.error("订单分配时发生异常，订单ID: {}", orderId, e);
            return generateErrorHtml("系统异常: " + e.getMessage(), orderId);
        }
    }

    /**
     * 智能订单分配 - 商城出库单根据库存情况智能分配订单到门店
     * 按照拆单次数从少到多排序显示结果
     * @param orderId 订单ID
     * @return 生成HTML格式的分配结果
     */
    public String OssmartOrderAllocation(Long orderId) {
        try {
            // 获取订单明细
            JSONArray orderDetails = tbqtyServices.getOsOrderDetails(orderId);
            // 获取满足条件的门店库存
            JSONArray storeInventory = tbqtyServices.getOsStoreInventoryDetails(orderId);

            if (orderDetails.isEmpty()) {
                return generateErrorHtml("未找到订单明细数据", orderId);
            }

            if (storeInventory.isEmpty()) {
                return generateErrorHtml("没有门店有足够的库存满足订单需求", orderId);
            }

            // 构建数据结构便于分析
            Map<Long, JSONObject> orderItemMap = new HashMap<>();
            for (int i = 0; i < orderDetails.size(); i++) {
                JSONObject item = orderDetails.getJSONObject(i);
                orderItemMap.put(item.getLong("id"), item);
            }

            // 按门店分组库存数据
            Map<Long, Map<String, Object>> storeMap = new HashMap<>();
            Map<Long, Set<Long>> storeItemMap = new HashMap<>();

            for (int i = 0; i < storeInventory.size(); i++) {
                JSONObject inventory = storeInventory.getJSONObject(i);
                Long storeId = inventory.getLong("c_store_id");
                Long orderItemId = inventory.getLong("order_item_id");

                if (!storeMap.containsKey(storeId)) {
                    Map<String, Object> storeInfo = new HashMap<>();
                    storeInfo.put("store_name", inventory.getString("store_name"));
                    storeInfo.put("items", new ArrayList<JSONObject>());
                    storeMap.put(storeId, storeInfo);
                    storeItemMap.put(storeId, new HashSet<>());
                }

                List<JSONObject> items = safeCastToListJSONObject(storeMap.get(storeId).get("items"));
                items.add(inventory);
                storeItemMap.get(storeId).add(orderItemId);
            }

            // 执行全面分配分析 - 获取所有可能的分配方案
            AllocationResult result = performComprehensiveAllocation(orderItemMap, storeMap, storeItemMap);

            // 生成HTML结果 - 按拆单次数排序显示
            return generateOptimizedAllocationHtml(result, orderId, orderDetails.size());

        } catch (Exception e) {
            log.error("订单分配时发生异常，订单ID: {}", orderId, e);
            return generateErrorHtml("系统异常: " + e.getMessage(), orderId);
        }
    }
    
    /**
     * 执行全面分配分析 - 获取所有可能的分配方案（优化版：排除单门店全量满足的门店）
     */
    private AllocationResult performComprehensiveAllocation(Map<Long, JSONObject> orderItemMap, 
                                                          Map<Long, Map<String, Object>> storeMap,
                                                          Map<Long, Set<Long>> storeItemMap) {
        
        AllocationResult result = new AllocationResult();
        Set<Long> orderItemIds = orderItemMap.keySet();
        int totalItems = orderItemIds.size();
        
        log.info("开始全面分配分析，订单总明细数: {}，可用门店数: {}", totalItems, storeMap.size());
        log.info("订单明细ID列表: {}", orderItemIds);
        
        // 输出每个门店能满足的明细
        for (Map.Entry<Long, Set<Long>> entry : storeItemMap.entrySet()) {
            Long storeId = entry.getKey();
            Set<Long> itemIds = entry.getValue();
            String storeName = (String) storeMap.get(storeId).get("store_name");
            //log.info("门店 {} (ID: {}) 可满足的明细ID: {}", storeName, storeId, itemIds);
        }
        
        // 策略1: 寻找能满足所有明细的单个门店 (拆儅1次)
        Set<Long> fullSatisfactionStores = new HashSet<>(); // 记录能够全量满足的门店
        for (Map.Entry<Long, Set<Long>> entry : storeItemMap.entrySet()) {
            if (entry.getValue().containsAll(orderItemIds)) {
               // log.info("找到单门店全量满足方案：门店ID {}", entry.getKey());
                StoreAllocation allocation = new StoreAllocation();
                allocation.storeId = entry.getKey();
                allocation.storeName = (String) storeMap.get(entry.getKey()).get("store_name");
                allocation.items = safeCastToListJSONObject(storeMap.get(entry.getKey()).get("items"));
                allocation.itemCount = totalItems;
                allocation.splitCount = 1; // 拆儅1次
                result.fullAllocations.add(allocation);
                fullSatisfactionStores.add(entry.getKey()); // 记录全量满足的门店ID
            } else {
                log.debug("门店ID {} 无法单独满足所有明细，可满足: {}，需要: {}", 
                    entry.getKey(), entry.getValue(), orderItemIds);
            }
        }
        
        // 策略2: 寻找两个门店组合能满足所有明细 (拆儅2次)（排除已能全量满足的门店）
        List<Long> availableStoreIds = storeItemMap.keySet().stream()
            .filter(storeId -> !fullSatisfactionStores.contains(storeId)) // 排除全量满足的门店
            .collect(java.util.stream.Collectors.toList());

                  log.info("策略2：排除 {} 个全量满足门店后，剩余 {} 个门店参与两门店组合计算", 
            fullSatisfactionStores.size(), availableStoreIds.size());
        
        // 用于存储合并后的分配方案，key为门店1的ID
        Map<Long, TwoStoreAllocation> mergedAllocations = new HashMap<>();
        
        for (int i = 0; i < availableStoreIds.size(); i++) {
            for (int j = i + 1; j < availableStoreIds.size(); j++) {
                Long store1 = availableStoreIds.get(i);
                Long store2 = availableStoreIds.get(j);
                Set<Long> combinedItems = new HashSet<>(storeItemMap.get(store1));
                combinedItems.addAll(storeItemMap.get(store2));
                
                if (combinedItems.containsAll(orderItemIds)) {
                    // 检查是否已存在该门店1的分配方案
                    if (mergedAllocations.containsKey(store1)) {
                        // 如果已存在，则将门店2添加到列表中
                        TwoStoreAllocation existingAllocation = mergedAllocations.get(store1);
                        existingAllocation.store2Ids.add(store2);
                        existingAllocation.store2Names.add((String) storeMap.get(store2).get("store_name"));
                        
                        // 计算新添加的门店2的库存
                        int store2Stock = 0;
                        List<JSONObject> store2Items = filterItemsForStore(storeMap.get(store2), store2);
                        for (JSONObject item : store2Items) {
                            Integer qty = item.getInteger("store_qty") != null ? item.getInteger("store_qty") : 0;
                            store2Stock += qty;
                        }
                        existingAllocation.store2Stocks.add(store2Stock);

                        // 合并门店2的商品明细到现有列表中
                        // existingAllocation.store2Items.addAll(store2Items);
                    } else {
                        // 如果不存在，则创建新的分配方案
                        TwoStoreAllocation allocation = new TwoStoreAllocation();
                        allocation.store1Id = store1;
                        allocation.store1Name = (String) storeMap.get(store1).get("store_name");
                        allocation.store1Items = filterItemsForStore(storeMap.get(store1), store1);
                        allocation.store2Ids = new ArrayList<>();
                        allocation.store2Names = new ArrayList<>();
                        allocation.store2Stocks = new ArrayList<>();

                        // 添加门店2并计算其库存
                        allocation.store2Ids.add(store2);
                        allocation.store2Names.add((String) storeMap.get(store2).get("store_name"));

                        // 计算门店2的库存
                        int store2Stock = 0;
                        List<JSONObject> store2Items = filterItemsForStore(storeMap.get(store2), store2);
                        for (JSONObject item : store2Items) {
                            Integer qty = item.getInteger("store_qty") != null ? item.getInteger("store_qty") : 0;
                            store2Stock += qty;
                        }
                        allocation.store2Stocks.add(store2Stock);

                        allocation.store2Items = store2Items;
                        allocation.splitCount = 2; // 拆儅2次
                        
                        // 计算门店1库存合计
                        int store1Stock = 0;
                        for (JSONObject item : allocation.store1Items) {
                            Integer qty = item.getInteger("store_qty") != null ? item.getInteger("store_qty") : 0;
                            store1Stock += qty;
                        }
                        allocation.store1TotalStock = store1Stock;
                        
                        // 计算门店2库存合计（取第一个门店2的库存，因为后续会添加更多门店2）
                        allocation.store2TotalStock = store2Stock;
                        
                        // 计算门店1+门店2库存合计
                        allocation.totalStock = store1Stock + store2Stock;
                        
                        mergedAllocations.put(store1, allocation);
                    }
                }
            }
              // 将合并后的分配方案添加到结果中
        result.twoStoreAllocations.clear(); // 清空原有的结果
        result.twoStoreAllocations.addAll(mergedAllocations.values());
        
        // 按照门店1+门店2库存合计从大到小排序
        result.twoStoreAllocations.sort((a1, a2) -> {
            return Integer.compare(a2.totalStock, a1.totalStock);
        });
        
        
        // log.info("策略2：排除 {} 个全量满足门店后，剩余 {} 个门店参与两门店组合计算", 
        //     fullSatisfactionStores.size(), availableStoreIds.size());
        
        // for (int i = 0; i < availableStoreIds.size(); i++) {
        //     for (int j = i + 1; j < availableStoreIds.size(); j++) {
        //         Long store1 = availableStoreIds.get(i);
        //         Long store2 = availableStoreIds.get(j);
        //         Set<Long> combinedItems = new HashSet<>(storeItemMap.get(store1));
        //         combinedItems.addAll(storeItemMap.get(store2));
                
        //         if (combinedItems.containsAll(orderItemIds)) {
        //             TwoStoreAllocation allocation = new TwoStoreAllocation();
        //             allocation.store1Id = store1;
        //             allocation.store1Name = (String) storeMap.get(store1).get("store_name");
        //             allocation.store1Items = filterItemsForStore(storeMap.get(store1), store1);
        //             allocation.store2Id = store2;
        //             allocation.store2Name = (String) storeMap.get(store2).get("store_name");
        //             allocation.store2Items = filterItemsForStore(storeMap.get(store2), store2);
        //             allocation.splitCount = 2; // 拆儅2次
        //             result.twoStoreAllocations.add(allocation);
        //         }
        //     }
        }
        
        //策略3: 寻找三个门店组合能满足所有明细 (拆儅3次)（排除已能全量满足的门店）
        log.info("策略3：排除 {} 个全量满足门店后，剩余 {} 个门店参与三门店组合计算", 
            fullSatisfactionStores.size(), availableStoreIds.size());
        if(orderItemIds.size()>4){
        for (int i = 0; i < availableStoreIds.size(); i++) {
            for (int j = i + 1; j < availableStoreIds.size(); j++) {
                for (int k = j + 1; k < availableStoreIds.size(); k++) {
                    Long store1 = availableStoreIds.get(i);
                    Long store2 = availableStoreIds.get(j);
                    Long store3 = availableStoreIds.get(k);
                    Set<Long> combinedItems = new HashSet<>(storeItemMap.get(store1));
                    combinedItems.addAll(storeItemMap.get(store2));
                    combinedItems.addAll(storeItemMap.get(store3));
                    
                    if (combinedItems.containsAll(orderItemIds)) {
                        ThreeStoreAllocation allocation = new ThreeStoreAllocation();
                        allocation.store1Id = store1;
                        allocation.store1Name = (String) storeMap.get(store1).get("store_name");
                        allocation.store1Items = filterItemsForStore(storeMap.get(store1), store1);
                        allocation.store2Id = store2;
                        allocation.store2Name = (String) storeMap.get(store2).get("store_name");
                        allocation.store2Items = filterItemsForStore(storeMap.get(store2), store2);
                        allocation.store3Id = store3;
                        allocation.store3Name = (String) storeMap.get(store3).get("store_name");
                        allocation.store3Items = filterItemsForStore(storeMap.get(store3), store3);
                        allocation.splitCount = 3; // 拆儅3次
                        result.threeStoreAllocations.add(allocation);
                    }
                }
            }
        }
        }
        
        // 设置分配类型（优先级：拆单次数最少）
        if (!result.fullAllocations.isEmpty()) {
            result.allocationType = 1;
            log.info("找到 {} 个单门店全量满足方案", result.fullAllocations.size());
        } else if (!result.twoStoreAllocations.isEmpty()) {
            result.allocationType = 2;
            log.info("找到 {} 个两门店组合方案", result.twoStoreAllocations.size());
        } else if (!result.threeStoreAllocations.isEmpty()) {
            result.allocationType = 3;
            log.info("找到 {} 个三门店组合方案", result.threeStoreAllocations.size());
        } else {
            result.allocationType = 0; // 无法分配
            log.warn("未找到任何可行的分配方案");
        }
        
        return result;
    }

    
    
    /**
     * 生成优化的分配结果HTML - 按拆单次数排序显示（一行4列卡片布局）
     */
    private String generateOptimizedAllocationHtml(AllocationResult result, Long orderId, int totalOrderItems) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<title>订单智能分配结果</title>")
            .append("<style>")
            .append("body{font-family:'Microsoft YaHei',Arial,sans-serif;margin:20px;background:#f5f5f5;}")
            .append(".container{max-width:1200px;margin:0 auto;background:white;padding:25px;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1);}")
            .append(".header{text-align:center;margin-bottom:30px;padding-bottom:20px;border-bottom:2px solid #e6f3ff;}")
            .append(".order-info{background:#e6f3ff;padding:15px;margin:15px 0;border-radius:5px;border-left:4px solid #2196F3;}")
            .append(".strategy-1{background:linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);border-left:4px solid #28a745;}")
            .append(".strategy-2{background:linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);border-left:4px solid #ffc107;}")
            .append(".strategy-3{background:linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%);border-left:4px solid #dc3545;}")
            .append(".strategy{padding:15px;margin:15px 0;border-radius:8px;}")
            .append(".store-combo{background:#f8f9fa;margin:10px 0;padding:15px;border-radius:5px;border:1px solid #dee2e6;}")
            .append(".solutions-grid{display:grid;grid-template-columns:repeat(3, 1fr);gap:15px;margin:15px 0;}")
            .append(".solution-card{background:#ffffff;border:1px solid #ddd;border-radius:6px;padding:10px;box-shadow:0 2px 4px rgba(0,0,0,0.1);transition:all 0.3s ease;}")
            .append(".solution-card:hover{transform:translateY(-2px);box-shadow:0 4px 8px rgba(0,0,0,0.15);border-color:#007bff;}")
            .append(".solution-title{font-weight:bold;color:#007bff;margin-bottom:6px;font-size:14px;border-bottom:1px solid #e9ecef;padding-bottom:4px;}")
            .append(".solution-store{color:#333;margin:4px 0;font-weight:500;font-size:12px;background:#f8f9fa;padding:8px;border-radius:4px;border-left:3px solid #28a745;}")
            .append(".split-badge{display:inline-block;background:#007bff;color:white;padding:3px 8px;border-radius:12px;font-size:12px;margin-left:10px;}")
            .append(".success{color:#28a745;} .warning{color:#ffc107;} .danger{color:#dc3545;}")
            .append(".no-solution{background:#f8d7da;color:#721c24;padding:20px;text-align:center;border-radius:8px;border:1px solid #f5c6cb;}")
            .append(".summary{background:#e9ecef;padding:10px;border-radius:5px;margin:10px 0;}")
            .append(".item-grid{display:grid;grid-template-columns:repeat(3, 1fr);gap:8px;margin:6px 0;}")
            .append(".item-card{background:#ffffff;border:1px solid #dee2e6;border-radius:6px;padding:8px;font-size:11px;transition:all 0.3s ease;box-shadow:0 1px 3px rgba(0,0,0,0.1);}")
            .append(".item-card:hover{background:#f8f9fa;transform:translateY(-1px);box-shadow:0 2px 6px rgba(0,0,0,0.15);border-color:#007bff;}")
            .append(".item-header{font-weight:bold;color:#333;margin-bottom:4px;font-size:11px;border-bottom:1px solid #e9ecef;padding-bottom:2px;}")
            .append(".item-detail{margin:2px 0;font-size:10px;color:#666;}")
            .append(".stock-highlight{color:#FF5722 !important;background:linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%) !important;padding:4px 10px !important;border-radius:6px !important;font-weight:bold !important;font-size:13px !important;border:1px solid #ffab40 !important;display:inline-block !important;box-shadow:0 1px 3px rgba(255,87,34,0.2) !important;text-shadow:0 1px 1px rgba(255,255,255,0.8) !important;}")
            .append(".store-section{margin:15px 0;padding:15px;background:#fafafa;border-radius:5px;border-left:3px solid #007bff;}")
            .append("@media (max-width: 1200px) { .item-grid { grid-template-columns: repeat(3, 1fr); } .solutions-grid { grid-template-columns: repeat(3, 1fr); } }")
            .append("@media (max-width: 900px) { .item-grid { grid-template-columns: repeat(2, 1fr); } .solutions-grid { grid-template-columns: repeat(2, 1fr); } }")
            .append("@media (max-width: 600px) { .item-grid { grid-template-columns: 1fr; } .solutions-grid { grid-template-columns: 1fr; } }")
            .append("</style></head><body>")
            .append("<div class='container'>")
            //.append("<div class='header'><h1>📦 订单智能分配系统</h1></div>")
            .append("<div class='order-info'><strong>订单ID:</strong> ").append(orderId)
            .append(" | <strong>订单明细数:</strong> ").append(totalOrderItems)
            .append(" | <strong>分析时间:</strong> ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</div>");
        
        // 按拆单次数从少到多显示结果
        boolean hasResults = false;
        
        // 策略1: 单门店全量满足 (拆儅1次) - 最优方案
        // if (!result.fullAllocations.isEmpty()) {
        //     hasResults = true;
        //     // 限制显示前8条
        //     int displayCount = Math.min(8, result.fullAllocations.size());
        //     html.append("<div class='strategy strategy-1'>")
        //         .append("<h3>🏆 最优方案：单门店全量满足 <span class='split-badge'>拆儅1次</span></h3>")
        //         .append("<div class='summary'>找到 <strong>").append(result.fullAllocations.size())
        //         .append("</strong> 个门店可以单独满足所有订单需求，无需拆单！")
        //         .append(displayCount < result.fullAllocations.size() ? 
        //             "（显示前" + displayCount + "条）" : "")
        //         .append("</div>")
        //         .append("<div class='solutions-grid'>");
            
        //     for (int i = 0; i < displayCount; i++) {
        //         StoreAllocation allocation = result.fullAllocations.get(i);
        //         html.append("<div class='solution-card'>")
        //             .append("<div class='solution-title'>方案 ").append(i + 1).append("</div>")
        //             .append("<div class='solution-store'>").append(allocation.storeName)
        //             .append(" (ID: ").append(allocation.storeId).append(")")
        //             .append("<br>满足所有 ").append(allocation.itemCount).append(" 个明细</div>")
        //             .append(generateItemCards(allocation.items))
        //             .append("</div>");
        //     }
        //     html.append("</div></div>");
        // }
        
        // 策略2.1: 最佳推荐（从所有两门店组合中选择库存合计最大的）
        if (!result.twoStoreAllocations.isEmpty()) {
            hasResults = true;
            // 从所有两门店组合中选择库存合计最大的方案
            TwoStoreAllocation bestAllocation = null;
            int maxTotalStock = 0;
            for (TwoStoreAllocation allocation : result.twoStoreAllocations) {
                if (allocation.totalStock > maxTotalStock) {
                    maxTotalStock = allocation.totalStock;
                    bestAllocation = allocation;
                }
            }
            html.append("<div class='strategy strategy-1'>")
                .append("<h3>🏆 最佳推荐：两门店组合满足（库存合计最大） <span class='split-badge'>拆单2次</span></h3>")
                .append("<div class='summary'>从所有满足条件的门店组合中，")
                .append("选择门店1+门店2库存合计最大的方案作为最佳推荐")
                .append("</div>")
                .append("<div class='solutions-grid'>");
            
            // 显示最佳推荐方案
            html.append("<div class='solution-card' style='padding:12px;'>")
                .append("<div class='solution-title' style='margin-bottom:8px;'>最佳方案</div>")
                .append("<div style='display:flex;flex-wrap:wrap;gap:10px;margin-bottom:10px;'>")
                .append("<div class='solution-store' style='flex:1;min-width:200px;'>门店1: " + bestAllocation.store1Name)
                .append(" (ID: " + bestAllocation.store1Id + ")")
                .append("<br>库存: " + bestAllocation.store1TotalStock + "</div>");
            
            // 对于最佳方案，选择库存最大的门店2
            if (bestAllocation.store2Names != null && !bestAllocation.store2Names.isEmpty()) {
                // 计算每个门店2的库存，选择库存最大的那个
                String bestStore2Name = null;
                Long bestStore2Id = null;
                int maxStore2Stock = 0;
                
                // 遍历所有可能的门店2，找出库存最大的那个
                for (int j = 0; j < bestAllocation.store2Ids.size(); j++) {
                    Long store2Id = bestAllocation.store2Ids.get(j);
                    String store2Name = bestAllocation.store2Names.get(j);
                    
                    // 从store2Stocks中获取该门店的库存
                    int store2Stock = 0;
                    if (bestAllocation.store2Stocks != null && j < bestAllocation.store2Stocks.size()) {
                        store2Stock = bestAllocation.store2Stocks.get(j);
                    }
                    
                    if (store2Stock > maxStore2Stock) {
                        maxStore2Stock = store2Stock;
                        bestStore2Name = store2Name;
                        bestStore2Id = store2Id;
                    }
                }
                
                // 如果找到最佳门店2，则显示
                if (bestStore2Name != null) {
                    html.append("<div class='solution-store' style='flex:1;min-width:200px;'>门店2: " + bestStore2Name)
                        .append(" (ID: " + bestStore2Id + ")")
                        .append("<br>库存: " + maxStore2Stock + "</div>");
                } else {
                    // 如果没有找到，显示第一个门店2
                    String store2Name = bestAllocation.store2Names.get(0);
                    Long store2Id = bestAllocation.store2Ids.get(0);
                    html.append("<div class='solution-store' style='flex:1;min-width:200px;'>门店2: " + store2Name)
                        .append(" (ID: " + store2Id + ")</div>");
                }
            }
            html.append("</div>");
            
            // 显示商品库存明细
            html.append("<div style='display:flex;flex-wrap:wrap;gap:15px;'>")
                .append("<div style='flex:1;min-width:300px;'>")
                .append("<div class='solution-store' style='margin-bottom:8px;'>门店1商品库存</div>")
                .append(generateItemCards(bestAllocation.store1Items))
                .append("</div>")
                .append("<div style='flex:1;min-width:300px;'>")
                .append("<div class='solution-store' style='margin-bottom:8px;'>门店2商品库存</div>");
            // 这里需要获取门店2的商品明细
            // 暂时使用bestAllocation.store2Items，实际项目中可能需要根据选择的门店2获取对应的商品明细
            html.append(generateItemCards(bestAllocation.store2Items))
                .append("</div>")
                .append("</div>")
                .append("</div>");
            
            html.append("</div></div>");
        }
        
        // 策略2: 所有两门店组合满足 (拆儅2次)
        if (!result.twoStoreAllocations.isEmpty()) {
            hasResults = true;
            // 限制显示前8条
            int displayCount = Math.min(10, result.twoStoreAllocations.size());
            html.append("<div class='strategy strategy-2'>")
                .append("<h3>🔄 所有两门店组合满足 <span class='split-badge'>拆单2次</span></h3>")
                .append("<div class='summary'>找到 <strong>").append(result.twoStoreAllocations.size())
                .append("</strong> 个两门店组合方案，需要拆单2次发货")
                .append(displayCount < result.twoStoreAllocations.size() ? 
                    "。（显示前" + displayCount + "条）" : "。")
                .append("</div>")
                .append("<div class='solutions-grid'>");
            
            // for (int i = 0; i < displayCount; i++) {
            //     TwoStoreAllocation allocation = result.twoStoreAllocations.get(i);
            //     html.append("<div class='solution-card'>")
            //         .append("<div class='solution-title'>方案 ").append(i + 1).append("</div>")
            //         .append("<div class='solution-store'>门店1: ").append(allocation.store1Name)
            //         .append(" (ID: ").append(allocation.store1Id).append(")")
            //         .append("<br>门店2: ").append(allocation.store2Name)
            //         .append(" (ID: ").append(allocation.store2Id).append(")</div>")
            //         .append(generateItemCards(allocation.store1Items))
            //         .append("<hr style='margin:8px 0;border:none;border-top:1px solid #eee;'>")
            //         .append(generateItemCards(allocation.store2Items))
            //         .append("</div>");
            // }
             for (int i = 0; i < displayCount; i++) {
                TwoStoreAllocation allocation = result.twoStoreAllocations.get(i);
                html.append("<div class='solution-card'>")
                    .append("<div class='solution-title'>方案 ").append(i + 1).append("</div>")
                    .append("<div class='solution-store'>门店1: ").append(allocation.store1Name)
                    .append(" (ID: ").append(allocation.store1Id).append(")</div>");
                
                // 为门店2创建下拉框选择
                if (allocation.store2Names != null && !allocation.store2Names.isEmpty()) {
                    html.append("<div class='solution-store'>门店2: ")
                        .append("<select onchange='updateStore2Info(this)'>");

                    for (int j = 0; j < allocation.store2Names.size(); j++) {
                        html.append("<option value='").append(j).append("'>")
                            .append(allocation.store2Names.get(j))
                            .append(" (ID: ").append(allocation.store2Ids.get(j)).append(")")
                            .append("</option>");
                    }

                    html.append("</select></div>");

                    // 添加隐藏的门店2信息容器，用于动态显示选中的门店信息
                    html.append("<div id='store2-info-").append(i).append("' class='store2-details'>");
                    // 默认显示第一个门店2的信息
                    html.append("<div class='solution-store'>选中门店: ").append(allocation.store2Names.get(0))
                        .append(" (ID: ").append(allocation.store2Ids.get(0)).append(")</div>");
                    html.append("</div>");
                }

                // 显示门店1的商品明细
                html.append(generateItemCards(allocation.store1Items));
                html.append("<hr style='margin:8px 0;border:none;border-top:1px solid #eee;'>");

                // 显示门店2的商品明细
                //html.append(generateItemCards(allocation.store2Items));
                html.append("</div>");
            }
            html.append("</div></div>");
        }
        
        // 策略3: 三门店组合满足 (拆儅3次)
        if (!result.threeStoreAllocations.isEmpty()) {
            hasResults = true;
            // 限制显示前8条
            int displayCount = Math.min(8, result.threeStoreAllocations.size());
            html.append("<div class='strategy strategy-3'>")
                .append("<h3>⚠️ 备用方案：三门店组合满足 <span class='split-badge'>拆单3次</span></h3>")
                .append("<div class='summary'>找到 <strong>").append(result.threeStoreAllocations.size())
                .append("</strong> 个三门店组合方案，需要拆单3次发货")
                .append(displayCount < result.threeStoreAllocations.size() ? 
                    "。（显示前" + displayCount + "条）" : "。")
                .append("</div>")
                .append("<div class='solutions-grid'>");
            
            for (int i = 0; i < displayCount; i++) {
                ThreeStoreAllocation allocation = result.threeStoreAllocations.get(i);
                html.append("<div class='solution-card'>")
                    .append("<div class='solution-title'>方案 ").append(i + 1).append("</div>")
                    .append("<div class='solution-store'>门店1: ").append(allocation.store1Name)
                    .append(" (ID: ").append(allocation.store1Id).append(")")
                    .append("<br>门店2: ").append(allocation.store2Name)
                    .append(" (ID: ").append(allocation.store2Id).append(")")
                    .append("<br>门店3: ").append(allocation.store3Name)
                    .append(" (ID: ").append(allocation.store3Id).append(")</div>")
                    .append(generateItemCards(allocation.store1Items))
                    .append("<hr style='margin:5px 0;border:none;border-top:1px solid #eee;'>")
                    .append(generateItemCards(allocation.store2Items))
                    .append("<hr style='margin:5px 0;border:none;border-top:1px solid #eee;'>")
                    .append(generateItemCards(allocation.store3Items))
                    .append("</div>");
            }
            html.append("</div></div>");
        }
        
        // 无可行方案
        if (!hasResults) {
            html.append("<div class='no-solution'>")
                .append("<h3>❌ 无可行分配方案</h3>")
                .append("<p>很抱歉，系统未找到任何门店组合能够满足当前订单的所有明细需求。</p>")
                .append("<p>建议：</p><ul>")
                .append("<li>检查库存是否充足</li>")
                .append("<li>联系供应商进行补货</li>")
                .append("<li>考虑修改订单数量或商品</li>")
                .append("</ul></div>");
        }
        
        html.append("</div></body></html>");
        return html.toString();
    }
    
    /**
     * 过滤门店的商品明细
     */
    private List<JSONObject> filterItemsForStore(Map<String, Object> storeInfo, Long storeId) {
        return safeCastToListJSONObject(storeInfo.get("items"));
    }
    
    /**
     * 生成商品明细卡片（优化版：只显示条码号和库存，库存醒目显示，按库存从大到小排序）
     */
    private String generateItemCards(List<JSONObject> items) {
        StringBuilder cards = new StringBuilder();
        cards.append("<div class='item-grid'>");
        
        // 按库存从大到小排序
        List<JSONObject> sortedItems = new ArrayList<>(items);
        sortedItems.sort((o1, o2) -> {
            Integer qty1 = o1.getInteger("store_qty") != null ? o1.getInteger("store_qty") : 0;
            Integer qty2 = o2.getInteger("store_qty") != null ? o2.getInteger("store_qty") : 0;
            return qty2.compareTo(qty1); // 从大到小排序
        });
        
        for (JSONObject item : sortedItems) {
            String barcodeNo = item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A";
            Integer storeQty = item.getInteger("store_qty") != null ? item.getInteger("store_qty") : 0;
            
            cards.append("<div class='item-card'>")
                .append("<div class='item-header'>📦 条码: <span style='color:#2196F3; font-weight:bold;'>").append(barcodeNo).append("</span></div>")
                .append("<div class='item-detail'>📊 库存: <span class='stock-highlight'>").append(storeQty).append("</span></div>")
                .append("</div>");
        }
        
        cards.append("</div>");
        return cards.toString();
    }
    
    // /**
    //  * 生成两门店组合HTML（一行4列卡片布局）
    //  */
    // private String generateTwoStoreComboHtml(TwoStoreAllocation allocation, int index) {
    //     StringBuilder html = new StringBuilder();
    //     html.append("<div class='store-combo'>")
    //         .append("<h4>方案 ").append(index).append(": 两门店组合方案</h4>")
    //         .append("<div class='store-section'>")
    //         .append("<h5>🏢 门店1: ").append(allocation.store1Name).append(" (ID: ").append(allocation.store1Id).append(")</h5>")
    //         .append(generateItemCards(allocation.store1Items))
    //         .append("</div>")
    //         .append("<div class='store-section'>")
    //         .append("<h5>🏢 门店2: ").append(allocation.store2Name).append(" (ID: ").append(allocation.store2Id).append(")</h5>")
    //         .append(generateItemCards(allocation.store2Items))
    //         .append("</div>")
    //         .append("</div>");
    //     return html.toString();
    // }
    
    /**
     * 生成三门店组合HTML（一行4列卡片布局）
     */
    private String generateThreeStoreComboHtml(ThreeStoreAllocation allocation, int index) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='store-combo'>")
            .append("<h4>方案 ").append(index).append(": 三门店组合方案</h4>");
        
        // 门店1
        html.append("<div class='store-section'>")
            .append("<h5>🏢 门店1: ").append(allocation.store1Name).append(" (ID: ").append(allocation.store1Id).append(")</h5>")
            .append(generateItemCards(allocation.store1Items))
            .append("</div>");
        
        // 门店2
        html.append("<div class='store-section'>")
            .append("<h5>🏢 门店2: ").append(allocation.store2Name).append(" (ID: ").append(allocation.store2Id).append(")</h5>")
            .append(generateItemCards(allocation.store2Items))
            .append("</div>");
        
        // 门店3
        html.append("<div class='store-section'>")
            .append("<h5>🏢 门店3: ").append(allocation.store3Name).append(" (ID: ").append(allocation.store3Id).append(")</h5>")
            .append(generateItemCards(allocation.store3Items))
            .append("</div>");
        
        html.append("</div>");
        return html.toString();
    }
    
    /**
     * 生成错误信息HTML
     */
    private String generateErrorHtml(String errorMessage, Long orderId) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<title>订单分配结果</title>")
            .append("<style>body{font-family:Arial,sans-serif;margin:20px;}")
            .append(".error{color:red;background:#ffe6e6;padding:10px;border:1px solid #ff0000;}</style>")
            .append("</head><body>")
            .append("<h2>订单分配结果 - 订单ID: ").append(orderId).append("</h2>")
            .append("<div class='error'>错误: ").append(errorMessage).append("</div>")
            .append("</body></html>");
        return html.toString();
    }
    
    // /**
    //  * 生成分配结果HTML
    //  */
    // private String generateAllocationHtml(AllocationResult result, Long orderId) {
    //     StringBuilder html = new StringBuilder();
    //     html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
    //         .append("<title>订单分配结果</title>")
    //         .append("<style>")
    //         .append("body{font-family:Arial,sans-serif;margin:20px;}")
    //         .append("table{border-collapse:collapse;width:100%;margin:10px 0;}")
    //         .append("th,td{border:1px solid #ddd;padding:8px;text-align:left;}")
    //         .append("th{background-color:#f2f2f2;}")
    //         .append(".strategy{background-color:#e6f3ff;padding:10px;margin:10px 0;border-radius:5px;}")
    //         .append(".store{background-color:#f9f9f9;margin:5px 0;padding:10px;border-radius:3px;}")
    //         .append(".success{color:green;} .warning{color:orange;} .error{color:red;}")
    //         .append("</style></head><body>")
    //         .append("<h2>订单分配结果 - 订单ID: ").append(orderId).append("</h2>");
        
    //     switch (result.allocationType) {
    //         case 1:
    //             html.append("<div class='strategy success'><h3>策略1: 单个门店全量满足</h3></div>");
    //             for (StoreAllocation allocation : result.fullAllocations) {
    //                 html.append(generateStoreAllocationHtml(allocation));
    //             }
    //             break;
    //         case 2:
    //             html.append("<div class='strategy warning'><h3>策略2: 两个门店组合满足</h3></div>");
    //             for (TwoStoreAllocation allocation : result.twoStoreAllocations) {
    //                 html.append(generateTwoStoreAllocationHtml(allocation));
    //             }
    //             break;
    //         case 3:
    //             html.append("<div class='strategy warning'><h3>策略3: 三个门店组合满足</h3></div>");
    //             for (ThreeStoreAllocation allocation : result.threeStoreAllocations) {
    //                 html.append(generateThreeStoreAllocationHtml(allocation));
    //             }
    //             break;
    //         default:
    //             html.append("<div class='strategy error'><h3>无法分配</h3>")
    //                 .append("<p>没有找到合适的门店组合来满足订单需求。</p></div>");
    //     }
        
    //     html.append("</body></html>");
    //     return html.toString();
    // }
    
    /**
     * 生成单门店分配HTML（包含条码号）
     */
    private String generateStoreAllocationHtml(StoreAllocation allocation) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='store'>")
            .append("<h4>门店: ").append(allocation.storeName)
            .append(" (ID: ").append(allocation.storeId).append(")")
            .append(" - 可满足所有 ").append(allocation.itemCount).append(" 个明细</h4>")
            .append("<table><tr><th>订单明细ID</th><th>条码ID</th><th>条码号</th><th>库存数量</th></tr>");
        
        for (JSONObject item : allocation.items) {
            html.append("<tr>")
                .append("<td>").append(item.getLong("order_item_id")).append("</td>")
                .append("<td>").append(item.getLong("m_productalias_id")).append("</td>")
                .append("<td>").append(item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A").append("</td>")
                .append("<td>").append(item.getInteger("store_qty")).append("</td>")
                .append("</tr>");
        }
        
        html.append("</table></div>");
        return html.toString();
    }
    
    /**
     * 生成两门店分配HTML（包含条码号）
     */
    // private String generateTwoStoreAllocationHtml(TwoStoreAllocation allocation) {
    //     StringBuilder html = new StringBuilder();
    //     html.append("<div class='store'>")
    //         .append("<h4>门店组合方案</h4>")
    //         .append("<h5>门店1: ").append(allocation.store1Name).append(" (ID: ").append(allocation.store1Id).append(")</h5>")
    //         .append("<table><tr><th>订单明细ID</th><th>条码ID</th><th>条码号</th><th>库存数量</th></tr>");
        
    //     for (JSONObject item : allocation.store1Items) {
    //         html.append("<tr>")
    //             .append("<td>").append(item.getLong("order_item_id")).append("</td>")
    //             .append("<td>").append(item.getLong("m_productalias_id")).append("</td>")
    //             .append("<td>").append(item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A").append("</td>")
    //             .append("<td>").append(item.getInteger("store_qty")).append("</td>")
    //             .append("</tr>");
    //     }
        
    //     html.append("</table>")
    //         .append("<h5>门店2: ").append(allocation.store2Name).append(" (ID: ").append(allocation.store2Id).append(")</h5>")
    //         .append("<table><tr><th>订单明细ID</th><th>条码ID</th><th>条码号</th><th>库存数量</th></tr>");
        
    //     for (JSONObject item : allocation.store2Items) {
    //         html.append("<tr>")
    //             .append("<td>").append(item.getLong("order_item_id")).append("</td>")
    //             .append("<td>").append(item.getLong("m_productalias_id")).append("</td>")
    //             .append("<td>").append(item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A").append("</td>")
    //             .append("<td>").append(item.getInteger("store_qty")).append("</td>")
    //             .append("</tr>");
    //     }
        
    //     html.append("</table></div>");
    //     return html.toString();
    // }
    
    /**
     * 生成三门店分配HTML（包含条码号）
     */
    private String generateThreeStoreAllocationHtml(ThreeStoreAllocation allocation) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='store'>")
            .append("<h4>门店组合方案（三门店）</h4>");
        
        // 门店1
        html.append("<h5>门店1: ").append(allocation.store1Name).append(" (ID: ").append(allocation.store1Id).append(")</h5>")
            .append("<table><tr><th>订单明细ID</th><th>条码ID</th><th>条码号</th><th>库存数量</th></tr>");
        for (JSONObject item : allocation.store1Items) {
            html.append("<tr>")
                .append("<td>").append(item.getLong("order_item_id")).append("</td>")
                .append("<td>").append(item.getLong("m_productalias_id")).append("</td>")
                .append("<td>").append(item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A").append("</td>")
                .append("<td>").append(item.getInteger("store_qty")).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");
        
        // 门店2
        html.append("<h5>门店2: ").append(allocation.store2Name).append(" (ID: ").append(allocation.store2Id).append(")</h5>")
            .append("<table><tr><th>订单明细ID</th><th>条码ID</th><th>条码号</th><th>库存数量</th></tr>");
        for (JSONObject item : allocation.store2Items) {
            html.append("<tr>")
                .append("<td>").append(item.getLong("order_item_id")).append("</td>")
                .append("<td>").append(item.getLong("m_productalias_id")).append("</td>")
                .append("<td>").append(item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A").append("</td>")
                .append("<td>").append(item.getInteger("store_qty")).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");
        
        // 门店3
        html.append("<h5>门店3: ").append(allocation.store3Name).append(" (ID: ").append(allocation.store3Id).append(")</h5>")
            .append("<table><tr><th>订单明细ID</th><th>条码ID</th><th>条码号</th><th>库存数量</th></tr>");
        for (JSONObject item : allocation.store3Items) {
            html.append("<tr>")
                .append("<td>").append(item.getLong("order_item_id")).append("</td>")
                .append("<td>").append(item.getLong("m_productalias_id")).append("</td>")
                .append("<td>").append(item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A").append("</td>")
                .append("<td>").append(item.getInteger("store_qty")).append("</td>")
                .append("</tr>");
        }
        html.append("</table></div>");
        return html.toString();
    }
    
    // 内部类定义
    private static class AllocationResult {
        int allocationType = 0; // 0-无法分配, 1-单门店, 2-两门店, 3-三门店
        List<StoreAllocation> fullAllocations = new ArrayList<>();
        List<TwoStoreAllocation> twoStoreAllocations = new ArrayList<>();
        List<ThreeStoreAllocation> threeStoreAllocations = new ArrayList<>();
    }
    
    private static class StoreAllocation {
        Long storeId;
        String storeName;
        List<JSONObject> items;
        int itemCount;
        int splitCount; // 拆单次数
    }
    
    private static class TwoStoreAllocation {
        Long store1Id;
        String store1Name;
        List<JSONObject> store1Items;
        int store1TotalStock; // 门店1库存合计
        List<Long> store2Ids;     // 修改为列表形式
        List<String> store2Names; // 修改为列表形式
        List<JSONObject> store2Items;
        List<Integer> store2Stocks; // 门店2库存列表
        int splitCount; // 拆单次数
        int store2TotalStock; // 门店2库存合计
        int totalStock; // 门店1+门店2库存合计
    }
    
    private static class ThreeStoreAllocation {
        Long store1Id;
        String store1Name;
        List<JSONObject> store1Items;
        Long store2Id;
        String store2Name;
        List<JSONObject> store2Items;
        Long store3Id;
        String store3Name;
        List<JSONObject> store3Items;
        int splitCount; // 拆单次数
    }
}