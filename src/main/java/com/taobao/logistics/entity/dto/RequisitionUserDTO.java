package com.taobao.logistics.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ShiShiDaWei on 2021/11/5.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequisitionUserDTO {

    private Integer id;

    private String userid;

    private String mobile;

    private Integer wxStatus;

    private String jobNum;

    private String brand;

    private Integer storeId;

    private String storeName;

    private String storeCode;

    private String name;




}
