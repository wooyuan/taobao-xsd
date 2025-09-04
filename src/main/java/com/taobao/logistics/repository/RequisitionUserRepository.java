package com.taobao.logistics.repository;

import com.taobao.logistics.entity.wx.RequisitionUser;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by ShiShiDaWei on 2021/11/3.
 */
public interface RequisitionUserRepository extends BaseRepository<RequisitionUser, Integer> {


    Optional<RequisitionUser> findByUserid(String userId);

    int countByJobNum(String jobNum);

    RequisitionUser findByJobNum(String jobNum);

    @Query("select distinct r.storeId as storeId, r.storeName as storeName from RequisitionUser r where r.isactive = 'Y' order by r.storeId asc")
    List<Map<String, Object>> findDistinctStoreIdAndStoreName();

    @Query("select r.storeId as storeId, r.storeName as storeName from RequisitionUser r where r.isactive = 'Y' and r.id = ?1 ")
    List<Map<String, Object>> findDistinctStoreIdAndStoreName(int id);
}
