package com.taobao.logistics.repository;

import com.taobao.logistics.entity.WarehouseShip;

import java.util.List;
import java.util.Optional;

/**
 * Created by ShiShiDaWei on 2021/9/30.
 */
public interface WarehouseRepository extends BaseRepository<WarehouseShip, Integer> {


    Optional<WarehouseShip> findByWaybillCode(String waybillCode);

    List<WarehouseShip> findByDocNo(String docNo);


}
