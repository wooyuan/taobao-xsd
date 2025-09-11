package com.taobao.logistics.controller;

import com.taobao.logistics.service.LogCleanupJobService;
import com.taobao.logistics.utils.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 日志管理控制器
 * 提供日志清理的手动触发和状态查看功能
 */
@Slf4j
@RestController
@RequestMapping("/api/log")
public class LogManagementController {

    @Autowired
    private LogCleanupJobService logCleanupJobService;

    @Value("${logging.file.path:${user.dir}/LOG/}")
    private String logDirectory;

    @Value("${log.cleanup.retention.days:30}")
    private int retentionDays;

    /**
     * 手动触发日志清理
     */
    @PostMapping("/cleanup")
    public AjaxResult manualCleanup() {
        try {
            log.info("收到手动清理日志请求");
            logCleanupJobService.manualCleanup();
            return AjaxResult.success("日志清理任务已执行完成");
        } catch (Exception e) {
            log.error("手动清理日志失败", e);
            return AjaxResult.error("日志清理失败：" + e.getMessage());
        }
    }

    /**
     * 获取日志文件状态信息
     */
    @GetMapping("/status")
    public AjaxResult getLogStatus() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 基本配置信息
            result.put("logDirectory", logDirectory);
            result.put("retentionDays", retentionDays);
            result.put("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            Path logPath = Paths.get(logDirectory);
            
            if (!Files.exists(logPath)) {
                result.put("directoryExists", false);
                result.put("message", "日志目录不存在");
                return AjaxResult.success(result);
            }
            
            result.put("directoryExists", true);
            
            // 扫描日志文件
            File logDir = logPath.toFile();
            File[] files = logDir.listFiles();
            
            if (files == null) {
                result.put("message", "无法读取日志目录");
                return AjaxResult.success(result);
            }
            
            List<Map<String, Object>> logFiles = new ArrayList<>();
            long totalSize = 0;
            int currentLogCount = 0;
            int oldLogCount = 0;
            
            // 计算截止日期
            LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(retentionDays);
            Date cutoffDate = Date.from(cutoffDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
            
            for (File file : files) {
                if (isLogFile(file.getName())) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("size", file.length());
                    fileInfo.put("lastModified", new Date(file.lastModified()));
                    
                    Date fileModifyTime = new Date(file.lastModified());
                    boolean isOld = fileModifyTime.before(cutoffDate);
                    fileInfo.put("isOld", isOld);
                    
                    if (isOld) {
                        oldLogCount++;
                    } else {
                        currentLogCount++;
                    }
                    
                    totalSize += file.length();
                    logFiles.add(fileInfo);
                }
            }
            
            // 按修改时间倒序排列
            logFiles.sort((a, b) -> ((Date) b.get("lastModified")).compareTo((Date) a.get("lastModified")));
            
            result.put("logFiles", logFiles);
            result.put("totalSize", totalSize);
            result.put("currentLogCount", currentLogCount);
            result.put("oldLogCount", oldLogCount);
            result.put("cutoffDate", cutoffDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return AjaxResult.success(result);
            
        } catch (Exception e) {
            log.error("获取日志状态失败", e);
            return AjaxResult.error("获取日志状态失败：" + e.getMessage());
        }
    }
    
    /**
     * 判断是否为日志文件
     */
    private boolean isLogFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        // 匹配当前日志文件
        if ("taobao.log".equals(fileName) || "taobaoxsd.log".equals(fileName)) {
            return true;
        }
        
        // 匹配压缩的历史日志文件
        if (fileName.startsWith("taobaoxsd.log.") && fileName.endsWith(".gz")) {
            return true;
        }
        
        // 匹配历史日志文件
        if (fileName.startsWith("taobao.log.") && fileName.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
            return true;
        }
        
        return false;
    }
}