package com.taobao.logistics.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 数据库初始化类，在应用启动时执行SQL语句
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("开始执行数据库初始化操作...");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 1. 创建系统时间轴序列（如果不存在）
            try {
                // 先尝试创建系统时间轴序列
                String createSeqSql = "CREATE SEQUENCE SYSTEM_TIMELINE_SEQ " +
                        "INCREMENT BY 1 " +
                        "START WITH 1 " +
                        "NOCACHE " +
                        "NOCYCLE " +
                        "ORDER";
                stmt.executeUpdate(createSeqSql);
                logger.info("成功创建SYSTEM_TIMELINE_SEQ序列");
            } catch (Exception e) {
                // 如果序列已存在，忽略此错误
                if (e.getMessage().contains("ORA-00955")) {
                    logger.info("SYSTEM_TIMELINE_SEQ序列已存在，跳过创建");
                } else {
                    logger.error("创建系统时间轴序列失败: {}", e.getMessage());
                }
            }
            
            // 2. 创建系统信息序列（如果不存在）
            try {
                // 尝试创建系统信息序列
                String createSystemInfoSeqSql = "CREATE SEQUENCE SYSTEM_INFO_SEQ " +
                        "INCREMENT BY 1 " +
                        "START WITH 1 " +
                        "NOCACHE " +
                        "NOCYCLE " +
                        "ORDER";
                stmt.executeUpdate(createSystemInfoSeqSql);
                logger.info("成功创建SYSTEM_INFO_SEQ序列");
            } catch (Exception e) {
                // 如果序列已存在，忽略此错误
                if (e.getMessage().contains("ORA-00955")) {
                    logger.info("SYSTEM_INFO_SEQ序列已存在，跳过创建");
                } else {
                    logger.error("创建系统信息序列失败: {}", e.getMessage());
                }
            }
            
            // 3. 创建员工工时序列（如果不存在）
            try {
                // 尝试创建员工工时序列
                String createWorkHourSeqSql = "CREATE SEQUENCE WORK_HOUR_SEQ " +
                        "INCREMENT BY 1 " +
                        "START WITH 1 " +
                        "NOCACHE " +
                        "NOCYCLE " +
                        "ORDER";
                stmt.executeUpdate(createWorkHourSeqSql);
                logger.info("成功创建WORK_HOUR_SEQ序列");
            } catch (Exception e) {
                // 如果序列已存在，忽略此错误
                if (e.getMessage().contains("ORA-00955")) {
                    logger.info("WORK_HOUR_SEQ序列已存在，跳过创建");
                } else {
                    logger.error("创建员工工时序列失败: {}", e.getMessage());
                }
            }
            
            // 4. 创建员工工时表（如果不存在）
            try {
                // 尝试创建员工工时表
                String createWorkHourTableSql = "CREATE TABLE WORK_HOUR (" +
                        "ID NUMBER(11) NOT NULL, " +
                        "SYSTEM VARCHAR2(100) NOT NULL, " +
                        "WORK_MONTH VARCHAR2(50), " +
                        "WORK_WEEK VARCHAR2(50), " +
                        "WORK_WEEK_END_DATE VARCHAR2(20), " +
                        "EMPLOYEE VARCHAR2(100) NOT NULL, " +
                        "WORK_CATEGORY VARCHAR2(100), " +
                        "TYPE VARCHAR2(100), " +
                        "ITEM VARCHAR2(500), " +
                        "REPORTER VARCHAR2(100), " +
                        "COST_DEPARTMENT VARCHAR2(100), " +
                        "PLANNED_HOURS NUMBER(10,2), " +
                        "ACTUAL_HOURS NUMBER(10,2), " +
                        "CREATE_TIME DATE DEFAULT SYSDATE, " +
                        "UPDATE_TIME DATE DEFAULT SYSDATE, " +
                        "CONSTRAINT PK_WORK_HOUR PRIMARY KEY (ID)" +
                        ")";
                stmt.executeUpdate(createWorkHourTableSql);
                logger.info("成功创建WORK_HOUR表");
            } catch (Exception e) {
                // 如果表已存在，忽略此错误
                if (e.getMessage().contains("ORA-00955")) {
                    logger.info("WORK_HOUR表已存在，跳过创建");
                    
                    // 尝试给现有表添加新字段
                    try {
                        String addColumnSql = "ALTER TABLE WORK_HOUR ADD WORK_WEEK_END_DATE VARCHAR2(20)";
                        stmt.executeUpdate(addColumnSql);
                        logger.info("成功给WORK_HOUR表添加WORK_WEEK_END_DATE字段");
                    } catch (Exception ex) {
                        // 如果字段已存在，忽略此错误
                        if (ex.getMessage().contains("ORA-00904")) {
                            logger.info("WORK_WEEK_END_DATE字段已存在，跳过添加");
                        } else {
                            logger.error("给WORK_HOUR表添加字段失败: {}", ex.getMessage());
                        }
                    }
                } else {
                    logger.error("创建员工工时表失败: {}", e.getMessage());
                }
            }
            
            // 2. 修复系统时间轴触发器
            String fixTriggerSql = "CREATE OR REPLACE TRIGGER SYSTEM_TIMELINE_TRIG " +
                    "BEFORE INSERT OR UPDATE ON SYSTEM_TIMELINE " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "  IF INSERTING THEN " +
                    "    IF :NEW.ID IS NULL THEN " +
                    "      SELECT SYSTEM_TIMELINE_SEQ.NEXTVAL INTO :NEW.ID FROM DUAL; " +
                    "    END IF; " +
                    "    IF :NEW.CREATE_TIME IS NULL THEN " +
                    "      :NEW.CREATE_TIME := SYSDATE; " +
                    "    END IF; " +
                    "    :NEW.UPDATE_TIME := SYSDATE; " +
                    "  END IF; " +
                    "  " +
                    "  IF UPDATING THEN " +
                    "    :NEW.UPDATE_TIME := SYSDATE; " +
                    "  END IF; " +
                    "END;";
            
            stmt.executeUpdate(fixTriggerSql);
            logger.info("成功修复SYSTEM_TIMELINE_TRIG触发器");
            
            // 4. 创建员工工时触发器
            String createWorkHourTriggerSql = "CREATE OR REPLACE TRIGGER WORK_HOUR_TRIG " +
                    "BEFORE INSERT OR UPDATE ON WORK_HOUR " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "  IF INSERTING THEN " +
                    "    IF :NEW.ID IS NULL THEN " +
                    "      SELECT WORK_HOUR_SEQ.NEXTVAL INTO :NEW.ID FROM DUAL; " +
                    "    END IF; " +
                    "    IF :NEW.CREATE_TIME IS NULL THEN " +
                    "      :NEW.CREATE_TIME := SYSDATE; " +
                    "    END IF; " +
                    "    :NEW.UPDATE_TIME := SYSDATE; " +
                    "  END IF; " +
                    "  " +
                    "  IF UPDATING THEN " +
                    "    :NEW.UPDATE_TIME := SYSDATE; " +
                    "  END IF; " +
                    "END;";
            
            stmt.executeUpdate(createWorkHourTriggerSql);
            logger.info("成功创建WORK_HOUR_TRIG触发器");
            
            // 5. 启用触发器（如果被禁用）
            stmt.executeUpdate("ALTER TRIGGER SYSTEM_TIMELINE_TRIG ENABLE");
            stmt.executeUpdate("ALTER TRIGGER WORK_HOUR_TRIG ENABLE");
            logger.info("成功启用所有触发器");
            
        } catch (Exception e) {
            logger.error("数据库初始化失败: {}", e.getMessage());
            // 不抛出异常，允许应用继续运行
        }
        
        logger.info("数据库初始化操作完成");
    }
}