package com.taobao.logistics.controller;

import com.taobao.logistics.model.Inventory;
import com.taobao.logistics.service.InventoryService;
import com.taobao.logistics.utils.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存查询Controller
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    /**
     * 查询库存信息
     * @param styleNumber 款号
     * @param barcode 条码
     * @param factoryCode 原厂编号
     * @return 库存信息列表
     */
    @GetMapping("/query")
    public AjaxResult queryInventory(
            @RequestParam(required = false) String styleNumber,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String factoryCode) {
        
        try {
            List<Inventory> inventoryList = inventoryService.queryInventory(styleNumber, barcode, factoryCode);
            return AjaxResult.success(inventoryList);
        } catch (Exception e) {
            return AjaxResult.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询库存详情
     * @param id 库存ID
     * @return 库存详情
     */
    @GetMapping("/info/{id}")
    public AjaxResult getInventoryById(@PathVariable Long id) {
        try {
            Inventory inventory = inventoryService.getInventoryById(id);
            return AjaxResult.success(inventory);
        } catch (Exception e) {
            return AjaxResult.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新库存数量
     * @param inventory 库存信息
     * @return 结果
     */
    @PutMapping("/update")
    public AjaxResult updateInventory(@RequestBody Inventory inventory) {
        try {
            int rows = inventoryService.updateInventory(inventory);
            if (rows > 0) {
                return AjaxResult.success("更新成功");
            }
            return AjaxResult.error("更新失败");
        } catch (Exception e) {
            return AjaxResult.error("更新失败：" + e.getMessage());
        }
    }
}