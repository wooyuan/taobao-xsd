package com.taobao.logistics.integration.taobao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 产品图片URL同步定时任务服务
 * 每天凌晨1点执行，将m_product表中的数据同步到PRODUCT_IMGURL_ISOK表
 */
@Slf4j
@Service
public class ProductImgUrlSyncJobService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 线程池配置
    private final ExecutorService executorService;
    
    // 批处理大小
    private final int batchSize;
    
    // 并发数量
    private final int concurrencyLevel;
    
    // 批量更新大小
    private final int updateBatchSize;
    
    // URL检查超时时间(毫秒)
    private final int urlCheckTimeout;
    
    // 构造函数初始化线程池和配置参数
    public ProductImgUrlSyncJobService() {
        // 可以通过配置文件注入这些参数
        this.batchSize = 1000;  // 每批查询数量
        this.concurrencyLevel = 20; // 并发检查数量，可根据实际网络情况调整
        this.updateBatchSize = 100; // 批量更新大小
        this.urlCheckTimeout = 3000; // URL检查超时时间
        
        // 初始化线程池
        this.executorService = new ThreadPoolExecutor(
                concurrencyLevel, 
                concurrencyLevel, 
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时由调用者线程执行，避免任务丢失
        );
    }

    /**
     * 同步任务结果统计类
     */
    public static class SyncResult {
        private int totalRecords;       // 总检查数量
        private int totalYUpdated;      // 有图片数量（URL存在更新为Y的数量）
        private int totalNUpdated;      // 无图片数量（URL不存在更新为N的数量）
        private int newlySyncedRecords; // 新增同步记录数
        
        public SyncResult(int totalRecords, int totalYUpdated, int totalNUpdated, int newlySyncedRecords) {
            this.totalRecords = totalRecords;
            this.totalYUpdated = totalYUpdated;
            this.totalNUpdated = totalNUpdated;
            this.newlySyncedRecords = newlySyncedRecords;
        }
        
        public int getTotalRecords() {
            return totalRecords;
        }
        
        public int getTotalYUpdated() {
            return totalYUpdated;
        }
        
        public int getTotalNUpdated() {
            return totalNUpdated;
        }
        
        public int getNewlySyncedRecords() {
            return newlySyncedRecords;
        }
    }
    
    /**
     * 定时任务：每天凌晨1点执行产品图片URL同步和检查
     * cron表达式：秒 分 时 日 月 周
     * 0 0 0 * * ? 表示每天凌晨0点执行
     */
     @Scheduled(cron = "0 0 0 * * ?")
    public void syncProductImgUrl() {
        // 调用返回结果的方法，但不使用返回值（用于定时任务）
        try {
            syncProductImgUrlWithResult();
        } catch (Exception e) {
            log.error("产品图片URL同步任务执行失败", e);
        }
    }
    
    /**
     * 执行产品图片URL同步任务并返回详细结果
     * @return 同步结果统计信息
     */
    public SyncResult syncProductImgUrlWithResult() {
        log.info("开始执行产品图片URL同步任务");
        
        try {
            // 0. 删除不在m_product表中的记录（清理无效数据）
            String deleteSql = "DELETE FROM PRODUCT_IMGURL_ISOK@bj_70 a " +
                              "WHERE NOT EXISTS (SELECT 1 " +
                              "                FROM m_product@bj_70 b " +
                              "                WHERE b.isactive = 'Y' " +
                              "                AND a.m_product_id = b.id)";
            
            int deletedRows = jdbcTemplate.update(deleteSql);
            log.info("清理无效数据完成，删除记录数：{}", deletedRows);
            
            // 1. 同步新产品数据到PRODUCT_IMGURL_ISOK表
            String syncSql = "INSERT INTO PRODUCT_IMGURL_ISOK@bj_70 " +
                         "(ID, AD_CLIENT_ID, AD_ORG_ID, M_PRODUCT_ID, name, IMAGEURL, ISOK, OWNERID, MODIFIERID, CREATIONDATE, MODIFIEDDATE, ISACTIVE, year) " +
                         "SELECT t.ID, " +
                         "       t.AD_CLIENT_ID, " +
                         "       t.AD_ORG_ID, " +
                         "       t.ID, " +
                         "       t.NAME, " +
                         "       t.IMAGEURL, " +
                         "       'A', " +
                         "       t.OWNERID, " +
                         "       t.MODIFIERID, " +
                         "       t.CREATIONDATE, " +
                         "       t.MODIFIEDDATE, " +
                         "       t.ISACTIVE, " +
                         "       t.YEAR " +
                         "FROM m_product@bj_70 t " +
                         "WHERE t.isactive = 'Y'  and t.imageurl is not null " +
                         "AND NOT EXISTS (SELECT 1 FROM PRODUCT_IMGURL_ISOK@bj_70 b WHERE b.id = t.id)";
            
            int affectedRows = jdbcTemplate.update(syncSql);
            log.info("产品图片URL同步任务执行完成，新增记录数：{}", affectedRows);
            
            // 2. 检查图片URL是否存在并更新状态，获取统计结果
            int[] stats = checkAndUpdateImgUrlStatus();
            int totalRecords = stats[0];
            int totalYUpdated = stats[1];
            int totalNUpdated = stats[2];
            
            // 3. 插入数据到ON_QDRANT_PRODUCT表（基于m_dim12_id）- 暂时取消
            /*
            String insertSql = "INSERT INTO ON_QDRANT_PRODUCT ( " +
                              "     id, creationdate, modifieddate,has_train, " +
                              "     m_product_id, sku_name, img_url, rtype, code " +
                              " ) " +
                              " SELECT " +
                              "     get_sequences('ON_QDRANT_PRODUCT'), " +
                              "     a.modifieddate, " +
                              "     a.modifieddate, " +
                              "     'N', " +
                              "     b.id, " +
                              "     d.no, " +
                              "     a.imageurl,    " +
                              "     'Y', " +
                              "     c.attribname   " +
                              " FROM " +
                              "     product_imgurl_isok@bj_70 a " +
                              "     JOIN m_product@bj_70 b ON a.m_product_id = b.id " +
                              "     JOIN m_dim@bj_70 c ON b.m_dim12_id = c.id " +
                              "     JOIN m_product_alias@bj_70 d ON b.id = d.m_product_id " +
                              " WHERE " +
                              "     b.isactive = 'Y' " +
                              "     AND b.m_dim12_id IS NOT NULL " +
                              "     AND a.isok = 'N' " +
                              "     AND to_number(to_char(a.modifieddate,'YYYYMMDD')) = to_number(to_char(SYSDATE,'YYYYMMDD')) " +
                              "     AND NOT EXISTS ( " +
                              "         SELECT 1 " +
                              "         FROM ON_QDRANT_PRODUCT f " +
                              "         WHERE f.sku_name = d.no " +
                              "     )";
            
            int insertedRows = jdbcTemplate.update(insertSql);
            log.info("插入ON_QDRANT_PRODUCT表完成（基于m_dim12_id），新增记录数：{}", insertedRows);
            */
            
            // 4. 插入数据到ON_QDRANT_PRODUCT表（基于m_dim15_id）- 暂时取消
            /*
            String insertSqlDim15 = "INSERT INTO ON_QDRANT_PRODUCT ( " +
                                   "     id, creationdate, modifieddate,has_train, " +
                                   "     m_product_id, sku_name, img_url, rtype, code " +
                                   " ) " +
                                   " SELECT " +
                                   "     get_sequences('ON_QDRANT_PRODUCT'), " +
                                   "     a.modifieddate, " +
                                   "     a.modifieddate, " +
                                   "     'N', " +
                                   "     b.id, " +
                                   "     d.no, " +
                                   "     a.imageurl,    " +
                                   "     'Y', " +
                                   "     c.attribname   " +
                                   " FROM " +
                                   "     product_imgurl_isok@bj_70 a " +
                                   "     JOIN m_product@bj_70 b ON a.m_product_id = b.id " +
                                   "     JOIN m_dim@bj_70 c ON b.m_dim15_id = c.id " +
                                   "     JOIN m_product_alias@bj_70 d ON b.id = d.m_product_id " +
                                   " WHERE " +
                                   "     b.isactive = 'Y' " +
                                   "     AND b.m_dim15_id IS NOT NULL " +
                                   "     AND a.isok = 'N' " +
                                   "     AND to_number(to_char(a.modifieddate,'YYYYMMDD')) = to_number(to_char(SYSDATE,'YYYYMMDD')) " +
                                   "     AND NOT EXISTS ( " +
                                   "         SELECT 1 " +
                                   "         FROM ON_QDRANT_PRODUCT f " +
                                   "         WHERE f.sku_name = d.no " +
                                   "     )";
            
            int insertedRowsDim15 = jdbcTemplate.update(insertSqlDim15);
            log.info("插入ON_QDRANT_PRODUCT表完成（基于m_dim15_id），新增记录数：{}", insertedRowsDim15);
            */
            
            // 返回详细统计结果
            return new SyncResult(totalRecords, totalYUpdated, totalNUpdated, affectedRows);
            
        } catch (Exception e) {
            log.error("产品图片URL同步任务执行失败", e);
            throw e;
        }
    }
    
    /**
     * 检查图片URL是否存在并更新状态（优化版本）
     * @return 统计结果数组 [总检查数量, URL存在更新为Y的数量, URL不存在更新为N的数量]
     */
    private int[] checkAndUpdateImgUrlStatus() {
        log.info("开始检查产品图片URL是否存在（优化版本）");
        
        try {
            int totalRecords = 0;
            int totalYUpdated = 0; // URL存在更新为Y的数量
            int totalNUpdated = 0; // URL不存在更新为N的数量
            int batchNum = 0;
            
            // 先获取总记录数，确保分页逻辑正确
            int totalCount = 0;
            try {
                String countSql = "SELECT COUNT(*) " +
                                 "FROM product_imgurl_isok@bj_70 a " +
                                 "WHERE a.imageurl is not null  and ((a.isok in ('Y') and to_number(to_char(a.creationdate, 'YYYYMMDD')) >= 20240101 )  or a.isok in ( 'A') )  " +
                                 "AND to_number(to_char(a.modifieddate,'YYYYMMDD'))<to_number(to_char(sysdate,'YYYYMMDD'))";
                totalCount = jdbcTemplate.queryForObject(countSql, Integer.class);
                log.info("总记录数: {}", totalCount);
            } catch (Exception e) {
                log.error("获取总记录数失败", e);
            }
            
            // 分批查询和处理，避免一次性加载大量数据
            // 使用基于ID的分页方式，避免ROWNUM分页的问题
            Long lastId = 0L;
            while (true) {
                batchNum++;
                
                // 分批查询需要检查的图片URL
                // 使用基于ID的分页，确保不会遗漏数据
                // Oracle 11g兼容的分页语法
                String querySql = "SELECT * FROM (" +
                                  "    SELECT a.id, a.imageurl, ROWNUM AS rn " +
                                  "    FROM product_imgurl_isok@bj_70 a " +
                                  "    WHERE a.imageurl is not null " +
                                  "    AND  ((a.isok in ('Y') and to_number(to_char(a.creationdate, 'YYYYMMDD')) >= 20240101 )  or a.isok in ( 'A') )  " +
                                  "    AND to_number(to_char(a.modifieddate,'YYYYMMDD'))<to_number(to_char(sysdate,'YYYYMMDD')) " +
                                  "    AND a.id > ? " +
                                  "    ORDER BY a.id " +
                                  ") WHERE ROWNUM <= ?";

                log.info("执行查询，批次：{}, 起始ID：{}, 批次大小：{}", batchNum, lastId, batchSize);
                
                List<ImgUrlRecord> records = jdbcTemplate.query(querySql, new Object[]{lastId, batchSize}, new RowMapper<ImgUrlRecord>() {
                    @Override
                    public ImgUrlRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
                        ImgUrlRecord record = new ImgUrlRecord();
                        record.setId(rs.getLong("id"));
                        record.setImageUrl(rs.getString("imageurl"));
                        return record;
                    }
                });
                
                log.info("查询到记录数: {}", records.size());
                
                // 如果没有更多记录，退出循环
                if (records.isEmpty()) {
                    log.info("没有更多记录，循环结束。共处理 {} 批次", batchNum - 1);
                    break;
                }
                
                // 更新lastId为当前批次最后一条记录的ID
                lastId = records.get(records.size() - 1).getId();
                log.info("本批次最后一条记录ID: {}, 下批次起始ID: {}", lastId, lastId);
                
                int batchRecords = records.size();
                totalRecords += batchRecords;
                log.info("正在处理第 {} 批数据，本批数量：{}, 累计数量：{}", batchNum, batchRecords, totalRecords);
                
                // 过滤出有效的URL记录
                List<ImgUrlRecord> validRecords = records.stream()
                        .filter(record -> record.getImageUrl() != null && !record.getImageUrl().trim().isEmpty())
                        .collect(Collectors.toList());
                
                // 记录批次中的关键ID
                if (!records.isEmpty()) {
                    long minId = records.stream().mapToLong(ImgUrlRecord::getId).min().orElse(0);
                    long maxId = records.stream().mapToLong(ImgUrlRecord::getId).max().orElse(0);
                    log.info("本批次ID范围：{} - {}, 包含648798: {}", minId, maxId, records.stream().anyMatch(r -> r.getId() == 648798));
                }
                
                // 并发检查URL
                List<Future<ImgUrlCheckResult>> futures = new ArrayList<>();
                for (ImgUrlRecord record : validRecords) {
                    log.info("提交URL检查任务 - ID: {}", record.getId());
                    futures.add(executorService.submit(() -> checkUrlAsync(record)));
                }
                
                // 收集检查结果并批量更新
                List<StatusUpdate> updates = new ArrayList<>(updateBatchSize);
                for (Future<ImgUrlCheckResult> future : futures) {
                    try {
                        ImgUrlCheckResult result = future.get(urlCheckTimeout + 1000, TimeUnit.MILLISECONDS); // 等待结果，超时略长于URL检查
                        if (result != null) {
                            // 根据检查结果设置状态：存在设为N，不存在设为Y
                            String status = result.isExists() ? "N" : "Y";
                            updates.add(new StatusUpdate(result.getId(), status));
                            
                            // 达到批量更新大小时执行更新
                            if (updates.size() >= updateBatchSize) {
                                int[] counts = batchUpdateStatusWithType(updates);
                                totalYUpdated += counts[0];
                                totalNUpdated += counts[1];
                                updates.clear();
                            }
                        }
                    } catch (Exception e) {
                        log.error("获取URL检查结果失败", e);
                    }
                }
                
                // 处理剩余的更新
                if (!updates.isEmpty()) {
                    int[] counts = batchUpdateStatusWithType(updates);
                    totalYUpdated += counts[0];
                    totalNUpdated += counts[1];
                }
                
                // 每批次处理完成后短暂休息，避免持续高负载
                Thread.sleep(500);
            }
            
            log.info("图片URL检查完成，总检查数量：{}, URL存在更新为N的数量：{}, URL不存在更新为Y的数量：{}", 
                    totalRecords, totalNUpdated, totalYUpdated);
            log.info("总记录数对比：数据库总记录数={}, 实际处理记录数={}", totalCount, totalRecords);
            if (totalCount > 0 && totalRecords < totalCount) {
                log.warn("警告：处理记录数小于数据库总记录数，可能存在数据丢失");
            }
            
            // 返回统计结果数组
            return new int[]{totalRecords, totalYUpdated, totalNUpdated};
            
        } catch (Exception e) {
            log.error("检查图片URL状态失败", e);
            // 发生异常时返回空统计
            return new int[]{0, 0, 0};
        }
    }
    
    /**
     * 异步检查URL
     */
    private ImgUrlCheckResult checkUrlAsync(ImgUrlRecord record) {
        try {
            boolean exists = isUrlExists(record.getImageUrl());
            log.info("URL检查结果 - ID: {}, URL: {}, exists: {}", record.getId(), record.getImageUrl(), exists);
            return new ImgUrlCheckResult(record.getId(), exists);
        } catch (Exception e) {
            log.error("检查图片URL失败，ID: {}, URL: {}", record.getId(), record.getImageUrl(), e);
            return new ImgUrlCheckResult(record.getId(), false);
        }
    }
    
    /**
     * 批量更新URL状态
     */
    private int batchUpdateStatus(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        
        try {
            // 记录方法执行日志
            log.info("开始执行批量更新URL状态方法，待更新记录数量：{}", ids.size());
            // 使用批量更新语句
            String updateSql = "UPDATE product_imgurl_isok@bj_70 SET isok = 'Y' ,modifieddate=sysdate WHERE id = ?";
            
            int[] updatedRows = jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setLong(1, ids.get(i));
                }
                
                @Override
                public int getBatchSize() {
                    return ids.size();
                }
            });
            
            // 统计成功更新的数量
            int successCount = 0;
            for (int count : updatedRows) {
                if (count > 0) {
                    successCount++;
                }
            }
            
            log.debug("批量更新完成，更新数量：{}", successCount);
            return successCount;
        } catch (Exception e) {
            log.error("批量更新URL状态失败", e);
            return 0;
        }
    }
    
    /**
     * 批量更新URL状态（支持Y和N两种状态）
     * @param updates 更新记录列表
     * @return int数组，[0]是更新为Y的数量，[1]是更新为N的数量
     */
    private int[] batchUpdateStatusWithType(List<StatusUpdate> updates) {
        if (updates.isEmpty()) {
            return new int[]{0, 0};
        }
        
        try {
            // 记录方法执行日志
            log.info("开始执行批量更新URL状态方法（支持Y/N状态），待更新记录数量：{}", updates.size());
            // 分别统计Y和N的更新数量
            int yCount = 0;
            int nCount = 0;
            
            // 直接执行单条更新，因为每条记录可能有不同的状态值
            // 在实际应用中，可以考虑按状态分组后分别进行批量更新
            for (StatusUpdate update : updates) {
                String updateSql = "UPDATE product_imgurl_isok@bj_70 SET isok = ? ,modifieddate=sysdate WHERE id = ?";
                log.info("执行更新 - ID: {}, 当前状态: {}, 目标状态: {}", update.getId(), "待查询", update.getStatus());
                int updated = jdbcTemplate.update(updateSql, update.getStatus(), update.getId());
                log.info("更新结果 - ID: {}, 受影响行数: {}", update.getId(), updated);
                if (updated > 0) {
                    if ("Y".equals(update.getStatus())) {
                        yCount++;
                    } else if ("N".equals(update.getStatus())) {
                        nCount++;
                    }
                }
            }
            
            log.debug("批量更新完成，更新为Y的数量：{}, 更新为N的数量：{}", yCount, nCount);
            return new int[]{yCount, nCount};
        } catch (Exception e) {
            log.error("批量更新URL状态（带类型）失败", e);
            return new int[]{0, 0};
        }
    }
    
    /**
     * 根据指定日期执行ON_QDRANT_PRODUCT表的插入操作（基于m_dim12_id）
     * @param dateStr 日期字符串，格式：YYYYMMDD
     * @return 插入的记录数
     */
    public int insertOnQdrantProductByDate(String dateStr) {
        log.info("开始执行ON_QDRANT_PRODUCT表插入操作（基于m_dim12_id），日期：{}", dateStr);
        
        try {
            String insertSql = "INSERT INTO ON_QDRANT_PRODUCT ( " +
                              "     id, creationdate, modifieddate,has_train, " +
                              "     m_product_id, sku_name, img_url, rtype, code " +
                              " ) " +
                              " SELECT " +
                              "     get_sequences('ON_QDRANT_PRODUCT'), " +
                              "     a.modifieddate, " +
                              "     a.modifieddate, " +
                              "     'N', " +
                              "     b.id, " +
                              "     d.no, " +
                              "     a.imageurl,    " +
                              "     'Y', " +
                              "     c.attribname   " +
                              " FROM " +
                              "     product_imgurl_isok@bj_70 a " +
                              "     JOIN m_product@bj_70 b ON a.m_product_id = b.id " +
                              "     JOIN m_dim@bj_70 c ON b.m_dim12_id = c.id " +
                              "     JOIN m_product_alias@bj_70 d ON b.id = d.m_product_id " +
                              " WHERE " +
                              "     b.isactive = 'Y' " +
                              "     AND b.m_dim12_id IS NOT NULL " +
                              "     AND a.isok = 'N' " +
                              "     AND to_number(to_char(a.modifieddate,'YYYYMMDD')) = ? " +
                              "     AND NOT EXISTS ( " +
                              "         SELECT 1 " +
                              "         FROM ON_QDRANT_PRODUCT f " +
                              "         WHERE f.sku_name = d.no " +
                              "     )";
            
            int insertedRows = jdbcTemplate.update(insertSql, dateStr);
            log.info("插入ON_QDRANT_PRODUCT表完成（基于m_dim12_id），日期：{}，新增记录数：{}", dateStr, insertedRows);
            return insertedRows;
        } catch (Exception e) {
            log.error("执行ON_QDRANT_PRODUCT表插入操作失败（基于m_dim12_id），日期：{}", dateStr, e);
            throw e;
        }
    }
    
    /**
     * 根据指定日期执行ON_QDRANT_PRODUCT表的插入操作（基于m_dim15_id）
     * @param dateStr 日期字符串，格式：YYYYMMDD
     * @return 插入的记录数
     */
    public int insertOnQdrantProductByDateDim15(String dateStr) {
        log.info("开始执行ON_QDRANT_PRODUCT表插入操作（基于m_dim15_id），日期：{}", dateStr);
        
        try {
            String insertSql = "INSERT INTO ON_QDRANT_PRODUCT ( " +
                              "     id, creationdate, modifieddate,has_train, " +
                              "     m_product_id, sku_name, img_url, rtype, code " +
                              " ) " +
                              " SELECT " +
                              "     get_sequences('ON_QDRANT_PRODUCT'), " +
                              "     a.modifieddate, " +
                              "     a.modifieddate, " +
                              "     'N', " +
                              "     b.id, " +
                              "     d.no, " +
                              "     a.imageurl,    " +
                              "     'Y', " +
                              "     c.attribname   " +
                              " FROM " +
                              "     product_imgurl_isok@bj_70 a " +
                              "     JOIN m_product@bj_70 b ON a.m_product_id = b.id " +
                              "     JOIN m_dim@bj_70 c ON b.m_dim15_id = c.id " +
                              "     JOIN m_product_alias@bj_70 d ON b.id = d.m_product_id " +
                              " WHERE " +
                              "     b.isactive = 'Y' " +
                              "     AND b.m_dim15_id IS NOT NULL " +
                              "     AND a.isok = 'N' " +
                              "     AND to_number(to_char(a.modifieddate,'YYYYMMDD')) = ? " +
                              "     AND NOT EXISTS ( " +
                              "         SELECT 1 " +
                              "         FROM ON_QDRANT_PRODUCT f " +
                              "         WHERE f.sku_name = d.no " +
                              "     )";
            
            int insertedRows = jdbcTemplate.update(insertSql, dateStr);
            log.info("插入ON_QDRANT_PRODUCT表完成（基于m_dim15_id），日期：{}，新增记录数：{}", dateStr, insertedRows);
            return insertedRows;
        } catch (Exception e) {
            log.error("执行ON_QDRANT_PRODUCT表插入操作失败（基于m_dim15_id），日期：{}", dateStr, e);
            throw e;
        }
    }
    
    /**
     * 检查URL是否存在（优化版本）- 公开给Controller调用
     * @param urlStr URL字符串
     * @return 如果URL存在返回true，否则返回false
     */
    public boolean isUrlExists(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            
            // 优化连接参数
            connection.setRequestMethod("HEAD"); // 只发送HEAD请求，不下载整个文件
            connection.setConnectTimeout(urlCheckTimeout); // 使用配置的超时时间
            connection.setReadTimeout(urlCheckTimeout);
            connection.setInstanceFollowRedirects(true); // 允许重定向
            connection.setUseCaches(true); // 使用缓存减少重复请求
            connection.setRequestProperty("Connection", "keep-alive"); // 保持连接
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; URLChecker/1.0)"); // 设置User-Agent
            connection.setDoOutput(false); // 不需要输出
            connection.setDoInput(true); // 需要输入
            
            int responseCode = connection.getResponseCode();
            // 200-299 表示成功，301/302表示重定向成功
            return (responseCode >= 200 && responseCode < 300) || responseCode == 301 || responseCode == 302;
        } catch (IOException e) {
            // 只记录重要异常，减少日志量
            if (log.isDebugEnabled()) {
                log.debug("URL检查异常，URL: {}", urlStr, e);
            }
            return false;
        } finally {
            // 确保连接关闭
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 图片URL记录内部类
     */
    private static class ImgUrlRecord {
        private Long id;
        private String imageUrl;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getImageUrl() {
            return imageUrl;
        }
        
        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
    
    /**
     * URL检查结果类
     */
    private static class ImgUrlCheckResult {
        private Long id;
        private boolean exists;
        
        public ImgUrlCheckResult(Long id, boolean exists) {
            this.id = id;
            this.exists = exists;
        }
        
        public Long getId() {
            return id;
        }
        
        public boolean isExists() {
            return exists;
        }
    }
    
    /**
     * 更新记录状态类
     */
    private static class StatusUpdate {
        private Long id;
        private String status;
        
        public StatusUpdate(Long id, String status) {
            this.id = id;
            this.status = status;
        }
        
        public Long getId() {
            return id;
        }
        
        public String getStatus() {
            return status;
        }
    }
}