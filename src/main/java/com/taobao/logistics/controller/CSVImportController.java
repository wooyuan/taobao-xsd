package com.taobao.logistics.controller;

import com.taobao.logistics.utils.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CSV导入控制器
 * 处理客户积分数据的CSV文件导入
 */
@RestController
@RequestMapping("/api/csv")
public class CSVImportController {

    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;
    
    // 添加日志记录器
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CSVImportController.class);

    /**
     * 导入CSV文件到HD_VIP_INFO表
     * @param file CSV文件
     * @return 导入结果
     */
    @PostMapping("/import")
    public AjaxResult importCSV(@RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis();
        
        // 验证文件
        if (file.isEmpty()) {
            return AjaxResult.error("文件为空");
        }
        
        if (!file.getOriginalFilename().endsWith(".csv")) {
            return AjaxResult.error("请选择CSV格式文件");
        }
        
        // 导入结果统计
        AtomicInteger totalRows = new AtomicInteger(0);
        AtomicInteger successRows = new AtomicInteger(0);
        AtomicInteger errorRows = new AtomicInteger(0);
        List<ErrorDetail> errorDetails = new ArrayList<>();
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        BufferedReader reader = null;
        InputStream inputStream = null;
        
        try {
            // 获取数据库连接
            conn = secondaryDataSource.getConnection();
            
            // 准备插入语句
            String sql = "INSERT INTO HD_VIP_INFO (mobile, integral) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);
            
            // 开始事务
            conn.setAutoCommit(false);
            
            // 改进编码处理，支持GBK和UTF-8编码
            inputStream = file.getInputStream();
            
            // 检测文件编码
            byte[] bom = new byte[3];
            inputStream.mark(3); // 标记流位置以便重置
            int read = inputStream.read(bom);
            String encoding = "GBK";
            if (read == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                encoding = "UTF-8";
            } else {
                // 重置流位置
                inputStream.reset();
            }
            
            // 使用检测到的编码创建阅读器
            reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
            
            // 创建表（如果不存在）
            createTableIfNotExists(conn);
            
            String line;
            int rowNumber = 0;
            boolean isFirstLine = true;
            
            // 动态列索引
            int mobileColumnIndex = -1;
            int integralColumnIndex = -1;
            
            // 读取文件内容
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                totalRows.incrementAndGet();
                
                try {
                    // 跳过空行
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 处理BOM标记（如果存在）
                    String processedLine = line;
                    if (processedLine.startsWith("\uFEFF")) {
                        processedLine = processedLine.substring(1);
                    }
                    
                    // 分割CSV行，支持制表符和逗号分隔
                    String[] parts;
                    if (processedLine.contains("\t")) {
                        parts = processedLine.split("\t");
                    } else {
                        // 支持带引号的CSV字段
                        parts = processedLine.split(",", -1);
                    }
                    
                    // 处理表头行，动态确定列索引
                    if (isFirstLine) {
                        isFirstLine = false;
                        
                        // 遍历表头，寻找包含"手机号"相关的列
                        for (int i = 0; i < parts.length; i++) {
                            String header = parts[i].trim();
                            // 移除可能的引号
                            header = header.replaceAll("^\"|\"$", "");
                            
                            // 支持更多的表头文字变体，使用不区分大小写的匹配
                            String lowerHeader = header.toLowerCase();
                            
                            if (lowerHeader.contains("手机号") || lowerHeader.contains("手机") || lowerHeader.contains("电话") || 
                                lowerHeader.contains("联系电话") || lowerHeader.contains("客户电话") || 
                                lowerHeader.contains("客户手机") || lowerHeader.contains("用户手机") ||
                                lowerHeader.contains("phone") || lowerHeader.contains("mobile")) {
                                mobileColumnIndex = i;
                                break;
                            }
                        }
                        
                        // 遍历表头，寻找包含"积分"相关的列
                        // 优先匹配"客户积分"列
                        for (int i = 0; i < parts.length; i++) {
                            String header = parts[i].trim();
                            // 移除可能的引号
                            header = header.replaceAll("^\"|\"$", "");
                            
                            // 支持更多的表头文字变体，使用不区分大小写的匹配
                            String lowerHeader = header.toLowerCase();
                            
                            // 优先匹配"客户积分"
                            if (lowerHeader.contains("客户积分")) {
                                integralColumnIndex = i;
                                break;
                            }
                        }
                        
                        // 如果没有找到"客户积分"，再尝试其他积分相关列
                        if (integralColumnIndex == -1) {
                            for (int i = 0; i < parts.length; i++) {
                                String header = parts[i].trim();
                                // 移除可能的引号
                                header = header.replaceAll("^\"|\"$", "");
                                
                                // 支持更多的表头文字变体，使用不区分大小写的匹配
                                String lowerHeader = header.toLowerCase();
                                
                                if (lowerHeader.contains("积分") || lowerHeader.contains("成长值") || lowerHeader.contains("会员积分") || 
                                    lowerHeader.contains("用户积分") ||
                                    lowerHeader.contains("integral") || lowerHeader.contains("points")) {
                                    integralColumnIndex = i;
                                    break;
                                }
                            }
                        }
                        
                        // 验证是否找到了必要的列
                        boolean hasError = false;
                        if (mobileColumnIndex == -1) {
                            hasError = true;
                        }
                        
                        if (integralColumnIndex == -1) {
                            hasError = true;
                        }
                        
                        if (hasError) {
                            // 如果未找到必要列，记录错误
                            errorRows.incrementAndGet();
                            if (mobileColumnIndex == -1) {
                                errorDetails.add(new ErrorDetail(rowNumber, "格式错误：未找到包含'手机号'、'手机'、'电话'等相关文字的列"));
                            }
                            if (integralColumnIndex == -1) {
                                errorDetails.add(new ErrorDetail(rowNumber, "格式错误：未找到包含'积分'、'成长值'等相关文字的列"));
                            }
                            // 停止处理，因为表头不正确
                            break;
                        }
                        
                        continue; // 跳过表头行的处理
                    }
                    
                    // 如果表头未正确处理，不再处理后续行
                    if (mobileColumnIndex == -1 || integralColumnIndex == -1) {
                        continue;
                    }
                    
                    // 确保数据行有足够的列
                    if (parts.length <= mobileColumnIndex || parts.length <= integralColumnIndex) {
                        errorRows.incrementAndGet();
                        errorDetails.add(new ErrorDetail(rowNumber, "格式错误：数据行缺少必要的列"));
                        continue;
                    }
                    
                    // 提取数据（使用动态找到的列索引）
                    String mobile = parts[mobileColumnIndex].trim();
                    String integralStr = parts[integralColumnIndex].trim();
                    
                    // 移除可能的引号
                    mobile = mobile.replaceAll("^\"|\"$", "");
                    integralStr = integralStr.replaceAll("^\"|\"$", "");
                    
                    // 跳过空手机号记录
                    if (mobile.isEmpty()) {
                        continue;
                    }
                    
                    // 简化手机号处理：取消格式校验，接受各种格式
                    String cleanedMobile = mobile;
                    
                    // 处理科学计数法
                    if (mobile.contains("E+")) {
                        try {
                            double mobileDouble = Double.parseDouble(mobile);
                            cleanedMobile = String.format("%.0f", mobileDouble);
                        } catch (NumberFormatException e) {
                            // 转换失败，继续使用原字符串
                        }
                    }
                    
                    // 移除特殊字符和格式化字符
                    cleanedMobile = cleanedMobile.replaceAll("[^0-9]", "");
                    
                    // 取消手机号格式校验，接受任何格式的手机号
                    // 如果处理后为空，则跳过该记录
                    if (cleanedMobile.isEmpty()) {
                        continue;
                    }
                    
                    // 验证积分
                    double integral;
                    try {
                        // 处理空积分值
                        if (integralStr.isEmpty()) {
                            integral = 0.0;
                        } else {
                            integral = Double.parseDouble(integralStr);
                        }
                    } catch (NumberFormatException e) {
                        errorRows.incrementAndGet();
                        errorDetails.add(new ErrorDetail(rowNumber, "积分格式错误：" + integralStr));
                        continue;
                    }
                    
                    // 设置参数，使用处理后的手机号
                    pstmt.setString(1, cleanedMobile);
                    pstmt.setDouble(2, integral);
                    
                    // 添加到批处理
                    pstmt.addBatch();
                    
                    // 每10000行执行一次批处理，减少事务提交次数
                    if (rowNumber % 10000 == 0) {
                        int[] results = pstmt.executeBatch();
                        
                        // 统计成功行数
                        for (int result : results) {
                            if (result > 0 || result == Statement.SUCCESS_NO_INFO) {
                                successRows.incrementAndGet();
                            }
                        }
                        
                        // 清空批处理，准备下一批
                        pstmt.clearBatch();
                    }
                    
                } catch (Exception e) {
                    errorRows.incrementAndGet();
                    errorDetails.add(new ErrorDetail(rowNumber, "处理失败：" + e.getMessage()));
                }
            }
            
            // 执行剩余的批处理
            int[] results = pstmt.executeBatch();
            
            // 统计剩余成功行数
            for (int result : results) {
                if (result > 0 || result == Statement.SUCCESS_NO_INFO) {
                    successRows.incrementAndGet();
                }
            }
            
            // 提交事务
            conn.commit();
            
        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // 简化异常处理
                }
            }
            
            return AjaxResult.error("导入失败：" + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (reader != null) {
                    reader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                // 简化资源关闭异常处理
            }
        }
        
        long endTime = System.currentTimeMillis();
        double timeCost = (endTime - startTime) / 1000.0;
        
        // 构建返回结果，适配前端期望的格式
        ImportResult result = new ImportResult();
        result.setTotalRows(totalRows.get());
        result.setSuccessRows(successRows.get());
        result.setErrorRows(errorRows.get());
        result.setSuccessCount(successRows.get()); // 前端期望的字段名，与successRows保持一致
        result.setErrorCount(errorRows.get()); // 前端期望的字段名，与errorRows保持一致
        result.setTimeCost(String.format("%.2f", timeCost));
        result.setErrorDetails(errorDetails);
        
        // 返回包含success字段的结果，确保前端能正确识别
        return AjaxResult.success(result);
    }
    
    /**
     * 创建表（如果不存在）
     * @param conn 数据库连接
     */
    private void createTableIfNotExists(Connection conn) throws SQLException {
        // Oracle不支持IF NOT EXISTS语法，需要先检查表是否存在
        String checkTableSql = "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'HD_VIP_INFO'";
        PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
        java.sql.ResultSet rs = checkStmt.executeQuery();
        rs.next();
        int tableCount = rs.getInt(1);
        rs.close();
        checkStmt.close();
        
        // 如果表不存在，则创建表
        if (tableCount == 0) {
            String createTableSql = "" +
                "CREATE TABLE HD_VIP_INFO " +
                "( " +
                "  mobile   VARCHAR2(50), " +
                "  integral NUMBER(12,2) " +
                ")";
            
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
            createStmt.close();
        }
    }
    
    /**
     * 导入结果类
     */
    static class ImportResult {
        private int totalRows;
        private int successRows;
        private int errorRows;
        // 适配前端的字段名
        private int successCount;
        private int errorCount;
        private String timeCost;
        private List<ErrorDetail> errorDetails;
        
        // Getters and Setters
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public int getSuccessRows() { return successRows; }
        public void setSuccessRows(int successRows) { this.successRows = successRows; }
        public int getErrorRows() { return errorRows; }
        public void setErrorRows(int errorRows) { this.errorRows = errorRows; }
        // 适配前端的字段名
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public String getTimeCost() { return timeCost; }
        public void setTimeCost(String timeCost) { this.timeCost = timeCost; }
        public List<ErrorDetail> getErrorDetails() { return errorDetails; }
        public void setErrorDetails(List<ErrorDetail> errorDetails) { this.errorDetails = errorDetails; }
    }
    
    /**
     * 错误详情类
     */
    static class ErrorDetail {
        private int row;
        private String message;
        
        public ErrorDetail(int row, String message) {
            this.row = row;
            this.message = message;
        }
        
        // Getters
        public int getRow() { return row; }
        public String getMessage() { return message; }
    }
}
