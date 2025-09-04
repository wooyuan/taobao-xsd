package com.taobao.logistics.utils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * websocket client 客户端端控制
 */
public class WebSocketClient extends org.java_websocket.client.WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

    // 复制请留意，该位置url需要进行更改
    private static String wsUrl = "ws://127.0.0.1:10080//";

    private static WebSocketClient instance;

    private int sendFlag = 0;
    private String result = null;

    static {
        try {
            instance = new WebSocketClient(wsUrl);
            instance.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // 获取到当前实例
    public static WebSocketClient getInstance(){
        try{
            if(instance != null){
                if(instance.getReadyState() == WebSocket.READYSTATE.NOT_YET_CONNECTED
                        || instance.getReadyState() == WebSocket.READYSTATE.CLOSED){
                    instance.connect();
                }
            }else{
                instance = new WebSocketClient(wsUrl);
                instance.connect();
            }
        }catch (Exception ex){
            instance = null;
            logger.error(" websocket 构建实例错误！！" + ex);
        }
        return instance;
    }

    // 发送字符串消息
    public String sendStr(String text){
        synchronized(this){
            sendFlag = 1;
            this.send(text);
            while(sendFlag != 0){
                logger.debug("Waiting Retrun Value.... =============== " + sendFlag);
            }
            return result;
        }
    }


    private WebSocketClient(String url) throws URISyntaxException {
        super(new URI(url));
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        logger.debug(" ws STARTED！！");
    }

    @Override
    public void onMessage(String s) {
        result = s;
        sendFlag = 0;
        logger.debug(" ws Recive MESSAGE！！" + s);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        result = null;
        sendFlag = 0;
        logger.debug(" ws CLOSSEED！！");
    }

    @Override
    public void onError(Exception e) {
        result = null;
        sendFlag = 0;
        logger.debug(" ws Client ERROR！！");
    }
}
