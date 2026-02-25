package com.taobao.logistics.controller;

import com.taobao.logistics.utils.AjaxResult;
import com.taobao.logistics.websocket.PrintWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket打印控制器
 * 提供API接口用于发送打印指令
 */
@Slf4j
@RestController
@RequestMapping("/api/print")
public class PrintController {

    @Autowired
    private PrintWebSocketService printWebSocketService;

    /**
     * 发送打印指令到指定门店
     *
     * @param storeId 门店ID
     * @param printData 打印数据
     * @return 操作结果
     */
    @PostMapping("/send/{storeId}")
    public AjaxResult sendPrintCommand(@PathVariable String storeId, @RequestBody String printData) {
        log.info("【API请求】发送打印指令到门店[{}]，请求数据：{}", storeId, printData);
        try {
            boolean success = printWebSocketService.sendPrintCommand(storeId, printData);
            AjaxResult result;
            if (success) {
                result = AjaxResult.success("打印指令发送成功");
                log.info("【API响应】发送打印指令到门店[{}]成功，响应：{}", storeId, result);
            } else {
                result = AjaxResult.error("打印指令发送失败，门店可能离线");
                log.warn("【API响应】发送打印指令到门店[{}]失败，响应：{}", storeId, result);
            }
            return result;
        } catch (Exception e) {
            log.error("【API异常】发送打印指令到门店[{}]异常：{}", storeId, e.getMessage(), e);
            AjaxResult result = AjaxResult.error("发送打印指令异常：" + e.getMessage());
            log.error("【API响应】发送打印指令到门店[{}]异常响应：{}", storeId, result);
            return result;
        }
    }

    /**
     * 发送打印指令到所有门店
     *
     * @param printData 打印数据
     * @return 操作结果
     */
    @PostMapping("/sendAll")
    public AjaxResult sendPrintCommandToAll(@RequestBody String printData) {
        log.info("【API请求】发送打印指令到所有门店，请求数据：{}", printData);
        try {
            printWebSocketService.sendPrintCommandToAll(printData);
            AjaxResult result = AjaxResult.success("打印指令已发送到所有门店");
            log.info("【API响应】发送打印指令到所有门店成功，响应：{}", result);
            return result;
        } catch (Exception e) {
            log.error("【API异常】发送打印指令到所有门店异常：{}", e.getMessage(), e);
            AjaxResult result = AjaxResult.error("发送打印指令到所有门店异常：" + e.getMessage());
            log.error("【API响应】发送打印指令到所有门店异常响应：{}", result);
            return result;
        }
    }

    /**
     * 查询门店WebSocket连接状态
     *
     * @param storeId 门店ID
     * @return 连接状态
     */
    @GetMapping("/check/{storeId}")
    public AjaxResult checkStoreConnection(@PathVariable String storeId) {
        log.info("【API请求】查询门店[{}]连接状态", storeId);
        try {
            boolean isOnline = printWebSocketService.checkStoreConnection(storeId);
            AjaxResult result = AjaxResult.success("查询成功", isOnline);
            log.info("【API响应】查询门店[{}]连接状态成功，状态：{}，响应：{}", storeId, isOnline ? "在线" : "离线", result);
            return result;
        } catch (Exception e) {
            log.error("【API异常】查询门店[{}]连接状态异常：{}", storeId, e.getMessage(), e);
            AjaxResult result = AjaxResult.error("查询门店连接状态异常：" + e.getMessage());
            log.error("【API响应】查询门店[{}]连接状态异常响应：{}", storeId, result);
            return result;
        }
    }

    /**
     * 获取当前在线门店数量
     *
     * @return 在线门店数量
     */
    @GetMapping("/onlineCount")
    public AjaxResult getOnlineStoreCount() {
        log.info("【API请求】获取当前在线门店数量");
        try {
            int count = printWebSocketService.getOnlineStoreCount();
            AjaxResult result = AjaxResult.success("查询成功", count);
            log.info("【API响应】获取当前在线门店数量成功，数量：{}，响应：{}", count, result);
            return result;
        } catch (Exception e) {
            log.error("【API异常】获取当前在线门店数量异常：{}", e.getMessage(), e);
            AjaxResult result = AjaxResult.error("查询在线门店数量异常：" + e.getMessage());
            log.error("【API响应】获取当前在线门店数量异常响应：{}", result);
            return result;
        }
    }
}
