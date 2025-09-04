package com.taobao.logistics.repository;

import com.taobao.logistics.entity.TokenInfo;

import java.util.Optional;

/**
 * @author ShiShiDaWei
 * @date 2021/8/13
 */

public interface TokenInfoRepository extends BaseRepository<TokenInfo, Integer> {

    Optional<TokenInfo> findByAppId(String appKey);


    Optional<TokenInfo> findByAppIdAndTaobaoUserNick(String appKey, String taobaoUserNick);
}
