package com.taobao.logistics.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket处理器
 * 用于处理门店WebSocket客户端的连接、消息和断开连接
 */
@Component
@ServerEndpoint("/websocket/print/{storeId}")
public class PrintWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(PrintWebSocketHandler.class);

    // 静态变量，用来记录当前在线连接数
    private static int onlineCount = 0;

    // concurrent包的线程安全Set，用来存放每个客户端对应的PrintWebSocketHandler对象
    private static CopyOnWriteArraySet<PrintWebSocketHandler> webSocketSet = new CopyOnWriteArraySet<>();

    // 静态变量，用来根据storeId映射对应的WebSocket连接
    private static ConcurrentHashMap<String, PrintWebSocketHandler> storeWebSocketMap = new ConcurrentHashMap<>();

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // 门店ID
    private String storeId;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("storeId") String storeId) {
        this.session = session;
        this.storeId = storeId;
        
        // 添加到set中
        webSocketSet.add(this);
        // 添加到store映射中
        storeWebSocketMap.put(storeId, this);
        
        // 在线数加1
        addOnlineCount();
        
        logger.info("【WebSocket连接】门店[{}]连接成功！当前在线人数为：{}", storeId, getOnlineCount());
        logger.debug("【WebSocket连接】门店[{}]连接详情：sessionId={}, storeId={}", storeId, session.getId(), storeId);
        
        // 发送连接成功消息
        try {
            sendMessage("连接成功");
            logger.debug("【WebSocket发送】向门店[{}]发送连接成功消息：连接成功", storeId);
        } catch (IOException e) {
            logger.error("【WebSocket发送】门店[{}]发送消息异常：{}", storeId, e.getMessage());
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        // 从set中删除
        webSocketSet.remove(this);
        // 从store映射中删除
        storeWebSocketMap.remove(this.storeId);
        
        // 在线数减1
        subOnlineCount();
        
        logger.info("【WebSocket断开】门店[{}]连接关闭！当前在线人数为：{}", this.storeId, getOnlineCount());
        logger.debug("【WebSocket断开】门店[{}]断开详情：sessionId={}, storeId={}", this.storeId, this.session.getId(), this.storeId);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("【WebSocket接收】收到客户端消息：sessionId={}, storeId={}, message={}", session.getId(), this.storeId, message);
        
        try {
            // 解析消息，支持两种格式：
            // 1. 直接的打印数据（门店客户端发送的消息）
            // 2. JSON格式：{"storeId":"目标门店ID", "printData":"打印数据"}（管理端发送的打印请求）
            
            // 检查是否为JSON格式的打印请求
            if (message.startsWith("{") && message.endsWith("}")) {
                // 解析JSON消息
                try {
                    com.alibaba.fastjson.JSONObject jsonMessage = com.alibaba.fastjson.JSONObject.parseObject(message);
                    String targetStoreId = jsonMessage.getString("storeId");
                    String printData = jsonMessage.getString("printData");
                    
                    if (targetStoreId != null && printData != null) {
                        logger.info("【WebSocket转发】收到打印请求，目标门店：{}，打印数据：{}", targetStoreId, printData);
                        
                        // 转发打印指令到目标门店
                        boolean success = sendMessageToStore(targetStoreId, printData);
                        
                        // 回复发送结果
                        String replyMessage;
                        if (success) {
                            replyMessage = "打印指令已发送到门店[" + targetStoreId + "]";
                            logger.info("【WebSocket回复】打印指令发送成功，回复：{}", replyMessage);
                        } else {
                            replyMessage = "打印指令发送失败，门店[" + targetStoreId + "]可能离线";
                            logger.warn("【WebSocket回复】打印指令发送失败，回复：{}", replyMessage);
                        }
                        sendMessage(replyMessage);
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("【WebSocket解析】JSON消息解析失败，可能是普通消息：{}", e.getMessage());
                    // 继续处理，当作普通消息
                }
            }
            
            // 普通消息处理（门店客户端发送的消息）
            logger.info("【WebSocket消息】收到门店[{}]的普通消息：{}", this.storeId, message);
            // 回复收到消息
            String replyMessage = "收到消息：" + message;
            sendMessage(replyMessage);
            logger.debug("【WebSocket发送】向门店[{}]回复消息：{}", this.storeId, replyMessage);
        } catch (IOException e) {
            logger.error("【WebSocket发送】发送消息异常：{}", e.getMessage());
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("【WebSocket错误】门店[{}]WebSocket发生错误：{}", this.storeId, error.getMessage());
        logger.error("【WebSocket错误】门店[{}]错误详情：sessionId={}, error={}", this.storeId, session.getId(), error.getMessage(), error);
    }

    /**
     * 向客户端发送消息
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
        logger.debug("【WebSocket发送】向门店[{}]发送消息：{}", this.storeId, message);
    }

    /**
     * 向指定门店发送消息
     *
     * @param storeId  门店ID
     * @param message 消息内容
     */
    public static boolean sendMessageToStore(String storeId, String message) {
        logger.info("向门店[{}]发送消息：{}", storeId, message);
        PrintWebSocketHandler webSocket = storeWebSocketMap.get(storeId);
        if (webSocket != null) {
            try {
                webSocket.sendMessage(message);
                return true;
            } catch (IOException e) {
                logger.error("向门店[{}]发送消息失败：{}", storeId, e.getMessage());
                return false;
            }
        }
        logger.error("门店[{}]未连接WebSocket", storeId);
        return false;
    }

    /**
     * 向所有客户端发送消息
     *
     * @param message 消息内容
     */
    public static void sendMessageToAll(String message) {
        logger.info("向所有门店发送消息：{}", message);
        for (PrintWebSocketHandler webSocket : webSocketSet) {
            try {
                webSocket.sendMessage(message);
            } catch (IOException e) {
                logger.error("向门店[{}]发送消息失败：{}", webSocket.storeId, e.getMessage());
            }
        }
    }

    /**
     * 获取当前在线数
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * 在线数加1
     */
    public static synchronized void addOnlineCount() {
        PrintWebSocketHandler.onlineCount++;
    }

    /**
     * 在线数减1
     */
    public static synchronized void subOnlineCount() {
        PrintWebSocketHandler.onlineCount--;
    }

    /**
     * 获取门店WebSocket连接状态
     *
     * @param storeId 门店ID
     * @return 是否在线
     */
    public static boolean isStoreOnline(String storeId) {
        return storeWebSocketMap.containsKey(storeId);
    }
}
