package com.taobao.logistics.service.wx;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.logistics.config.WorkWxConfig;
import com.taobao.logistics.entity.TokenInfo;
import com.taobao.logistics.entity.dto.RequisitionUserDTO;
import com.taobao.logistics.entity.wx.RequisitionUser;
import com.taobao.logistics.repository.PortalDao;
import com.taobao.logistics.repository.RequisitionUserRepository;
import com.taobao.logistics.repository.TokenInfoRepository;
import com.taobao.logistics.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

/**
 * Created by ShiShiDaWei on 2021/10/27.
 */
@Slf4j
@Service
public class WxApiServices {
    private final static String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?";
    private final static String WX_USERINFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?";
    private final static String WX_GETUSER_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/get?";


    @Autowired
    private TokenInfoRepository tokenInfoRepository;

    @Autowired
    private RequisitionUserRepository requisitionUserRepository;

    @Autowired
    private PortalDao portalDao;


    /**
     * 获取accesstoken
     * @param agentId 应用ID
     * @return
     */
    public String getWxAccessToken(String agentId) {
        //TODO Cache
        Optional<TokenInfo> tokenInfo = tokenInfoRepository.findByAppId(agentId);
        TokenInfo orElseGet = tokenInfo.orElseGet(() -> {
            try {
                TokenInfo saveAndFlush = getTokenInfo();
                return saveAndFlush;
            } catch (IOException e) {
                e.printStackTrace();
                log.error("WxAccessTokenServices getAccessToken ERROR:{}", e.getMessage());
            }

            return new TokenInfo();
        });
        String accessToken = orElseGet.getAccessToken();
        if (StringUtils.hasLength(accessToken)) {
            Date usefulTime = orElseGet.getUsefulTime();
            LocalDateTime localDateTime = usefulTime.toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime();
            if (localDateTime.isBefore(LocalDateTime.now())) {
                try {
                    orElseGet = getTokenInfo();
                    accessToken = orElseGet.getAccessToken();
                } catch (IOException e) {
                    e.printStackTrace();
                    log.error("WxAccessTokenServices Refresh AccessToken ERROR:{}", e.getMessage());
                    return "errMsg请求企业微信accesstoken失败," + orElseGet.getMark();
                }
            }

            return accessToken;
        }else {
            return "errMsg数据不存在";
        }
    }

