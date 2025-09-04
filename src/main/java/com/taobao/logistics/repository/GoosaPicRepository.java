package com.taobao.logistics.repository;

import com.taobao.logistics.entity.wx.GoodsPic;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by ShiShiDaWei on 2021/11/1.
 */
public interface GoosaPicRepository extends BaseRepository<GoodsPic, Integer> {

    GoodsPic findByGoodsPicMD5(String md5);

    int countByGoodsPicMD5(String md5);

    @Query(value = "select count(distinct g.goodsPicMD5) from GoodsPic g where g.goodsPicMD5 = ?1")
    int countDistinctByGoodsPicMD5(String md5);

    @Query("select g from GoodsPic g where g.id in ?1")
    List<GoodsPic> findAllByIds(List<Integer> picIds);


}
