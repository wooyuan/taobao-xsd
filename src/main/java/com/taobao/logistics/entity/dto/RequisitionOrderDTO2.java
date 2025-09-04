package com.taobao.logistics.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by ShiShiDaWei on 2021/11/9.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequisitionOrderDTO2 {

    private String productArea;

    public String getProductArea() {
        return productArea;
    }

    public void setProductArea(String productArea) {
        this.productArea = productArea;
    }

    public RequisitionOrderDTO2(String productArea) {
        this.productArea = productArea;
    }

    public RequisitionOrderDTO2() {
    }
}