    /**
     * 获取work_wx 用户信息
     * @param code
     * @param accessToken
     * @return
     */
    public RequisitionUser getWxUserInfo(String code, String accessToken){
        try {
            String request = RequestUtils.request(WX_USERINFO_URL + "access_token=" + accessToken + "&code=" + code,
                    "GET", "", 8000, 10000);
            if (null != request && request.length() != 0) {
                JSONObject jsonObject = JSONObject.parseObject(request);
                int errorCode = jsonObject.getIntValue("errorCode");
                String errmsg = jsonObject.getString("errmsg");
                //OpenId
                if (0 == errorCode && jsonObject.containsKey("UserId")) {
                    String userId = jsonObject.getString("UserId");
                    String user = RequestUtils.request(WX_GETUSER_URL + "access_token=" + accessToken + "&userid=" + userId,
                            "GET", "", 8000, 10000);
                    JSONObject userJson = JSONObject.parseObject(user);
                    int errorCode1 = userJson.getIntValue("errorCode");
                    if (0 == errorCode1) {
                        JSONObject extattr = userJson.getJSONObject("extattr");
                        JSONArray attrs = extattr.getJSONArray("attrs");
                        String jobNum = "";
                        String brand = "";
                        for (int i = 0; i < attrs.size(); i++) {
                            JSONObject item = attrs.getJSONObject(i);
                            String value = item.getString("value");
                            if (item.containsValue("工号")) {
                                jobNum = value;
                            } else if (item.containsValue("品牌")) {
                                brand = value;
                            }
                        }
                        log.debug("WX_GETUSER_URL=={}", user);
                        Optional<RequisitionUser> optionalUser = requisitionUserRepository.findByUserid(userId);
                        if (optionalUser.isPresent()) {
                            return optionalUser.get();
                        }
                        RequisitionUserDTO userDTO = portalDao.getPortalUser(jobNum);
                        RequisitionUser wxUser = new RequisitionUser();
                        wxUser.setBrand(brand);
                        wxUser.setJobNum(jobNum);
                        wxUser.setMobile(userJson.getString("mobile"));
                        wxUser.setUserid(userId);
                        wxUser.setWxStatus(userJson.getInteger("status"));
                        wxUser.setName(userDTO.getName());
                        wxUser.setStoreName(userDTO.getStoreName());
                        wxUser.setStoreId(userDTO.getStoreId());
                        wxUser.setModifieddate(new Date());
                        wxUser.setPlayer(1);
                        wxUser.setStoreCode(userDTO.getStoreCode());
                        return requisitionUserRepository.save(wxUser);

                    }else {
                        log.debug("WX_GETUSER_URL=={}", user);
                        log.error("errmsg:{}", errmsg);
                    }
                }else {
                    log.debug("WX_USERINFO_URL=={}", request);
                      log.error("非企业成员或" + errmsg);
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取访问用户身份错误 {}", e.getMessage());
            return null;
        }
    }

    public RequisitionUserDTO getUser(String jobNum){
        RequisitionUserDTO portalUser = portalDao.getPortalUser(jobNum);
        System.out.println("JSONObject.toJSONString(portalUser) = " + JSONObject.toJSONString(portalUser));
//        RequisitionUserDTO requisitionUser = portalDao.getUser(jobNum);
//        System.out.println("JSONObject.toJSONString(requisitionUser) = " + JSONObject.toJSONString(requisitionUser));
        return portalUser;
    }

    public RequisitionUser saveUser(String jobNum){
        RequisitionUserDTO userDTO = portalDao.getPortalUser(jobNum);
        RequisitionUser wxUser = new RequisitionUser();
        wxUser.setJobNum(jobNum);
        wxUser.setName(userDTO.getName());
        wxUser.setStoreName(userDTO.getStoreName());
        wxUser.setStoreId(userDTO.getStoreId());
        wxUser.setModifieddate(new Date());
        wxUser.setPlayer(1);
        return requisitionUserRepository.save(wxUser);
    }









    private TokenInfo getTokenInfo() throws IOException {
        String doGet = RequestUtils.doGet(TOKEN_URL + "corpid=" + WorkWxConfig.WORK_WX_CORPID + "&corpsecret=" + WorkWxConfig.WORK_WX_SECRET);
        JSONObject jsonObject = JSONObject.parseObject(doGet);
        String errmsg = jsonObject.getString("errmsg");
        String accessToken = jsonObject.getString("access_token");
        Long expiresIn = jsonObject.getLong("expires_in");
        TokenInfo tokenInfo = new TokenInfo();
        if ("ok".equalsIgnoreCase(errmsg)) {
            TokenInfo info = tokenInfo;
            info.setMark(WorkWxConfig.WORK_WX_CORPID);
            LocalDateTime localDateTime = LocalDateTime.now().plusSeconds(expiresIn);
            Date date = Date.from(localDateTime.toInstant(ZoneOffset.of("+8")));
            info.setUsefulTime(date);
            info.setAccessToken(accessToken);
            info.setModifieddate(new Date());
            TokenInfo saveAndFlush = tokenInfoRepository.saveAndFlush(info);
            log.info("WorkWx Accesstoke :{}", JSONObject.toJSONString(saveAndFlush));
            return saveAndFlush;
        }else {
            log.debug("Get WorkWx AccessToken errMsg:{}!", errmsg);
            tokenInfo.setMark(errmsg);
        }
        return tokenInfo;
    }












    public static void main(String[] args) throws IOException {
        // create a LocalTime object
        LocalTime time1
                = LocalTime.parse("19:34:50");

        // create other LocalTime
        LocalTime time2
                = LocalTime.parse("23:14:00");

        // print instances
        System.out.println("LocalTime 1: " + time1);
        System.out.println("LocalTime 2: " + time2);

        // check if LocalTime is before LocalTime
        // using isBefore()
        boolean value = time1.isBefore(time2);

        // print result
        System.out.println("Is LocalTime1 before LocalTime2: "
                + value);


        String json = "{\"errcode\":0,\"errmsg\":\"ok\",\"userid\":\"17755169465\",\"name\":\"何伟\",\"department\":[143],\"position\":\"伯俊系统运维工程师\",\"mobile\":\"17755169465\",\"gender\":\"1\",\"email\":\"\",\"avatar\":\"https://wework.qpic.cn/wwhead/duc2TvpEgSQO4BpE0WZSZ0lZRDDussZbglb10H5t45j6tNQuVdyShgVUZX8EiaOVxRBviacNnBwvk/0\",\"status\":1,\"isleader\":0," +
                "\"extattr\":{\"attrs\":[{\"name\":\"工号\",\"value\":\"00035039\",\"type\":0,\"text\":{\"value\":\"00035039\"}},{\"name\":\"品牌\",\"value\":\"JNR\",\"type\":0,\"text\":{\"value\":\"JNR\"}}]},\"telephone\":\"\",\"enable\":1,\"hide_mobile\":0,\"order\":[0],\"main_department\":143,\"qr_code\":\"https://open.work.weixin.qq.com/wwopen/userQRCode?vcode=vc265a3c51c9ddb30a\",\"alias\":\"信息中心\",\"is_leader_in_dept\":[0],\"thumb_avatar\":\"https://wework.qpic.cn/wwhead/duc2TvpEgSQO4BpE0WZSZ0lZRDDussZbglb10H5t45j6tNQuVdyShgVUZX8EiaOVxRBviacNnBwvk/100\"}";
        JSONObject userJson = JSONObject.parseObject(json);
        JSONObject extattr = userJson.getJSONObject("extattr");
        JSONArray attrs = extattr.getJSONArray("attrs");


    }





}
