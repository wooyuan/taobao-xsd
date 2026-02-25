package com.taobao.logistics.service;

import com.taobao.logistics.model.Inventory;

import java.util.List;

/**
 * 库存服务接口
 */
public interface InventoryService {

    /**
     * 查询库存信息
     * @param styleNumber 款号
     * @param barcode 条码
     * @param factoryCode 原厂编号
     * @return 库存信息列表
     */
    List<Inventory> queryInventory(String styleNumber, String barcode, String factoryCode);

    /**
     * 根据ID查询库存详情
     * @param id 库存ID
     * @return 库存详情
     */
    Inventory getInventoryById(Long id);

    /**
     * 更新库存数量
     * @param inventory 库存信息
     * @return 影响行数
     */
    int updateInventory(Inventory inventory);
}