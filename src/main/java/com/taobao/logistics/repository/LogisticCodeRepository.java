package com.taobao.logistics.repository;

import com.taobao.logistics.entity.LogisticsCode;

import java.util.Optional;

/**
 * @author ShiShiDaWei
 * @date 2021/8/13
 */

public interface LogisticCodeRepository extends BaseRepository<LogisticsCode, Integer> {


    /**
     * 查询物流编号信息
     * @param name
     * @return
     */
    Optional<LogisticsCode> findByName(String name);


}
