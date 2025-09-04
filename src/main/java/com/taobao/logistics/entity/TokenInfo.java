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
 *
 * @author ShiShiDaWei
 * @date 2021/8/13
 * create table XCX_ACCESS_TOKEN
(
id                       NUMBER(10) not null,
access_token             VARCHAR2(2550),
useful_time              DATE,
mark                     VARCHAR2(2550),
app_id                   VARCHAR2(100),
ad_client_id             NUMBER(10) default 37,
ad_org_id                NUMBER(10) default 27,
ownerid                  NUMBER(10) default 893,
modifierid               NUMBER(10) default 893,
creationdate             DATE default sysdate,
modifieddate             DATE default sysdate,
isactive                 CHAR(1) default 'Y',
jsapi_ticket             VARCHAR2(2550),
jsapi_ticket_useful_time DATE
)
 */
@Data
@Entity
@Table(name = "XCX_ACCESS_TOKEN", schema = "NEANDS3")
public class TokenInfo {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_XCX_ACCESS_TOKEN")
    @SequenceGenerator(sequenceName = "SEQ_XCX_ACCESS_TOKEN", name = "SEQ_XCX_ACCESS_TOKEN", allocationSize = 1)
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

    private String accessToken;
    private String appId;
    private Date usefulTime;
    private String mark;
    private Long taobaoUserId;
    private String taobaoUserNick;
    private String appSecret;
}
