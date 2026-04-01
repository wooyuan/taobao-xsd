package com.taobao.logistics.integration.taobao.controller;

import com.taobao.logistics.integration.taobao.service.ProductImgUrlSyncJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 产品图片URL控制器
 * 提供手动触发图片URL同步任务的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/product-img")
public class ProductImgUrlController {

    @Autowired
    private ProductImgUrlSyncJobService productImgUrlSyncJobService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 手动触发产品图片URL同步任务
     * 访问地址: http://服务器地址:端口/api/product-img/sync
     * 
     * @return 执行结果及详细统计信息
     */
    @GetMapping("/sync")
    public String syncProductImgUrl() {
        log.info("收到手动触发产品图片URL同步任务请求");
        
        try {
            // 调用返回详细结果的同步方法
            ProductImgUrlSyncJobService.SyncResult result = productImgUrlSyncJobService.syncProductImgUrlWithResult();
            log.info("手动触发产品图片URL同步任务完成");
            
            // 构建包含详细统计信息的返回结果
            return String.format("同步任务执行完成！\n" +
                                "总计检查商品数量：%d\n" +
                                "有图片商品数量：%d\n" +
                                "无图片商品数量：%d\n" +
                                "本次新增同步记录数：%d",
                                result.getTotalRecords(),
                                result.getTotalYUpdated(),
                                result.getTotalNUpdated(),
                                result.getNewlySyncedRecords());
        } catch (Exception e) {
            log.error("手动触发产品图片URL同步任务失败", e);
            return "执行失败: " + e.getMessage();
        }
    }
    
    /**
     * 检查服务是否正常运行
     * 访问地址: http://服务器地址:端口/api/product-img/health
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public String healthCheck() {
        return "Product Image URL Service is running.";
    }
    
    /**
     * 测试单个记录的URL检查和更新
     * 访问地址: http://服务器地址:端口/api/product-img/test-update?id=668884
     * 
     * @param id 产品ID
     * @return 测试结果
     */
    @GetMapping("/test-update")
    public Map<String, Object> testUpdate(@RequestParam Long id) {
        Map<String, Object> result = new HashMap<>();
        log.info("测试单条记录更新，ID: {}", id);
        
        try {
            // 1. 查询当前记录
            String querySql = "SELECT id, imageurl, isok, modifieddate FROM product_imgurl_isok@bj_70 WHERE id = ?";
            Map<String, Object> record = jdbcTemplate.queryForMap(querySql, id);
            result.put("原记录", record);
            
            String imageUrl = (String) record.get("imageurl");
            String oldStatus = (String) record.get("isok");
            
            // 2. 检查URL是否存在
            boolean urlExists = productImgUrlSyncJobService.isUrlExists(imageUrl);
            result.put("URL", imageUrl);
            result.put("URL检查结果", urlExists ? "存在" : "不存在");
            
            // 3. 根据业务逻辑设置状态（图片存在设为N，不存在设为Y）
            String newStatus = urlExists ? "N" : "Y";
            result.put("目标状态", newStatus);
            
            // 4. 执行更新
            if (oldStatus.equals(newStatus)) {
                result.put("更新操作", "跳过（状态相同）");
                result.put("更新结果", "无需更新");
            } else {
                String updateSql = "UPDATE product_imgurl_isok@bj_70 SET isok = ?, modifieddate = sysdate WHERE id = ?";
                int updated = jdbcTemplate.update(updateSql, newStatus, id);
                result.put("更新操作", "执行更新");
                result.put("受影响行数", updated);
                result.put("更新结果", updated > 0 ? "成功" : "失败");
                
                // 5. 查询更新后的记录
                if (updated > 0) {
                    Map<String, Object> newRecord = jdbcTemplate.queryForMap(querySql, id);
                    result.put("新记录", newRecord);
                }
            }
            
            result.put("状态", "成功");
        } catch (Exception e) {
            log.error("测试失败，ID: {}", id, e);
            result.put("状态", "失败");
            result.put("错误信息", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据指定日期执行ON_QDRANT_PRODUCT表的插入操作（基于m_dim12_id）
     * 访问地址: http://服务器地址:端口/api/product-img/insert-onqdrant?date=20260303
     * 
     * @param date 日期字符串，格式：YYYYMMDD
     * @return 执行结果
     */
    @GetMapping("/insert-onqdrant")
    public String insertOnQdrantProduct(@RequestParam String date) {
        log.info("收到执行ON_QDRANT_PRODUCT表插入操作请求（基于m_dim12_id），日期：{}", date);
        
        try {
            // 调用插入方法
            int insertedRows = productImgUrlSyncJobService.insertOnQdrantProductByDate(date);
            log.info("执行ON_QDRANT_PRODUCT表插入操作完成（基于m_dim12_id）");
            
            // 构建返回结果
            return String.format("插入操作执行完成！\n" +
                                "日期：%s\n" +
                                "新增记录数：%d\n" +
                                "基于：m_dim12_id",
                                date, insertedRows);
        } catch (Exception e) {
            log.error("执行ON_QDRANT_PRODUCT表插入操作失败（基于m_dim12_id），日期：{}", date, e);
            return "执行失败: " + e.getMessage();
        }
    }
    
    /**
     * 根据指定日期执行ON_QDRANT_PRODUCT表的插入操作（基于m_dim15_id）
     * 访问地址: http://服务器地址:端口/api/product-img/insert-onqdrant-dim15?date=20260303
     * 
     * @param date 日期字符串，格式：YYYYMMDD
     * @return 执行结果
     */
    @GetMapping("/insert-onqdrant-dim15")
    public String insertOnQdrantProductDim15(@RequestParam String date) {
        log.info("收到执行ON_QDRANT_PRODUCT表插入操作请求（基于m_dim15_id），日期：{}", date);
        
        try {
            // 调用插入方法
            int insertedRows = productImgUrlSyncJobService.insertOnQdrantProductByDateDim15(date);
            log.info("执行ON_QDRANT_PRODUCT表插入操作完成（基于m_dim15_id）");
            
            // 构建返回结果
            return String.format("插入操作执行完成！\n" +
                                "日期：%s\n" +
                                "新增记录数：%d\n" +
                                "基于：m_dim15_id",
                                date, insertedRows);
        } catch (Exception e) {
            log.error("执行ON_QDRANT_PRODUCT表插入操作失败（基于m_dim15_id），日期：{}", date, e);
            return "执行失败: " + e.getMessage();
        }
    }
}