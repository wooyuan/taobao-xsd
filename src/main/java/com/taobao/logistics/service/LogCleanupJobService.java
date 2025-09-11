package com.taobao.logistics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日志文件清理定时任务服务
 * 每天凌晨2点执行，删除一个月之前的日志文件
 */
@Slf4j
@Service
public class LogCleanupJobService {

    /**
     * 日志文件存放目录，从配置文件读取，默认为当前目录下的LOG文件夹
     */
    @Value("${logging.file.path:${user.dir}/LOG/}")
    private String logDirectory;

    /**
     * 日志文件保留天数，默认30天
     */
    @Value("${log.cleanup.retention.days:30}")
    private int retentionDays;

    /**
     * 定时任务：每天凌晨2点执行日志清理
     * cron表达式：秒 分 时 日 月 周
     * 0 0 2 * * ? 表示每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldLogFiles() {
        log.info("开始执行日志文件清理任务，保留天数：{}天", retentionDays);
        
        try {
            Path logPath = Paths.get(logDirectory);
            
            // 检查日志目录是否存在
            if (!Files.exists(logPath)) {
                log.warn("日志目录不存在：{}", logDirectory);
                return;
            }
            
            // 计算截止日期（当前时间减去保留天数）
            LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(retentionDays);
            Date cutoffDate = Date.from(cutoffDateTime.atZone(ZoneId.systemDefault()).toInstant());
            
            log.info("将删除 {} 之前的日志文件", cutoffDateTime);
            
            // 扫描日志目录
            File logDir = logPath.toFile();
            File[] files = logDir.listFiles();
            
            if (files == null) {
                log.warn("无法读取日志目录：{}", logDirectory);
                return;
            }
            
            int deletedCount = 0;
            long deletedSize = 0;
            
            for (File file : files) {
                // 只处理符合日志文件命名规则的文件
                if (isLogFile(file.getName())) {
                    Date fileModifyTime = new Date(file.lastModified());
                    
                    // 如果文件修改时间早于截止时间，则删除
                    if (fileModifyTime.before(cutoffDate)) {
                        long fileSize = file.length();
                        
                        if (file.delete()) {
                            deletedCount++;
                            deletedSize += fileSize;
                            log.info("已删除过期日志文件：{}, 大小：{} bytes, 修改时间：{}", 
                                    file.getName(), fileSize, fileModifyTime);
                        } else {
                            log.error("删除日志文件失败：{}", file.getName());
                        }
                    }
                }
            }
            
            log.info("日志清理任务完成，共删除 {} 个文件，释放空间：{} bytes", deletedCount, deletedSize);
            
        } catch (Exception e) {
            log.error("执行日志清理任务时发生异常", e);
        }
    }
    
    /**
     * 判断是否为日志文件
     * 匹配规则：
     * - taobao.log（当前日志文件）
     * - taobaoxsd.log.yyyy-MM-dd.gz（压缩的历史日志文件）
     * - taobao.log.yyyy-MM-dd（历史日志文件）
     * 
     * @param fileName 文件名
     * @return 是否为日志文件
     */
    private boolean isLogFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        // 匹配当前日志文件
        if ("taobao.log".equals(fileName) || "taobaoxsd.log".equals(fileName)) {
            return false; // 当前日志文件不删除
        }
        
        // 匹配压缩的历史日志文件 taobaoxsd.log.yyyy-MM-dd.gz
        if (fileName.startsWith("taobaoxsd.log.") && fileName.endsWith(".gz")) {
            return true;
        }
        
        // 匹配历史日志文件 taobao.log.yyyy-MM-dd
        if (fileName.startsWith("taobao.log.") && fileName.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 手动触发日志清理（用于测试）
     */
    public void manualCleanup() {
        log.info("手动触发日志清理任务");
        cleanupOldLogFiles();
    }
}