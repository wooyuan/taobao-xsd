package com.taobao.logistics.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommonResult {

    private int code;

    private String errMsg;

    private Map<String, Object> data;

    public CommonResult(int code, String message) {
        this.code = code;
        this.errMsg = message;
        this.data = new HashMap<>();
    }

    public <T> CommonResult withData(String key, T value) {
        data.put(key, value);
        return this;
    }
}
