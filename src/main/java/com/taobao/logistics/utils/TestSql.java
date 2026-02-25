package com.taobao.logistics.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class TestSql {
    public static void main(String[] args) {
        // 数据库连接信息
        String url = "jdbc:oracle:thin:@10.100.21.181:1521/orcl";
        String username = "neands3";
        String password = "abc123";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // 加载驱动
            Class.forName("oracle.jdbc.OracleDriver");
            
            // 建立连接
            conn = DriverManager.getConnection(url, username, password);
            System.out.println("数据库连接成功");
            
            // 测试SQL查询语句
            String sql = "select cost_department, sum(nvl(actual_hours, 0)) as tot_hours from WORK_HOUR group by cost_department order by tot_hours desc";
            pstmt = conn.prepareStatement(sql);
            
            // 执行查询
            rs = pstmt.executeQuery();
            
            // 获取结果集元数据
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 打印列名
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(metaData.getColumnName(i) + "\t");
            }
            System.out.println();
            
            // 打印查询结果
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }
            
            // 检查表结构
            System.out.println("\n表结构信息：");
            sql = "desc WORK_HOUR";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                System.out.println(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}