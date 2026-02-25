package com.taobao.logistics.service.impl;

import com.taobao.logistics.model.Inventory;
import com.taobao.logistics.service.InventoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存服务实现类
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    // 模拟库存数据
    private static final List<Inventory> MOCK_INVENTORY_DATA = new ArrayList<>();
    
    static {
        // 初始化一些模拟数据
        MOCK_INVENTORY_DATA.add(new Inventory() {{
            setId(1L);
            setProductName("男士休闲T恤");
            setStyleNumber("TX001");
            setBarcode("6901234567890");
            setFactoryCode("FC001");
            setColor("白色");
            setSize("L");
            setStoreName("北京朝阳店");
            setStoreAddress("北京市朝阳区三里屯路1号");
            setStockQuantity(15);
            setMinStockThreshold(5);
            setStatus(1);
            setCreateTime(new Date());
            setUpdateTime(new Date());
        }});
        
        MOCK_INVENTORY_DATA.add(new Inventory() {{
            setId(2L);
            setProductName("女士连衣裙");
            setStyleNumber("DQ002");
            setBarcode("6901234567891");
            setFactoryCode("FC002");
            setColor("红色");
            setSize("M");
            setStoreName("上海南京路店");
            setStoreAddress("上海市黄浦区南京东路100号");
            setStockQuantity(3);
            setMinStockThreshold(5);
            setStatus(2);
            setCreateTime(new Date());
            setUpdateTime(new Date());
        }});
        
        MOCK_INVENTORY_DATA.add(new Inventory() {{
            setId(3L);
            setProductName("运动鞋");
            setStyleNumber("SX003");
            setBarcode("6901234567892");
            setFactoryCode("FC003");
            setColor("黑色");
            setSize("42");
            setStoreName("深圳华强北店");
            setStoreAddress("深圳市福田区华强北路200号");
            setStockQuantity(0);
            setMinStockThreshold(10);
            setStatus(3);
            setCreateTime(new Date());
            setUpdateTime(new Date());
        }});
        
        MOCK_INVENTORY_DATA.add(new Inventory() {{
            setId(4L);
            setProductName("男士休闲T恤");
            setStyleNumber("TX001");
            setBarcode("6901234567893");
            setFactoryCode("FC001");
            setColor("蓝色");
            setSize("M");
            setStoreName("广州天河店");
            setStoreAddress("广州市天河区天河路300号");
            setStockQuantity(8);
            setMinStockThreshold(5);
            setStatus(1);
            setCreateTime(new Date());
            setUpdateTime(new Date());
        }});
        
        MOCK_INVENTORY_DATA.add(new Inventory() {{
            setId(5L);
            setProductName("女士牛仔裤");
            setStyleNumber("NZ004");
            setBarcode("6901234567894");
            setFactoryCode("FC004");
            setColor("蓝色");
            setSize("S");
            setStoreName("杭州西湖店");
            setStoreAddress("杭州市西湖区西湖路100号");
            setStockQuantity(12);
            setMinStockThreshold(3);
            setStatus(1);
            setCreateTime(new Date());
            setUpdateTime(new Date());
        }});
        
        MOCK_INVENTORY_DATA.add(new Inventory() {{
            setId(6L);
            setProductName("运动鞋");
            setStyleNumber("SX003");
            setBarcode("6901234567895");
            setFactoryCode("FC003");
            setColor("白色");
            setSize("40");
            setStoreName("成都春熙路店");
            setStoreAddress("成都市锦江区春熙路200号");
            setStockQuantity(6);
            setMinStockThreshold(10);
            setStatus(2);
            setCreateTime(new Date());
            setUpdateTime(new Date());
        }});
        
        MOCK_INVENTORY_DATA.add(new Inventory() {{
            setId(7L);
            setProductName("男士衬衫");
            setStyleNumber("CS005");
            setBarcode("6901234567896");
            setFactoryCode("FC005");
            setColor("白色");
            setSize("XL");
            setStoreName("西安钟楼店");
            setStoreAddress("西安市莲湖区钟鼓楼广场100号");
            setStockQuantity(20);
            setMinStockThreshold(5);
            setStatus(1);
            setCreateTime(new Date());
            setUpdateTime(new Date());
        }});
        
        MOCK_INVENTORY_DATA.add(new Inventory() {{
            setId(8L);
            setProductName("女士连衣裙");
            setStyleNumber("DQ002");
            setBarcode("6901234567897");
            setFactoryCode("FC002");
            setColor("黑色");
            setSize("L");
            setStoreName("南京新街口店");
            setStoreAddress("南京市玄武区新街口100号");
            setStockQuantity(7);
            setMinStockThreshold(5);
            setStatus(1);
            setCreateTime(new Date());
            setUpdateTime(new Date());
        }});
    }

    @Override
    public List<Inventory> queryInventory(String styleNumber, String barcode, String factoryCode) {
        List<Inventory> filteredData = MOCK_INVENTORY_DATA;
        
        // 根据查询条件过滤数据
        if (styleNumber != null && !styleNumber.trim().isEmpty()) {
            filteredData = filteredData.stream()
                .filter(item -> item.getStyleNumber() != null && 
                               item.getStyleNumber().toLowerCase().contains(styleNumber.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (barcode != null && !barcode.trim().isEmpty()) {
            filteredData = filteredData.stream()
                .filter(item -> item.getBarcode() != null && 
                               item.getBarcode().contains(barcode))
                .collect(Collectors.toList());
        }
        
        if (factoryCode != null && !factoryCode.trim().isEmpty()) {
            filteredData = filteredData.stream()
                .filter(item -> item.getFactoryCode() != null && 
                               item.getFactoryCode().toLowerCase().contains(factoryCode.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return filteredData;
    }

    @Override
    public Inventory getInventoryById(Long id) {
        return MOCK_INVENTORY_DATA.stream()
            .filter(item -> id.equals(item.getId()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public int updateInventory(Inventory inventory) {
        for (int i = 0; i < MOCK_INVENTORY_DATA.size(); i++) {
            if (inventory.getId().equals(MOCK_INVENTORY_DATA.get(i).getId())) {
                Inventory existingInventory = MOCK_INVENTORY_DATA.get(i);
                
                // 更新库存数量
                if (inventory.getStockQuantity() != null) {
                    existingInventory.setStockQuantity(inventory.getStockQuantity());
                    
                    // 更新状态
                    if (inventory.getStockQuantity() <= 0) {
                        existingInventory.setStatus(3); // 缺货
                    } else if (inventory.getStockQuantity() <= existingInventory.getMinStockThreshold()) {
                        existingInventory.setStatus(2); // 低库存
                    } else {
                        existingInventory.setStatus(1); // 有库存
                    }
                }
                
                // 更新其他字段
                if (inventory.getProductName() != null) {
                    existingInventory.setProductName(inventory.getProductName());
                }
                if (inventory.getStyleNumber() != null) {
                    existingInventory.setStyleNumber(inventory.getStyleNumber());
                }
                if (inventory.getBarcode() != null) {
                    existingInventory.setBarcode(inventory.getBarcode());
                }
                if (inventory.getFactoryCode() != null) {
                    existingInventory.setFactoryCode(inventory.getFactoryCode());
                }
                if (inventory.getColor() != null) {
                    existingInventory.setColor(inventory.getColor());
                }
                if (inventory.getSize() != null) {
                    existingInventory.setSize(inventory.getSize());
                }
                if (inventory.getStoreName() != null) {
                    existingInventory.setStoreName(inventory.getStoreName());
                }
                if (inventory.getStoreAddress() != null) {
                    existingInventory.setStoreAddress(inventory.getStoreAddress());
                }
                
                existingInventory.setUpdateTime(new Date());
                
                return 1; // 更新成功
            }
        }
        
        return 0; // 更新失败
    }
}