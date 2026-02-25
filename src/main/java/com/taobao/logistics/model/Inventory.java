package com.taobao.logistics.model;

import java.util.Date;

/**
 * 库存信息模型
 */
public class Inventory {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 款号
     */
    private String styleNumber;
    
    /**
     * 条码
     */
    private String barcode;
    
    /**
     * 原厂编号
     */
    private String factoryCode;
    
    /**
     * 颜色
     */
    private String color;
    
    /**
     * 尺寸
     */
    private String size;
    
    /**
     * 门店名称
     */
    private String storeName;
    
    /**
     * 门店地址
     */
    private String storeAddress;
    
    /**
     * 库存数量
     */
    private Integer stockQuantity;
    
    /**
     * 最小库存阈值
     */
    private Integer minStockThreshold;
    
    /**
     * 状态（1-有库存，2-低库存，3-缺货）
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;

    // getter和setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getStyleNumber() {
        return styleNumber;
    }

    public void setStyleNumber(String styleNumber) {
        this.styleNumber = styleNumber;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getFactoryCode() {
        return factoryCode;
    }

    public void setFactoryCode(String factoryCode) {
        this.factoryCode = factoryCode;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getMinStockThreshold() {
        return minStockThreshold;
    }

    public void setMinStockThreshold(Integer minStockThreshold) {
        this.minStockThreshold = minStockThreshold;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}