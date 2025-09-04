package com.taobao.logistics.service;

import com.alibaba.fastjson.JSONObject;
import com.taobao.logistics.config.LogisticsConfig;
import com.taobao.logistics.entity.TokenInfo;
import com.taobao.logistics.repository.TokenInfoRepository;
import com.taobao.logistics.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by ShiShiDaWei on 2021/8/16.
 */
@Slf4j
@Service
public class AccessTokenServices {
    private static final String OAUTH_URL = "https://oauth.taobao.com/token";
    private static final String REDIRECT_URL = "http://cainiao.enjoyme.cn:9214/code/findxsd";


    @Autowired
    private TokenInfoRepository tokenInfoRepository;



    public TokenInfo getAccessToken(String code, String storeName, String appKey) {
        //code=OxlukWofLrB1Db1M6aJGF8x2332458&grant_type=authorization_code&client_id=11111111&client_secret=69a1a2a3a469a1469a14a9bf269a14&redirect_uri=http://www.oauth.net/2
        Optional<TokenInfo> tokenInfo;
        TokenInfo get = null;
//        if (LogisticsConfig.APP_FIRST_STORE.equalsIgnoreCase(storeName) && StringUtils.hasLength(appKey)) {
//            tokenInfo = tokenInfoRepository.findByAppIdAndTaobaoUserNick(appKey, storeName);
//        }else {
//            tokenInfo = tokenInfoRepository.findByAppIdAndTaobaoUserNick(appKey, storeName);
//        }
        tokenInfo = tokenInfoRepository.findByAppIdAndTaobaoUserNick(appKey, storeName);
        log.debug("{}获取accesstoken！！！{}", storeName, appKey);
        if (!StringUtils.hasLength(code)) {
            log.error("code为空！！！");
            return tokenInfo.orElse(null);
        }
        log.debug("tokenInfo.isPresent()={}, {}", tokenInfo.isPresent(), JSONObject.toJSONString(tokenInfo));
        if (tokenInfo.isPresent()) {
            get = tokenInfo.get();
            Date usefulTime = get.getUsefulTime();
            String appSecret = get.getAppSecret();
            LocalDateTime usefulTime1 = usefulTime.toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime();
            System.out.println("usefulTime = " + usefulTime1.format(DateTimeFormatter.ofPattern("yyyy_MM_dd HH:mm:ss")));
            log.info("userfulTiem isBefore now ={}", usefulTime1.isBefore(LocalDateTime.now()));
            if (usefulTime1.isBefore(LocalDateTime.now())) {
                log.debug("LocalDateTime.now() = " + LocalDateTime.now());
                int id = get.getId();
                return getTokenInfo(code, id, appKey, appSecret);
            }else {
                return get;
            }

        }else {
            String appSecret = LogisticsConfig.APP_SECRET;
            get = tokenInfo.orElseGet(() -> getTokenInfo(code, null, appKey, appSecret));
        }

        return get;
    }

    private TokenInfo getTokenInfo(String code, Integer id, String appKey, String appSecret) {
        String accessToken;
        Map<String, String> data = new HashMap<>();
        data.put("code", code);
        data.put("grant_type", "authorization_code");
        data.put("client_id", appKey);
        data.put("client_secret", appSecret);
        data.put("redirect_uri", REDIRECT_URL);
        data.put("view", "web");
        StringBuilder stringBuilder = RequestUtils.jointStr(data);
        int indexOf = stringBuilder.lastIndexOf("&");
        stringBuilder = stringBuilder.replace(indexOf, indexOf + 1, "");
        log.debug("stringBuilder.toString()={}", stringBuilder.toString());

        try {
            String doPost = RequestUtils.doFormPost(OAUTH_URL, stringBuilder.toString());
//                String doPost = WebUtils.doPost(OAUTH_URL, data, 3000, 3000);
            System.out.println("doPost = " + doPost);
            log.info("doPost={}", doPost);
            /**
             * {"w1_expires_in":15552000,"refresh_token_valid_time":1629099007907,"taobao_user_nick":"mday%E6%97%97%E8%88%B0%E5%BA%97","re_expires_in":0,"expire_time":1644651007907,"token_type":"Bearer","access_token":"62008075ad4f736ZZf6ffa49ff975b2b7d6a9e4ef8f29ba2207644947909","taobao_open_uid":"AAHa7xgfANpuroaKi4u7Muqf","w1_valid":1644651007907,"refresh_token":"6200407c3401a24ZZ0afd07ca8d389eb506eeb51d62f0a82207644947909","w2_expires_in":15552000,"w2_valid":1644651007907,"r1_expires_in":15552000,"r2_expires_in":15552000,"r2_valid":1644651007907,"r1_valid":1644651007907,"taobao_user_id":"2207644947909","expires_in":15552000}
             */
            if (StringUtils.hasLength(doPost)) {
                JSONObject jsonObject = JSONObject.parseObject(doPost);
                accessToken = jsonObject.getString("access_token");
                Long expiresIn = jsonObject.getLong("expires_in");
                Long taobaoUserId = jsonObject.getLong("taobao_user_id");
                String taobaoUserNick = jsonObject.getString("taobao_user_nick");
                LocalDateTime localDateTime = LocalDateTime.now().plusSeconds(expiresIn);
                java.util.Date date = Date.from(localDateTime.toInstant(ZoneOffset.of("+8")));
                TokenInfo info = new TokenInfo();
                info.setAppId(appKey);
                info.setAppSecret(appSecret);
                info.setAccessToken(accessToken);
                info.setUsefulTime(date);
                info.setMark(String.valueOf(taobaoUserId));
                info.setModifieddate(new Date());
                info.setTaobaoUserId(taobaoUserId);
                info.setTaobaoUserNick(taobaoUserNick.contains("%") ? URLDecoder.decode(taobaoUserNick, "UTF-8") : taobaoUserNick);

                if (id != null) {
                    info.setId(id);
                }
                log.debug("update tokenInfo ={}", JSONObject.toJSONString(info));
                return tokenInfoRepository.saveAndFlush(info);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("请求access_token失败！");
        }
        return null;
    }


    public static void main(String[] args) {
        String json = "{\"w1_expires_in\":15552000,\"refresh_token_valid_time\":1629099007907,\"taobao_user_nick\":\"mday%E6%97%97%E8%88%B0%E5%BA%97\",\"re_expires_in\":0,\"expire_time\":1644651007907,\"token_type\":\"Bearer\",\"access_token\":\"62008075ad4f736ZZf6ffa49ff975b2b7d6a9e4ef8f29ba2207644947909\",\"taobao_open_uid\":\"AAHa7xgfANpuroaKi4u7Muqf\",\"w1_valid\":1644651007907,\"refresh_token\":\"6200407c3401a24ZZ0afd07ca8d389eb506eeb51d62f0a82207644947909\",\"w2_expires_in\":15552000,\"w2_valid\":1644651007907,\"r1_expires_in\":15552000,\"r2_expires_in\":15552000,\"r2_valid\":1644651007907,\"r1_valid\":1644651007907,\"taobao_user_id\":\"2207644947909\",\"expires_in\":15552000}";
        System.out.println("JSONObject.parseObject(json) = " + JSONObject.parseObject(json).toJSONString());
        DateTimeFormatter ofPattern = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime parse = LocalDateTime.parse("2022/02/26 10:33:00", ofPattern);

        System.out.println( parse.isBefore(LocalDateTime.now()));

    }

}
