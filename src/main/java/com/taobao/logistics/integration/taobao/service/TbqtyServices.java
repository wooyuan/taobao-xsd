package com.taobao.logistics.integration.taobao.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.logistics.integration.StoreGroup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 淘宝小时达门店查询服务
 * 优化后的版本支持获取所有分页数据并存储到Oracle数据库
 *
 */
@Slf4j
@Service
public class TbqtyServices {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
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
     * 查询订单明细数据
     * @param orderId 订单ID
     * @return 包含订单明细数据的JSON数组
     */
    public JSONArray getOrderDetails(Long orderId) {
        String sql = "SELECT b.id, b.qty, b.m_productalias_id,c.no " +
                    "FROM M_TBORDER@bj_70 a, M_TBORDERitem@bj_70 b,m_product_alias@bj_70 c  " +
                    "WHERE a.id = b.m_tborder_id " +
                    "AND b.m_productalias_id<>697028 "+
                    "AND c.id=b.m_productalias_id "+
                    "AND a.id = ?";
        
        try {
            List<JSONObject> orderDetails = jdbcTemplate.query(sql, 
                new Object[]{orderId},
                (rs, rowNum) -> {
                    JSONObject detail = new JSONObject();
                    detail.put("id", rs.getLong("id"));                    // 订单明细ID
                    detail.put("qty", rs.getInt("qty"));                   // 发货数量
                    detail.put("m_productalias_id", rs.getLong("m_productalias_id")); // 条码ID
                    detail.put("barcode_no", rs.getString("no"));            // 条码号
                    return detail;
                });
            
            JSONArray resultArray = new JSONArray();
            resultArray.addAll(orderDetails);
            
            log.info("成功查询到订单 {} 的 {} 条明细数据", orderId, orderDetails.size());
            return resultArray;
            
        } catch (Exception e) {
            log.error("查询订单明细数据时发生异常，订单ID: {}", orderId, e);
            return new JSONArray();
        }
    }

    /**
     * 查询商城出库单明细
     * @param orderId 订单ID
     * @return 包含订单明细数据的JSON数组
     */
    public JSONArray getOsOrderDetails(Long orderId) {
        String sql = "  select b.id, b.ord_qty as qty, b.m_productalias_id,c.no  " +
                "  from OS_ORDER@bj_70 a, OS_ORDERITEM@bj_70 B, m_product_alias@bj_70 c " +
                "  where a.id = b.os_order_id " +
                "  and b.m_productalias_id <> 697028 " +
                "  and c.id = b.m_productalias_id " +
                "  and a.id=?";
        try {
            List<JSONObject> orderDetails = jdbcTemplate.query(sql,
                    new Object[]{orderId},
                    (rs, rowNum) -> {
                        JSONObject detail = new JSONObject();
                        detail.put("id", rs.getLong("id"));                    // 订单明细ID
                        detail.put("qty", rs.getInt("qty"));                   // 发货数量
                        detail.put("m_productalias_id", rs.getLong("m_productalias_id")); // 条码ID
                        detail.put("barcode_no", rs.getString("no"));            // 条码号
                        return detail;
                    });

            JSONArray resultArray = new JSONArray();
            resultArray.addAll(orderDetails);

            log.info("成功查询到订单 {} 的 {} 条明细数据", orderId, orderDetails.size());
            return resultArray;

        } catch (Exception e) {
            log.error("查询订单明细数据时发生异常，订单ID: {}", orderId, e);
            return new JSONArray();
        }
    }
    
