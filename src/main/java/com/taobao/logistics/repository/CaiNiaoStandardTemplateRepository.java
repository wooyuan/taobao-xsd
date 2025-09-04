package com.taobao.logistics.repository;

import com.taobao.logistics.entity.CaiNiaoStandardTemplate;

import java.util.Optional;

/**
 * Created by ShiShiDaWei on 2021/8/17.
 */
public interface CaiNiaoStandardTemplateRepository extends BaseRepository<CaiNiaoStandardTemplate, Integer> {


    Optional<CaiNiaoStandardTemplate> findByCpCode(String logisticCode);


}
