package com.taobao.logistics.entity.wx;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.taobao.logistics.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * Created by ShiShiDaWei on 2021/11/3.
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Table(name = "T_REQUISITION_USER", schema = "NEANDS3")
public class RequisitionUser extends BaseEntity {


    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_T_REQUISITION_USER")
    @SequenceGenerator(sequenceName = "SEQ_T_REQUISITION_USER", name = "SEQ_T_REQUISITION_USER", allocationSize = 1)
    private Integer id;

//    @Column(name = "ad_client_id", columnDefinition = "NUMBER(10) default 37 null", insertable = false)
//    public Long adClientId;
//    @Column(name = "ad_org_id", columnDefinition = "NUMBER(10) default 27 null", insertable = false)
//    public Long adOrgId;
//    @Column(name = "ownerid", columnDefinition = "NUMBER(10) default 893 null", insertable = false)
//    public Long ownerid;
//    @Column(name = "modifierid", columnDefinition = "NUMBER(10) default 893 null", insertable = false)
//    public Long modifierid;
//
//
////    @CreatedDate
//    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
//    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
//    @Column(columnDefinition = "DATE default sysdate null ", insertable = false, updatable = false)
//    public Date creationdate;
//
////    @LastModifiedDate
//    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
//    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
//    @Column(columnDefinition = "DATE default sysdate null ", insertable = false)
//    public Date modifieddate;
//
//    @Column(columnDefinition = "CHAR(1) default 'Y' null ", insertable = false)
//    public String isactive;

    private String userid;

    private String mobile;

    /**
     * 激活状态: 1=已激活，2=已禁用，4=未激活，5=退出企业。
     * 已激活代表已激活企业微信或已关注微工作台（原企业号）。
     * 未激活代表既未激活企业微信又未关注微工作台（原企业号）
     */
    private Integer wxStatus;

    @Column(nullable = false)
    private String jobNum;

    private String brand;

    private Integer storeId;

    private String storeName;

    private String name;

    /**
     * 1 申请人  2 审批人
     */
    @Column(columnDefinition = "NUMBER(10) default 1", nullable = false, insertable = false)
    private Integer player;

    private String storeCode;



}
