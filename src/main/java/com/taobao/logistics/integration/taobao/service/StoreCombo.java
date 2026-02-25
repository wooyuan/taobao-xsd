package com.taobao.logistics.integration.taobao.service;

public class StoreCombo {
    private String store1; // 门店一
    private String store2; // 门店二

    public StoreCombo() {}

    public StoreCombo(String store1, String store2) {
        this.store1 = store1;
        this.store2 = store2;
    }

    // Getter 和 Setter
    public String getStore1() {
        return store1;
    }

    public void setStore1(String store1) {
        this.store1 = store1;
    }

    public String getStore2() {
        return store2;
    }

    public void setStore2(String store2) {
        this.store2 = store2;
    }

    @Override
    public String toString() {
        return "StoreCombo{" +
                "store1='" + store1 + '\'' +
                ", store2='" + store2 + '\'' +
                '}';
    }
}