    /**
     * 查询当前有哪些门店库存满足条件的商品明细
     * @param orderId 订单ID
     * @return 包含门店库存满足条件的商品明细数据的JSON数组
     */
    public JSONArray getStoreInventoryDetails(Long orderId) {
        String sql = "SELECT d.id as c_store_id, d.name, c.qty, b.id, b.m_productalias_id,e.no " +
                    "FROM M_TBORDER@bj_70 a, M_TBORDERitem@bj_70 b, fa_storage@bj_70 c, c_store@bj_70 d,m_product_alias@bj_70 e,C_KCSTORE@bj_70 f  " +
                    "WHERE a.id = b.m_tborder_id " +
                    "AND b.m_product_id = c.m_product_id " +
                    "AND b.m_attributesetinstance_id = c.m_attributesetinstance_id " +
                    "AND d.C_STORE_BZ=1 " +
                    "AND d.id=f.C_STORE_ID "+
					"AND f.isactive = 'Y' "+
					"AND e.id=b.m_productalias_id " +
                    "AND b.m_productalias_id<>697028 " +
                    "AND c.qty >= b.qty " +
                    "AND c.c_store_id = d.id " +
                    "AND d.isactive = 'Y' " +
                    "AND a.id = ?";

        try {
            List<JSONObject> inventoryDetails = jdbcTemplate.query(sql,
                new Object[]{orderId},
                (rs, rowNum) -> {
                    JSONObject detail = new JSONObject();
                    detail.put("c_store_id", rs.getLong("c_store_id"));      // 门店ID
                    detail.put("store_name", rs.getString("name"));           // 门店名称
                    detail.put("store_qty", rs.getInt("qty"));                // 门店库存数量
                    detail.put("order_item_id", rs.getLong("id"));            // 订单明细ID
                    detail.put("m_productalias_id", rs.getLong("m_productalias_id")); // 条码ID
                    detail.put("barcode_no", rs.getString("no"));            // 条码号
                    return detail;
                });

            JSONArray resultArray = new JSONArray();
            resultArray.addAll(inventoryDetails);

            log.info("成功查询到订单 {} 的 {} 个门店库存满足条件的商品明细", orderId, inventoryDetails.size());
            return resultArray;

        } catch (Exception e) {
            log.error("查询门店库存满足条件的商品明细时发生异常，订单ID: {}", orderId, e);
            return new JSONArray();
        }
    }


    /**
     * 根据商城出库单查询当前有哪些门店库存满足条件的商品明细
     * @param orderId 订单ID
     * @return 包含门店库存满足条件的商品明细数据的JSON数组
     */
    public JSONArray getOsStoreInventoryDetails(Long orderId) {
        String sql = "SELECT d.id as c_store_id, d.name, c.qty-nvl(f2.qty,0) as qty, b.id, b.m_productalias_id,e.no " +
                "                FROM OS_ORDER@bj_70 a, OS_ORDERITEM@bj_70 b, fa_storage@bj_70 c, c_store@bj_70 d,m_product_alias@bj_70 e,C_KCSTORE@bj_70 f ,onlineorderqty@bj_70 f2 " +
                "                WHERE a.id = b.OS_ORDER_id  " +
                "                AND b.m_product_id = c.m_product_id " +
                "                AND b.m_attributesetinstance_id = c.m_attributesetinstance_id " +
                "                AND d.C_STORE_BZ=1 " +
                "                AND d.id=f.C_STORE_ID " +
                "                AND f.isactive = 'Y' " +
                "                and f2.c_store_id(+)=c.c_store_id " +
                "                and f2.m_attributesetinstance_id(+)=c.m_attributesetinstance_id " +
                "                and f2.m_product_id(+)=c.m_product_id " +
                "                AND e.id=b.m_productalias_id " +
                "                AND b.m_productalias_id<>697028 " +
                "                AND c.qty >= b.ord_qty " +
                "                and c.qty >=(b.ord_qty+nvl(f2.qty,0)) " +
                "                AND d.id<>7564 " +
                "                AND c.c_store_id = d.id " +
                "                AND d.isactive = 'Y' " +
                "                AND a.id = ? ";
        try {
            List<JSONObject> inventoryDetails = jdbcTemplate.query(sql,
                    new Object[]{orderId},
                    (rs, rowNum) -> {
                        JSONObject detail = new JSONObject();
                        detail.put("c_store_id", rs.getLong("c_store_id"));      // 门店ID
                        detail.put("store_name", rs.getString("name"));           // 门店名称
                        detail.put("store_qty", rs.getInt("qty"));                // 门店库存数量
                        detail.put("order_item_id", rs.getLong("id"));            // 订单明细ID
                        detail.put("m_productalias_id", rs.getLong("m_productalias_id")); // 条码ID
                        detail.put("barcode_no", rs.getString("no"));            // 条码号
                        return detail;
                    });

            JSONArray resultArray = new JSONArray();
            resultArray.addAll(inventoryDetails);

            log.info("成功查询到订单 {} 的 {} 个门店库存满足条件的商品明细", orderId, inventoryDetails.size());
            return resultArray;

        } catch (Exception e) {
            log.error("查询门店库存满足条件的商品明细时发生异常，订单ID: {}", orderId, e);
            return new JSONArray();
        }
    }
    
