package com.taobao.logistics.utils;

/**
 * Created by ShiShiDaWei on 2021/8/31.
 */

import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class WebSocketClientManager extends WebSocketClient {

    private static WebSocketClientManager webSocket = null;

    private int sendFlag = 0;
    private String result = null;
    
    static {
        try {
            webSocket = new WebSocketClientManager(new URI(LogisticsConfig.WS_URL), new Draft_17());
            webSocket.connect();
            log.debug("Static Block....");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    // 获取到当前实例
    public static WebSocketClientManager getwebSocket(){
        try{
            if(webSocket != null){
                if(webSocket.getReadyState() == WebSocket.READYSTATE.NOT_YET_CONNECTED
                        || webSocket.getReadyState() == WebSocket.READYSTATE.CLOSED
                        || webSocket.getReadyState().equals(WebSocket.READYSTATE.CLOSING)){
                    webSocket.connectBlocking();
                }
            }else{
                webSocket = new WebSocketClientManager(new URI(LogisticsConfig.WS_URL), new Draft_17());
                webSocket.connect();
            }
        }catch (Exception ex){
            webSocket = null;
            log.error(" websocket 构建实例错误！！" + ex);
        }
        return webSocket;
    }


    public String sendStr(String text){
        synchronized(this){
            sendFlag = 1;
            this.send(text);
            while(sendFlag != 0){
                log.debug("Waiting Retrun Value....============== " + sendFlag);

            }
            return result;
        }
    }


    private WebSocketClientManager(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }


    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        log.debug(" ws STARTED！！");
    }

    //WebSocket回调函数
    @Override
    public void onMessage(String message) {
        result = message;
        sendFlag = 0;
        System.out.println(message);
        log.debug("ws Recive MESSAGE！！" + message);

    }

    @Override
    public void onClose(int i, String s, boolean b) {
        result = null;
        sendFlag = 0;
        System.out.println("i = [" + i + "], s = [" + s + "], b = [" + b + "]");
        log.debug(" ws CLOSSEED！！");
    }

    @Override
    public void onError(Exception e) {
        result = null;
        sendFlag = 0;
        System.out.println(e.getMessage());
        log.debug("  ws Client ERROR！！");

    }
}

