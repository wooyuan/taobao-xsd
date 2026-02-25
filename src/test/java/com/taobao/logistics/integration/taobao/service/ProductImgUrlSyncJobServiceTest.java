package com.taobao.logistics.integration.taobao.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ProductImgUrlSyncJobService测试类
 */
@Slf4j
@SpringBootTest
public class ProductImgUrlSyncJobServiceTest {

    @Autowired
    private ProductImgUrlSyncJobService productImgUrlSyncJobService;

    /**
     * 测试图片URL同步和检查功能
     */
    @Test
    public void testSyncProductImgUrl() {
        log.info("开始测试产品图片URL同步任务");
        long startTime = System.currentTimeMillis();
        
        try {
            // 手动调用同步方法进行测试
            productImgUrlSyncJobService.syncProductImgUrl();
            
            long endTime = System.currentTimeMillis();
            log.info("产品图片URL同步任务测试完成，耗时: {}ms", (endTime - startTime));
        } catch (Exception e) {
            log.error("测试失败", e);
            throw e;
        }
    }
}
