package com.taobao.logistics.integration.taobao.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 独立的URL有效性检查测试类
 * 基于ProductImgUrlSyncJobService中的URL检查逻辑实现
 */
public class ProductImgUrlSyncAppTest {

    public static void main(String[] args) {
        System.out.println("开始测试URL有效性检查功能");
        
        try {
            // 测试指定的URL
            testSingleUrl("http://61.190.44.115:9210/JNR/1101/164496330.jpg");
            // 保留其他测试URL进行对比
            testSingleUrl("http://61.190.44.115:9210/JNR/1101/105425355.jpg");
            testSingleUrl("http://example.com/nonexistent.jpg");
            
            System.out.println("所有URL测试完成");
        } catch (Exception e) {
            System.err.println("测试过程中发生异常");
            e.printStackTrace();
        }
    }
    
    /**
     * 测试单个URL的有效性
     */
    private static void testSingleUrl(String url) {
        System.out.println("\n测试URL: " + url);
        long startTime = System.currentTimeMillis();
        
        try {
            boolean exists = isUrlExists(url);
            long endTime = System.currentTimeMillis();
            System.out.println("URL检查结果: " + exists + ", 耗时: " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            System.err.println("检查URL时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * URL有效性检查方法
     * 基于ProductImgUrlSyncJobService中的isUrlExists方法逻辑实现
     */
    private static boolean isUrlExists(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接超时为3秒
            connection.setConnectTimeout(3000);
            // 设置读取超时为3秒
            connection.setReadTimeout(3000);
            // 仅发送HEAD请求，不获取响应体
            connection.setRequestMethod("HEAD");
            // 允许自动重定向
            connection.setInstanceFollowRedirects(true);
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            
            // 响应码在200-299之间，或者301、302表示URL有效
            System.out.println("HTTP响应码: " + responseCode);
            return (responseCode >= 200 && responseCode < 300) || responseCode == 301 || responseCode == 302;
        } catch (IOException e) {
            System.err.println("URL检查IO异常: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
