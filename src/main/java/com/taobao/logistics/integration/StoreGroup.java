package com.taobao.logistics.integration;

import java.util.List;

public class StoreGroup {
    private String store1; // 门店一
    private List<String> store2Options; // 可选的门店二列表

    public StoreGroup(String store1, List<String> store2Options) {
        this.store1 = store1;
        this.store2Options = store2Options;
    }

    // Getter 和 Setter
    public String getStore1() {
        return store1;
    }

    public void setStore1(String store1) {
        this.store1 = store1;
    }

    public List<String> getStore2Options() {
        return store2Options;
    }

    public void setStore2Options(List<String> store2Options) {
        this.store2Options = store2Options;
    }

    @Override
    public String toString() {
        return "StoreGroup{" +
                "store1='" + store1 + '\'' +
                ", store2Options=" + store2Options +
                '}';
    }
}