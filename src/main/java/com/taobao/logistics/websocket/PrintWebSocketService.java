package com.taobao.logistics.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * WebSocket服务
 * 用于管理WebSocket连接和发送打印指令
 */
@Service
public class PrintWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(PrintWebSocketService.class);

    /**
     * 发送打印指令到指定门店
     *
     * @param storeId 门店ID
     * @param printData 打印数据
     * @return 是否发送成功
     */
    public boolean sendPrintCommand(String storeId, String printData) {
        logger.info("【WebSocket服务】发送打印指令到门店[{}]，打印数据：{}", storeId, printData);
        logger.debug("【WebSocket服务】打印指令详情：storeId={}, printData={}", storeId, printData);
        boolean result = PrintWebSocketHandler.sendMessageToStore(storeId, printData);
        logger.info("【WebSocket服务】发送打印指令到门店[{}]结果：{}", storeId, result ? "成功" : "失败");
        return result;
    }

    /**
     * 发送打印指令到所有门店
     *
     * @param printData 打印数据
     */
    public void sendPrintCommandToAll(String printData) {
        logger.info("【WebSocket服务】发送打印指令到所有门店，打印数据：{}", printData);
        logger.debug("【WebSocket服务】广播打印指令详情：printData={}", printData);
        PrintWebSocketHandler.sendMessageToAll(printData);
        logger.info("【WebSocket服务】打印指令已发送到所有门店");
    }

    /**
     * 查询门店WebSocket连接状态
     *
     * @param storeId 门店ID
     * @return 是否在线
     */
    public boolean checkStoreConnection(String storeId) {
        boolean isOnline = PrintWebSocketHandler.isStoreOnline(storeId);
        logger.info("【WebSocket服务】查询门店[{}]连接状态：{}", storeId, isOnline ? "在线" : "离线");
        logger.debug("【WebSocket服务】门店[{}]连接状态查询结果：storeId={}, isOnline={}", storeId, storeId, isOnline);
        return isOnline;
    }

    /**
     * 获取当前在线门店数量
     *
     * @return 在线门店数量
     */
    public int getOnlineStoreCount() {
        int count = PrintWebSocketHandler.getOnlineCount();
        logger.info("【WebSocket服务】获取当前在线门店数量：{}", count);
        logger.debug("【WebSocket服务】在线门店数量查询结果：count={}", count);
        return count;
    }
}
