package com.taobao.logistics.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by ShiShiDaWei on 2021/11/9.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequisitionOrderDTO {


    private Integer mDim12Id;

    private String mDim12Attr;




    public Integer getmDim12Id() {
        return mDim12Id;
    }

    public void setmDim12Id(Integer mDim12Id) {
        this.mDim12Id = mDim12Id;
    }

    public String getmDim12Attr() {
        return mDim12Attr;
    }

    public void setmDim12Attr(String mDim12Attr) {
        this.mDim12Attr = mDim12Attr;
    }



    public RequisitionOrderDTO(Integer mDim12Id, String mDim12Attr) {
        this.mDim12Id = mDim12Id;
        this.mDim12Attr = mDim12Attr;
    }


    public RequisitionOrderDTO() {
    }
}
