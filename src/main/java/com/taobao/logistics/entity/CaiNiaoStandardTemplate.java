package com.taobao.logistics.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by ShiShiDaWei on 2021/8/17.
 */
@Data
@Entity
@Table(name = "M_CAINIAO_TEMPLATE", schema = "NEANDS3")
public class CaiNiaoStandardTemplate {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_M_CAINIAO_TEMPLATE")
    @SequenceGenerator(sequenceName = "SEQ_M_CAINIAO_TEMPLATE", name = "SEQ_M_CAINIAO_TEMPLATE", allocationSize = 1)
    private int id;
    @Column(name = "ad_client_id", columnDefinition = "NUMBER(10) default 37 null")
    public Long adClientId = 37L;
    @Column(name = "ad_org_id", columnDefinition = "number(10) default 27 null")
    public Long adOrgId = 27L;
    @Column(name = "ownerid", columnDefinition = "number(10) default 893 null")
    public Long ownerid = 893L;
    @Column(name = "modifierid", columnDefinition = "number(10) default 893 null")
    public Long modifierid = 893L;
    @Column(columnDefinition = "date default sysdate null ")
    public Date creationdate;
    @Column(columnDefinition = "DATE default sysdate null ")
    public Date modifieddate;
    @Column(columnDefinition = "char(1) default 'Y' null ")
    public String isactive = "Y";

    public String cpCode;
    public Long templateId;
    public String templateName;
    public String templateUrl;
    public Long templateType;



}
