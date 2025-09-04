package com.taobao.logistics.repository;

import com.taobao.logistics.entity.dto.RequisitionOrderDTO2;
import com.taobao.logistics.entity.wx.RequisitionOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by ShiShiDaWei on 2021/11/5.
 */
//@Transactional(rollbackOn = Exception.class)
public interface RequisitionOrderRepository extends BaseRepository<RequisitionOrder, Integer> {

    @EntityGraph("RequisitionOrderEntity")
    @Query(value = "select r from RequisitionOrder r where r.reqUserId = :userId and r.orderState = :orderState and r.isactive = 'Y' order by r.modifieddate desc")
    List<RequisitionOrder> findAllByReqUserIdAndOrderState(@Param("userId") int userId, @Param("orderState") int orderState);


    @EntityGraph("RequisitionOrderEntity")
    List<RequisitionOrder> findAllByReqUserIdAndOrderStateAndIsactive(int userId, int orderState, String isactive, Pageable pageable);


//    @EntityGraph("RequisitionOrderEntity")//HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!
    @Query("select r from RequisitionOrder r where r.reqUserId = ?1 and r.orderState = ?2 and r.mDim12Id = ?3 and r.storeId = ?4 " +
            "and r.productArea = ?5 and r.isactive = ?6 and r.creationdate >= ?7 and r.creationdate <= ?8 and r.status =?9")
    List<RequisitionOrder> findAllByReqUserIdAndIsactive(int reqUserId, int orderState, int mDim12Id, int storeId, String ares, String isactive, Date startTime, Date endTime, int status, Pageable pageable);



    @Query("select count(r) from RequisitionOrder r where r.reqUserId = ?1 and r.orderState = ?2 and r.mDim12Id = ?3 and r.storeId = ?4 " +
            "and r.productArea = ?5 and r.isactive = ?6 and r.creationdate >= ?7 and r.creationdate <= ?8 and r.status =?9")
    Integer countByReqUserIdAndIsactive(int reqUserId, int orderState, int mDim12Id, int storeId, String ares, String isactive, Date startTime, Date endTime, int status);


    @Query("select r from RequisitionOrder r where r.orderState = ?1 and r.mDim12Id = ?2 and r.storeId = ?3  " +
            "and r.productArea = ?4 and r.isactive = ?5 and r.creationdate >= ?6 and r.creationdate <= ?7 and r.status =?8")
    List<RequisitionOrder> findAllByReqUserIdAndIsactive(int orderState, int mDim12Id, int storeId, String ares, String isactive, Date startTime, Date endTime, int status, Pageable pageable);

    @Query("select count(r) from RequisitionOrder r where r.orderState = ?1 and r.mDim12Id = ?2 and r.storeId = ?3 " +
            "and r.productArea = ?4 and r.isactive = ?5 and r.creationdate >= ?6 and r.creationdate <= ?7 and r.status =?8")
    Integer countByReqUserIdAndIsactive(int orderState, int mDim12Id, int storeId, String ares, String isactive, Date startTime, Date endTime, int status);


    @Query("select r from RequisitionOrder r where r.orderState = ?1 and r.mDim12Id = ?2 " +
            "and r.productArea = ?3 and r.isactive = ?4 and r.creationdate >= ?5 and r.creationdate <= ?6 and r.status =?7")
    List<RequisitionOrder> findAllByReqUserIdAndIsactive(int orderState, int mDim12Id, String ares, String isactive, Date startTime, Date endTime, int status, Pageable pageable);

    @Query("select count(r) from RequisitionOrder r where r.orderState = ?1 and r.mDim12Id = ?2 " +
            "and r.productArea = ?3 and r.isactive = ?4 and r.creationdate >= ?5 and r.creationdate <= ?6 and r.status =?7")
    Integer countByReqUserIdAndIsactive(int orderState, int mDim12Id, String ares, String isactive, Date startTime, Date endTime, int status);


    @Query(value = "select r.id from T_REQUISITION_ORDER r where r.req_user_id = ?1 and r.order_state = ?2", nativeQuery = true)
    List<Integer> findAllByReqUserIdAndIsactive(int reqUserId, int orderState);


    @Query(value = "select distinct r.mDim12Id as mDim12Id, trim(r.mDim12Attr)  as mDim12Attr from RequisitionOrder r " +
            "where r.isactive = 'Y' and r.mDim12Id is not null order by r.mDim12Id")
//    @Query(value = "select distinct t.m_dim12_id as mDim12Id, t.m_dim12_attr as mDim12Attr from #{#entityName} t order by t.m_dim12_id desc", nativeQuery = true)
    List<Map<String, Object>> findDistinctMDim12Id();

    @Query(value = "select distinct r.mDim12Id as mDim12Id, trim(r.mDim12Attr)  as mDim12Attr from RequisitionOrder r " +
            "where r.isactive = 'Y' and r.mDim12Id is not null and r.reqUserId = ?1 order by r.mDim12Id")
    List<Map<String, Object>> findDistinctMDim12Id(int reqUserId);


    @Query("select distinct new com.taobao.logistics.entity.dto.RequisitionOrderDTO2(trim(r.productArea)) from RequisitionOrder r " +
            "where r.isactive = 'Y' and r.productArea is not null")
    List<RequisitionOrderDTO2> findDistinctProductArea();

    //    需要设置别名
    @Query("select distinct r.productArea as productArea from RequisitionOrder r " +
            "where r.isactive = 'Y' and r.productArea is not null and r.reqUserId = ?1")
    List<Map<String, Object>> findDistinctProductArea(int reqUserId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query("update RequisitionOrder r set r.orderState = ?1 where r.id = ?2 and r.isactive = 'Y'")
    void updateById(int orderState, int orderId);


    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query("update RequisitionOrder r set r.orderState = :orderState, r.modifieddate = :modifyTime where r.id in :ids and r.isactive = 'Y'")
    void updateById(@Param(value = "orderState") int orderState, @Param(value = "modifyTime") Date modifyTime, @Param(value = "ids") List<Integer> ids);


    @Modifying
    @Transactional(rollbackOn = Exception.class)//update、delete、insert操作需要开启事务
    @Query("update RequisitionOrder r set r.isactive = ?1, r.modifieddate = ?2 where r.id = ?3 and r.isactive = 'Y'")
    void updateByIdAndIsactive(String isactive, Date modifyTime, Integer orderId);


    int countByReqUserIdAndOrderStateAndIsactive(int reqUserId, int orderState, String isactive);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query("update RequisitionOrder r set r.playerMsg = ?2, r.status = 2  where r.id = ?1")
    void updateMessageById(Integer reqId, String message );

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query("update RequisitionOrder r set r.playerMsg = ?2, r.status = 2 , r.newpicurl = ?3 where r.id = ?1")
    void updateMessageById2(Integer reqId, String message ,String picUrl);

    //更新收货状态
    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query("update RequisitionOrder r set r.mdstatus = ?2 where r.id = ?1")
    void updateMdstatusById(Integer reqId, String mdstatus);


//    //更新图片地址
//    @Modifying
//    @Transactional(rollbackOn = Exception.class)
//    @Query("update RequisitionOrder r set r.pic_url = ?2 where r.id = ?1")
//    void updatePicurlById(Integer reqId, String pic_url);


}
