package com.taobao.logistics.entity.wx;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

/**
 * Created by ShiShiDaWei on 2021/11/1.
 */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Table(name = "T_GOODS_PIC", indexes = {@Index(name = "index_goods_pic_md5", columnList = "goods_pic_md5", unique = true)})
public class GoodsPic {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_T_GOODS_PIC")
    @SequenceGenerator(sequenceName = "SEQ_T_GOODS_PIC", name = "SEQ_T_GOODS_PIC", allocationSize = 1)
    private Integer id;

    @Column(nullable = false)
    private String fileUri;

//    @CreatedDate
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATE default SYSDATE null ", insertable = false, updatable = false)
    private Date createDate;

    @Column(nullable = false)
    private String goodsPicNo;

    @Column(name = "goods_pic_md5", nullable = false)
    private String goodsPicMD5;

//    @Column(columnDefinition = "INT default 0")
//    private Integer count;

    @JsonIgnore
    @ManyToMany(mappedBy = "picList")
    private List<RequisitionOrder> reqOrders;






    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getGoodsPicNo() {
        return goodsPicNo;
    }

    public void setGoodsPicNo(String goodsPicNo) {
        this.goodsPicNo = goodsPicNo;
    }

    public String getGoodsPicMD5() {
        return goodsPicMD5;
    }

    public void setGoodsPicMD5(String goodsPicMD5) {
        this.goodsPicMD5 = goodsPicMD5;
    }

//    public Integer getCount() {
//        return count;
//    }
//
//    public void setCount(Integer count) {
//        this.count = count;
//    }

    @Override
    public String toString() {
        return "GoodsPic{" +
                "id=" + id +
                ", fileUri='" + fileUri + '\'' +
                ", createDate=" + createDate +
                ", goodsPicNo='" + goodsPicNo + '\'' +
                ", goodsPicMD5='" + goodsPicMD5 + '\'' +
//                ", count=" + count +
                '}';
    }
}
