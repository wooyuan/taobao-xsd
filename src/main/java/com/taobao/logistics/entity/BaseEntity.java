package com.taobao.logistics.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by ShiShiDaWei on 2021/11/1.
 */

@MappedSuperclass
public class BaseEntity implements Serializable {

    private static final long SERIAL_VERSION_UID = 1L;


    @Column(name = "ad_client_id", columnDefinition = "NUMBER(10) default 37 null", insertable = false)
    public Long adClientId;
    @Column(name = "ad_org_id", columnDefinition = "NUMBER(10) default 27 null", insertable = false)
    public Long adOrgId;
    @Column(name = "ownerid", columnDefinition = "NUMBER(10) default 893 null", insertable = false)
    public Long ownerid;
    @Column(name = "modifierid", columnDefinition = "NUMBER(10) default 893 null", insertable = false)
    public Long modifierid;

    //    @CreatedDate
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATE default sysdate null ", insertable = false, updatable = false)
    public Date creationdate;

    //    @LastModifiedDate
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATE default sysdate null ", insertable = false)
    public Date modifieddate;

    @Column(columnDefinition = "CHAR(1) default 'Y' null ", insertable = false)
    public String isactive;


    public Long getAdClientId() {
        return adClientId;
    }

    public void setAdClientId(Long adClientId) {
        this.adClientId = adClientId;
    }

    public Long getAdOrgId() {
        return adOrgId;
    }

    public void setAdOrgId(Long adOrgId) {
        this.adOrgId = adOrgId;
    }

    public Long getOwnerid() {
        return ownerid;
    }

    public void setOwnerid(Long ownerid) {
        this.ownerid = ownerid;
    }

    public Long getModifierid() {
        return modifierid;
    }

    public void setModifierid(Long modifierid) {
        this.modifierid = modifierid;
    }

    public Date getCreationdate() {
        return creationdate;
    }

    public void setCreationdate(Date creationdate) {
        this.creationdate = creationdate;
    }

    public Date getModifieddate() {
        return modifieddate;
    }

    public void setModifieddate(Date modifieddate) {
        this.modifieddate = modifieddate;
    }

    public String getIsactive() {
        return isactive;
    }

    public void setIsactive(String isactive) {
        this.isactive = isactive;
    }
}
