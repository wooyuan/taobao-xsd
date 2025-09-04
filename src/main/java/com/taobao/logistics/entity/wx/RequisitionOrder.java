package com.taobao.logistics.entity.wx;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.taobao.logistics.entity.BaseEntity;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * Created by ShiShiDaWei on 2021/11/5.
 */
@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
@Table(name = "T_REQUISITION_ORDER", schema = "NEANDS3")
@NamedEntityGraph(name = "RequisitionOrderEntity",
        attributeNodes = {@NamedAttributeNode("picList")})
public class RequisitionOrder extends BaseEntity {


    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_T_REQUISITION_ORDER")
    @SequenceGenerator(sequenceName = "SEQ_T_REQUISITION_ORDER", name = "SEQ_T_REQUISITION_ORDER", allocationSize = 1)
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

    @Column(name = "req_user_id")
    private Integer reqUserId;

    private Integer storeId;

    /**
     * 条码
     */
    private String productNo;

    /**
     * 商品ID  伯俊外键关联字段
     */
    @Column(name = "M_PRODUCT_ID")
    private Integer mProductId;

    @Column(name = "M_ATTRIBUTESETINSTANCE_ID")
    private Integer mAttributesetinstanceId;

    /**
     * 产地
     */
    private String productArea;

    /**
     * JNR大类
     */
    @NotNull(message = "JNR大类非法")
    @Column(name = "M_DIM12_ID")
    private Integer mDim12Id;

    @NotBlank(message = "JNR大类非法")
    @Column(name = "M_DIM12_Attr")
    private String mDim12Attr;

    /**
     * 供应商编码
     */
    @NotBlank(message = "供应商编码非法")
    private String serialNo;

    /**
     * 原厂编号
     */
    @NotBlank(message = "原厂编号非法")
    private String productCode;

    @Column(name = "M_COLOR")
    private String mColor;

    @Column(name = "M_SIZE")
    private String mSize;

    @NotNull(message = "数量非法")
    @Range(min = 1, max = 9999999, message = "数量过大或者大于0")
    private Integer qty;

    private String remark;

    private String newpicurl;

    private String picUrl;

    //    @JsonIgnoreProperties(value = { "reqOrders" })
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE )
    @JoinTable(name = "ORDER_PIC_MAP", joinColumns = @JoinColumn(name = "req_order_id"),
            inverseJoinColumns = @JoinColumn(name = "goods_pic_id"))
    private List<GoodsPic> picList;

    /**
     * 品牌ID
     */
    @Column(name = "M_DIM1_ID")
    private Integer mDim1Id;

    /**
     * 1未提交 2提交 3待审批
     */
    private Integer status;

    /**
     * 1未提交 2提交 3驳回
     */
    private Integer orderState;

    private String playerMsg;

    @Column(nullable = false)
    private String storeCode;

    /*
     * 门店名称
     * */
    private String storename;

    /**
     * 提交人
     */
    private String username;

    /*
     * 门店提交状态
     * */
    private String mdstatus;


    public Integer getmProductId() {
        return mProductId;
    }

    public void setmProductId(Integer mProductId) {
        this.mProductId = mProductId;
    }

    public Integer getmAttributesetinstanceId() {
        return mAttributesetinstanceId;
    }

    public void setmAttributesetinstanceId(Integer mAttributesetinstanceId) {
        this.mAttributesetinstanceId = mAttributesetinstanceId;
    }

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

    public String getmColor() {
        return mColor;
    }

    public void setmColor(String mColor) {
        this.mColor = mColor;
    }

    public String getmSize() {
        return mSize;
    }

    public void setmSize(String mSize) {
        this.mSize = mSize;
    }

    public Integer getmDim1Id() {
        return mDim1Id;
    }

    public void setmDim1Id(Integer mDim1Id) {
        this.mDim1Id = mDim1Id;
    }

    public String getMdstatus() {
        return mdstatus;
    }

    public void setMdstatus(String mdstatus) {
        this.mdstatus = mdstatus;
    }

    public void setStorename(String storename) {
        this.storename = storename;
    }

    public String getStorename() {
        return storename;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
    public void setNewpicurl(String newpicurl) {
        this.newpicurl = newpicurl;
    }

    public String getNewpicurl() {
        return newpicurl;
    }
}
