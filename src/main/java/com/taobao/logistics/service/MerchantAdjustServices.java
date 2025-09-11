package com.taobao.logistics.service;

import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.InventoryMerchantAdjustRequest;
import com.taobao.api.response.InventoryMerchantAdjustResponse;
import com.taobao.logistics.config.LogisticsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MerchantAdjustServices {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String URL = "http://gw.api.taobao.com/router/rest";
    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final int MAX_RETRY = 3;


    public void updateInventory() {
        String querySql = "SELECT id, store_id, qty, item_id, sku_id FROM xsd_qty WHERE is_tb IS NULL";
        String updateSql = "UPDATE xsd_qty SET is_tb = ? WHERE id = ?";

        // 获取当前日期
        String currentDate = new SimpleDateFormat(DATE_FORMAT).format(new Date());

        try {
            // 查询需要更新的数据
            List<Map<String, Object>> results = jdbcTemplate.queryForList(querySql);

            for (Map<String, Object> row : results) {
                Long id = ((Number) row.get("id")).longValue();
                String storeId = String.valueOf(row.get("store_id"));
                Long qty = ((Number) row.get("qty")).longValue();
                Long itemId = Long.valueOf(String.valueOf(row.get("item_id")));
                Long skuId =  Long.valueOf(String.valueOf(row.get("sku_id")));

                // 生成订单ID
                String mainOrderId = "checkin_main_" + currentDate + "_" + id;
                String subOrderId = "checkin_sub_" + currentDate + "_" + id;

                // 调用接口更新库存
                boolean success = callInventoryApi(storeId, qty, itemId, skuId, mainOrderId, subOrderId);

                if (success) {
                    // 更新数据库标记
                    jdbcTemplate.update(updateSql, "Y", id);
                    log.info("成功更新库存，ID: {}", id);
                } else {
                    jdbcTemplate.update(updateSql, "E", id);
                    log.error("更新库存失败，ID: {}", id);
                }
            }
        } catch (Exception e) {
            log.error("更新库存过程中发生错误", e);
        }
    }
    public void updateInventorybyid(String id,String storeId, Long qty, Long itemId, Long skuId) {
       // String updateSql = "UPDATE fa_storage SET result_code = ? WHERE id = ?";
        // 获取当前日期
        String currentDate = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        try {
//            SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
//            dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
//            dataSource.setUrl("jdbc:oracle:thin:@10.100.21.151:1521/orcl");
//            dataSource.setUsername("neands3");
//            dataSource.setPassword("abc123");
//            // 创建JdbcTemplate实例
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            // 生成订单ID
            String mainOrderId = "checkin_main_" + currentDate + "_" + id;
            String subOrderId = "checkin_sub_" + currentDate + "_" + id;
                // 调用接口更新库存
                boolean success = callInventoryApi(storeId, qty, itemId, skuId, mainOrderId, subOrderId);

                if (success) {
                    // 更新数据库标记
                    //jdbcTemplate.update(updateSql, "Y", id);
                    log.info("成功更新库存，ID: {}", id);
                } else {
                    //jdbcTemplate.update(updateSql, "E", id);
                    log.error("更新库存失败，ID: {}", id);
                }

        } catch (Exception e) {
            log.error("更新库存过程中发生错误", e);
        }
    }


    private boolean callInventoryApi(String storeId, Long qty, Long itemId, Long skuId,
                                     String mainOrderId, String subOrderId) {
        int retryCount = 0;

        while (retryCount < MAX_RETRY) {
            try {
                TaobaoClient client = new DefaultTaobaoClient(URL,
                        LogisticsConfig.XSDAPP_KEY,
                        LogisticsConfig.XSDAPP_SECRET);

                InventoryMerchantAdjustRequest req = new InventoryMerchantAdjustRequest();
                InventoryMerchantAdjustRequest.InventoryCheckDto inventoryCheck =
                        new InventoryMerchantAdjustRequest.InventoryCheckDto();

                inventoryCheck.setCheckMode(1L); // 1:全量更新 2:出入库盘盈盘亏
                inventoryCheck.setInvStoreType(6L); // 6:门店类型
                inventoryCheck.setStoreCode(storeId);
                inventoryCheck.setOrderId(mainOrderId);

                List<InventoryMerchantAdjustRequest.InventoryCheckDetailDto> details =
                        new ArrayList<>();
                InventoryMerchantAdjustRequest.InventoryCheckDetailDto detail =
                        new InventoryMerchantAdjustRequest.InventoryCheckDetailDto();

                detail.setQuantity(qty);
                detail.setScItemId(itemId);
                detail.setSkuId(skuId);
                detail.setSubOrderId(subOrderId);
                details.add(detail);

                inventoryCheck.setDetailList(details);
                req.setInventoryCheck(inventoryCheck);

                // 使用配置中的sessionKey
                InventoryMerchantAdjustResponse response = client.execute(
                        req, LogisticsConfig.SESSIONKEY);

                if (response.isSuccess()) {
                    return true;
                } else {
                    log.error("API调用失败: {}", response.getBody());
                    retryCount++;
                    Thread.sleep(1000); // 等待1秒后重试
                }
            } catch (ApiException e) {
                log.error("API异常，重试次数: {}", retryCount, e);
                retryCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("线程被中断", e);
                return false;
            }
        }

        return false;
    }

    public static void main(String[] args) {
        // 测试代码
        try {
            MerchantAdjustServices service = new MerchantAdjustServices();
            service.updateInventory();
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }
}