    /**
     * 智能订单分配 - 根据库存情况智能分配订单到门店
     * @param orderId 订单ID
     * @return 生成HTML格式的分配结果
     */
    public String smartOrderAllocation(Long orderId) {
        try {
            // 获取订单明细
            JSONArray orderDetails = getOrderDetails(orderId);
            // 获取满足条件的门店库存
            JSONArray storeInventory = getStoreInventoryDetails(orderId);
            
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
                
                // 安全地获取items列表并添加inventory
                List<JSONObject> items = safeCastToListJSONObject(storeMap.get(storeId).get("items"));
                items.add(inventory);
                storeItemMap.get(storeId).add(orderItemId);
            }
            
            // 分配逻辑
            AllocationResult result = performAllocation(orderItemMap, storeMap, storeItemMap);
            
            // 生成HTML结果
            return generateAllocationHtml(result, orderId);
            
        } catch (Exception e) {
            log.error("订单分配时发生异常，订单ID: {}", orderId, e);
            return generateErrorHtml("系统异常: " + e.getMessage(), orderId);
        }
    }
    
    /**
     * 执行分配逻辑（优化版：排除单门店全量满足的门店）
     */
    private AllocationResult performAllocation(Map<Long, JSONObject> orderItemMap, 
                                             Map<Long, Map<String, Object>> storeMap,
                                             Map<Long, Set<Long>> storeItemMap) {
        
        AllocationResult result = new AllocationResult();
        Set<Long> orderItemIds = orderItemMap.keySet();
        int totalItems = orderItemIds.size();
        
        // 策略1: 寻找能满足所有明细的单个门店
        Set<Long> fullSatisfactionStores = new HashSet<>(); // 记录能够全量满足的门店
        for (Map.Entry<Long, Set<Long>> entry : storeItemMap.entrySet()) {
            if (entry.getValue().containsAll(orderItemIds)) {
                StoreAllocation allocation = new StoreAllocation();
                allocation.storeId = entry.getKey();
                allocation.storeName = (String) storeMap.get(entry.getKey()).get("store_name");
                // 安全地获取items列表
                allocation.items = safeCastToListJSONObject(storeMap.get(entry.getKey()).get("items"));
                allocation.itemCount = totalItems;
                result.fullAllocations.add(allocation);
                fullSatisfactionStores.add(entry.getKey()); // 记录全量满足的门店ID
            }
        }
        
        if (!result.fullAllocations.isEmpty()) {
            result.allocationType = 1;
            return result; // 如果有单门店全量满足，直接返回，不再计算后续拆单
        }
        
        // 策略2: 寻找两个门店组合能满足所有明细（排除已能全量满足的门店）
        List<Long> availableStoreIds = storeItemMap.keySet().stream()
            .filter(storeId -> !fullSatisfactionStores.contains(storeId)) // 排除全量满足的门店
            .collect(Collectors.toList());
        
        log.info("策略2：排除 {} 个全量满足门店后，剩余 {} 个门店参与两门店组合计算", 
            fullSatisfactionStores.size(), availableStoreIds.size());
        
        for (int i = 0; i < availableStoreIds.size(); i++) {
            for (int j = i + 1; j < availableStoreIds.size(); j++) {
                Long store1 = availableStoreIds.get(i);
                Long store2 = availableStoreIds.get(j);
                Set<Long> combinedItems = new HashSet<>(storeItemMap.get(store1));
                combinedItems.addAll(storeItemMap.get(store2));
                
                if (combinedItems.containsAll(orderItemIds)) {
                    TwoStoreAllocation allocation = new TwoStoreAllocation();
                    allocation.store1Id = store1;
                    allocation.store1Name = (String) storeMap.get(store1).get("store_name");
                    allocation.store1Items = filterItemsForStore(storeMap.get(store1), store1);
                    allocation.store2Id = store2;
                    allocation.store2Name = (String) storeMap.get(store2).get("store_name");
                    allocation.store2Items = filterItemsForStore(storeMap.get(store2), store2);
                    result.twoStoreAllocations.add(allocation);
                }
            }
        }
        
        if (!result.twoStoreAllocations.isEmpty()) {
            result.allocationType = 2;
            return result;
        }
        
        // 策略3: 寻找三个门店组合能满足所有明细（排除已能全量满足的门店）
        log.info("策略3：排除 {} 个全量满足门店后，剩余 {} 个门店参与三门店组合计算", 
            fullSatisfactionStores.size(), availableStoreIds.size());
        
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
                        result.threeStoreAllocations.add(allocation);
                    }
                }
            }
        }
        
        if (!result.threeStoreAllocations.isEmpty()) {
            result.allocationType = 3;
        } else {
            result.allocationType = 0; // 无法分配
        }
        
        return result;
    }
    
    /**
     * 过滤门店的商品明细
     */
    private List<JSONObject> filterItemsForStore(Map<String, Object> storeInfo, Long storeId) {
        return safeCastToListJSONObject(storeInfo.get("items"));
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

    /**
 * 获取所有门店组合，按门店一进行分组，每个门店一对应多个可选的门店二
 */
public List<StoreGroup> getStoreGroups() {
    // 假设你有一个查询所有组合的方法，比如通过数据库或缓存获取
    List<StoreCombo> allCombos = findAllCombos(); // 你需要实现这个方法

    Map<String, Set<String>> storeGroupMap = new HashMap<>();
    for (StoreCombo combo : allCombos) {
        String store1 = combo.getStore1();
        String store2 = combo.getStore2();
        storeGroupMap.computeIfAbsent(store1, k -> new HashSet<>()).add(store2);
    }

    // 转换为结果列表
    return storeGroupMap.entrySet().stream()
        .map(entry -> new StoreGroup(entry.getKey(), new ArrayList<>(entry.getValue())))
        .collect(Collectors.toList());
}
    
    /**
     * 生成分配结果HTML
     */
    private String generateAllocationHtml(AllocationResult result, Long orderId) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<title>订单分配结果</title>")
            .append("<style>")
            .append("body{font-family:Arial,sans-serif;margin:20px;}")
            .append("table{border-collapse:collapse;width:100%;margin:10px 0;}")
            .append("th,td{border:1px solid #ddd;padding:8px;text-align:left;}")
            .append("th{background-color:#f2f2f2;}")
            .append(".strategy{background-color:#e6f3ff;padding:10px;margin:10px 0;border-radius:5px;}")
            .append(".store{background-color:#f9f9f9;margin:5px 0;padding:10px;border-radius:3px;}")
            .append(".solutions-grid{display:grid;grid-template-columns:repeat(3, 1fr);gap:20px;margin:15px 0;}")
            .append(".solution-card{background:#ffffff;border:1px solid #ddd;border-radius:8px;padding:15px;box-shadow:0 2px 6px rgba(0,0,0,0.1);transition:all 0.3s ease;}")
            .append(".solution-card:hover{transform:translateY(-3px);box-shadow:0 6px 12px rgba(0,0,0,0.15);border-color:#007bff;}")
            .append(".solution-title{font-weight:bold;color:#007bff;margin-bottom:10px;font-size:16px;border-bottom:2px solid #e9ecef;padding-bottom:8px;}")
            .append(".solution-store{color:#333;margin:5px 0;font-weight:500;}")
            .append(".success{color:green;} .warning{color:orange;} .error{color:red;}")
            .append("</style></head><body>")
            .append("<h2>订单分配结果 - 订单ID: ").append(orderId).append("</h2>");
        
        switch (result.allocationType) {
            case 1:
                // html.append("<div class='strategy success'><h3>策略1: 单个门店全量满足</h3></div>")
                //     .append("<div class='solutions-grid'>");
                // // 限制显示前8条
                // int fullDisplayCount = Math.min(8, result.fullAllocations.size());
                // for (int i = 0; i < fullDisplayCount; i++) {
                //     StoreAllocation allocation = result.fullAllocations.get(i);
                //     html.append("<div class='solution-card'>")
                //         .append("<div class='solution-title'>方案 ").append(i + 1).append("</div>")
                //         .append("<div class='solution-store'>").append(allocation.storeName)
                //         .append(" (ID: ").append(allocation.storeId).append(")")
                //         .append("<br>满足所有 ").append(allocation.itemCount).append(" 个明细</div>")
                //         .append(generateSingleStoreItemsHtml(allocation.items))
                //         .append("</div>");
                // }
                // if (fullDisplayCount < result.fullAllocations.size()) {
                //     html.append("<div style='grid-column: 1 / -1; text-align: center; color: #666; padding: 10px;'>")
                //         .append("... 还有 ").append(result.fullAllocations.size() - fullDisplayCount).append(" 个方案（已省略显示）")
                //         .append("</div>");
                // }
                // html.append("</div>");
                break;
            case 2:
                html.append("<div class='strategy warning'><h3>策略2: 两个门店组合满足</h3></div>")
                    .append("<div class='solutions-grid'>");
                // 限制显示前8条
                int twoDisplayCount = Math.min(8, result.twoStoreAllocations.size());
                for (int i = 0; i < twoDisplayCount; i++) {
                    TwoStoreAllocation allocation = result.twoStoreAllocations.get(i);
                    html.append("<div class='solution-card'>")
                        .append("<div class='solution-title'>方案 ").append(i + 1).append("</div>")
                        .append("<div class='solution-store'>门店1: ").append(allocation.store1Name)
                        .append(" (ID: ").append(allocation.store1Id).append(")")
                        .append("<br>门店2: ").append(allocation.store2Name)
                        .append(" (ID: ").append(allocation.store2Id).append(")</div>")
                        .append(generateTwoStoreItemsHtml(allocation.store1Items, allocation.store2Items))
                        .append("</div>");
                }
                if (twoDisplayCount < result.twoStoreAllocations.size()) {
                    html.append("<div style='grid-column: 1 / -1; text-align: center; color: #666; padding: 10px;'>")
                        .append("... 还有 ").append(result.twoStoreAllocations.size() - twoDisplayCount).append(" 个方案（已省略显示）")
                        .append("</div>");
                }
                html.append("</div>");
                break;
            case 3:
                html.append("<div class='strategy warning'><h3>策略3: 三个门店组合满足</h3></div>")
                    .append("<div class='solutions-grid'>");
                // 限制显示前8条
                int threeDisplayCount = Math.min(8, result.threeStoreAllocations.size());
                for (int i = 0; i < threeDisplayCount; i++) {
                    ThreeStoreAllocation allocation = result.threeStoreAllocations.get(i);
                    html.append("<div class='solution-card'>")
                        .append("<div class='solution-title'>方案 ").append(i + 1).append("</div>")
                        .append("<div class='solution-store'>门店1: ").append(allocation.store1Name)
                        .append(" (ID: ").append(allocation.store1Id).append(")")
                        .append("<br>门店2: ").append(allocation.store2Name)
                        .append(" (ID: ").append(allocation.store2Id).append(")")
                        .append("<br>门店3: ").append(allocation.store3Name)
                        .append(" (ID: ").append(allocation.store3Id).append(")</div>")
                        .append(generateThreeStoreItemsHtml(allocation.store1Items, allocation.store2Items, allocation.store3Items))
                        .append("</div>");
                }
                if (threeDisplayCount < result.threeStoreAllocations.size()) {
                    html.append("<div style='grid-column: 1 / -1; text-align: center; color: #666; padding: 10px;'>")
                        .append("... 还有 ").append(result.threeStoreAllocations.size() - threeDisplayCount).append(" 个方案（已省略显示）")
                        .append("</div>");
                }
                html.append("</div>");
                break;
            default:
                html.append("<div class='strategy error'><h3>无法分配</h3>")
                    .append("<p>没有找到合适的门店组合来满足订单需求。</p></div>");
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * 生成两门店分配HTML（包含条码号）
     */
    private String generateTwoStoreAllocationHtml(TwoStoreAllocation allocation) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='store'>")
            .append("<h4>门店组合方案</h4>")
            .append("<h5>门店1: ").append(allocation.store1Name).append(" (ID: ").append(allocation.store1Id).append(")</h5>")
            .append("<table><tr><th>订单明细ID</th><th>条码ID</th><th>条码号</th><th>库存数量</th></tr>");
        
        for (JSONObject item : allocation.store1Items) {
            html.append("<tr>")
                .append("<td>").append(item.getLong("order_item_id")).append("</td>")
                .append("<td>").append(item.getLong("m_productalias_id")).append("</td>")
                .append("<td>").append(item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A").append("</td>")
                .append("<td>").append(item.getInteger("store_qty")).append("</td>")
                .append("</tr>");
        }
        
        html.append("</table>")
            .append("<h5>门店2: ").append(allocation.store2Name).append(" (ID: ").append(allocation.store2Id).append(")</h5>")
            .append("<table><tr><th>订单明细ID</th><th>条码ID</th><th>条码号</th><th>库存数量</th></tr>");
        
        for (JSONObject item : allocation.store2Items) {
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
    
    /**
     * 生成单门店商品明细HTML（优化版）
     */
    private String generateSingleStoreItemsHtml(List<JSONObject> items) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='margin-top:10px; padding:8px; background:#fafafa; border-radius:4px;'>")
            .append("<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(200px,1fr)); gap:8px;'>");
        
        for (JSONObject item : items) {
            String barcodeNo = item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A";
            Integer storeQty = item.getInteger("store_qty");
            
            html.append("<div style='background:white; padding:8px; border-radius:4px; border-left:3px solid #4CAF50; box-shadow:0 1px 3px rgba(0,0,0,0.1);'>")
                .append("<div style='font-size:12px; color:#333; margin-bottom:4px;'>")
                .append("📦 条码: <span style='font-weight:bold; color:#2196F3;'>").append(barcodeNo).append("</span>")
                .append("</div>")
                .append("<div style='font-size:13px; font-weight:bold;'>")
                .append("📊 库存: <span style='color:#FF5722; background:#fff3e0; padding:2px 6px; border-radius:3px; font-size:14px;'>").append(storeQty).append("</span>")
                .append("</div>")
                .append("</div>");
        }
        
        html.append("</div></div>");
        return html.toString();
    }
    
    /**
     * 生成两门店商品明细HTML（优化版）
     */
    private String generateTwoStoreItemsHtml(List<JSONObject> store1Items, List<JSONObject> store2Items) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='margin-top:10px; padding:8px; background:#fafafa; border-radius:4px;'>");
        
        // 门店1明细
        html.append("<div style='margin-bottom:8px;'>")
            .append("<div style='font-weight:bold;color:#007bff;margin-bottom:4px;font-size:12px;'>🏢 门店1:</div>")
            .append("<div style='display:flex; flex-wrap:wrap; gap:6px;'>");
        for (JSONObject item : store1Items) {
            String barcodeNo = item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A";
            Integer storeQty = item.getInteger("store_qty");
            html.append("<div style='background:white; padding:4px 8px; border-radius:3px; border-left:2px solid #007bff; font-size:11px;'>")
                .append("📦 ").append(barcodeNo)
                .append(" <span style='color:#FF5722; font-weight:bold; background:#fff3e0; padding:1px 4px; border-radius:2px;'>[").append(storeQty).append("]</span>")
                .append("</div>");
        }
        html.append("</div></div>");
        
        // 门店2明细
        html.append("<div>")
            .append("<div style='font-weight:bold;color:#007bff;margin-bottom:4px;font-size:12px;'>🏢 门店2:</div>")
            .append("<div style='display:flex; flex-wrap:wrap; gap:6px;'>");
        for (JSONObject item : store2Items) {
            String barcodeNo = item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A";
            Integer storeQty = item.getInteger("store_qty");
            html.append("<div style='background:white; padding:4px 8px; border-radius:3px; border-left:2px solid #007bff; font-size:11px;'>")
                .append("📦 ").append(barcodeNo)
                .append(" <span style='color:#FF5722; font-weight:bold; background:#fff3e0; padding:1px 4px; border-radius:2px;'>[").append(storeQty).append("]</span>")
                .append("</div>");
        }
        html.append("</div></div>");
        
        html.append("</div>");
        return html.toString();
    }
    
    /**
     * 生成三门店商品明细HTML（优化版）
     */
    private String generateThreeStoreItemsHtml(List<JSONObject> store1Items, List<JSONObject> store2Items, List<JSONObject> store3Items) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='margin-top:10px; padding:8px; background:#fafafa; border-radius:4px;'>");
        
        // 门店1明细
        html.append("<div style='margin-bottom:6px;'>")
            .append("<div style='font-weight:bold;color:#007bff;margin-bottom:3px;font-size:11px;'>🏢 门店1:</div>")
            .append("<div style='display:flex; flex-wrap:wrap; gap:4px;'>");
        for (JSONObject item : store1Items) {
            String barcodeNo = item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A";
            Integer storeQty = item.getInteger("store_qty");
            html.append("<div style='background:white; padding:2px 6px; border-radius:2px; border-left:2px solid #007bff; font-size:10px;'>")
                .append("📦 ").append(barcodeNo)
                .append(" <span style='color:#FF5722; font-weight:bold; background:#fff3e0; padding:1px 3px; border-radius:2px;'>[").append(storeQty).append("]</span>")
                .append("</div>");
        }
        html.append("</div></div>");
        
        // 门店2明细
        html.append("<div style='margin-bottom:6px;'>")
            .append("<div style='font-weight:bold;color:#007bff;margin-bottom:3px;font-size:11px;'>🏢 门店2:</div>")
            .append("<div style='display:flex; flex-wrap:wrap; gap:4px;'>");
        for (JSONObject item : store2Items) {
            String barcodeNo = item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A";
            Integer storeQty = item.getInteger("store_qty");
            html.append("<div style='background:white; padding:2px 6px; border-radius:2px; border-left:2px solid #007bff; font-size:10px;'>")
                .append("📦 ").append(barcodeNo)
                .append(" <span style='color:#FF5722; font-weight:bold; background:#fff3e0; padding:1px 3px; border-radius:2px;'>[").append(storeQty).append("]</span>")
                .append("</div>");
        }
        html.append("</div></div>");
        
        // 门店3明细
        html.append("<div>")
            .append("<div style='font-weight:bold;color:#007bff;margin-bottom:3px;font-size:11px;'>🏢 门店3:</div>")
            .append("<div style='display:flex; flex-wrap:wrap; gap:4px;'>");
        for (JSONObject item : store3Items) {
            String barcodeNo = item.getString("barcode_no") != null ? item.getString("barcode_no") : "N/A";
            Integer storeQty = item.getInteger("store_qty");
            html.append("<div style='background:white; padding:2px 6px; border-radius:2px; border-left:2px solid #007bff; font-size:10px;'>")
                .append("📦 ").append(barcodeNo)
                .append(" <span style='color:#FF5722; font-weight:bold; background:#fff3e0; padding:1px 3px; border-radius:2px;'>[").append(storeQty).append("]</span>")
                .append("</div>");
        }
        html.append("</div></div>");
        
        html.append("</div>");
        return html.toString();
    }
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
    }
    
    private static class TwoStoreAllocation {
        Long store1Id;
        String store1Name;
        List<JSONObject> store1Items;
        Long store2Id;
        String store2Name;
        List<JSONObject> store2Items;
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
    }

    /**
     * 获取所有门店组合
     * @return 所有门店组合列表
     */
    public List<StoreCombo> findAllCombos() {
        // 这里应该实现获取所有门店组合的逻辑
        // 由于当前类主要是处理订单分配逻辑，这里返回一个空列表
        // 实际实现应该根据业务需求来定义
        log.info("获取所有门店组合");
        return new ArrayList<>();
    }

    // 测试方法 - 添加更详细的调试信息
    public static void main(String[] args) {
        try {
            TbqtyServices service = new TbqtyServices();

//            SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
//            dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
//            dataSource.setUrl("jdbc:oracle:thin:@10.100.21.181:1521/orcl");
//            dataSource.setUsername("neands3");
//            dataSource.setPassword("abc123");
//
//            // 创建JdbcTemplate实例
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            // 测试数据库连接
//            try {
//                String testSql = "SELECT 1 FROM DUAL";
//                Integer result = jdbcTemplate.queryForObject(testSql, Integer.class);
//                log.info("数据库连接测试成功: {}", result);
//            } catch (Exception e) {
//                log.error("数据库连接测试失败", e);
//                return;
//            }

            // 执行完整流程
           // service.completeStoreFetchProcess();
        } catch (Exception e) {
            log.error("测试失败", e);
            e.printStackTrace();
        }
    }
}