package com.taobao.logistics.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.CainiaoCloudprintStdtemplatesGetRequest;
import com.taobao.api.request.CainiaoWaybillIiGetRequest;
import com.taobao.api.request.CainiaoWaybillIiLogisticsdetailUrlGetRequest;
import com.taobao.api.request.CainiaoWaybillIiSearchRequest;
import com.taobao.api.response.CainiaoCloudprintStdtemplatesGetResponse;
import com.taobao.api.response.CainiaoWaybillIiGetResponse;
import com.taobao.api.response.CainiaoWaybillIiLogisticsdetailUrlGetResponse;
import com.taobao.api.response.CainiaoWaybillIiSearchResponse;
import com.taobao.logistics.config.LogisticsConfig;
import com.taobao.logistics.entity.Address;
import com.taobao.logistics.entity.CaiNiaoStandardTemplate;
import com.taobao.logistics.entity.PrintView;
import com.taobao.logistics.entity.TokenInfo;
import com.taobao.logistics.entity.WarehouseShip;
import com.taobao.logistics.repository.CaiNiaoStandardTemplateRepository;
import com.taobao.logistics.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ShiShiDaWei on 2021/8/16.
 */
@Slf4j
@Service
public class OrderWaybillServices {


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CaiNiaoStandardTemplateRepository caiNiaoStandardTemplateRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;


    /**
     * 获取云打印单号
     * @param logisticCode
     * @param branchCode
     * @param orderId
     * @param sessionKey
     * @param taobaoUserId
     * @return
     * @throws ApiException
     */
    public String getWaybill(String logisticCode, String branchCode, String senderPhone, int orderId, String sessionKey, Long taobaoUserId) throws ApiException {
        String url = "http://gw.api.taobao.com/router/rest";
        String appkey = LogisticsConfig.APP_KEY;
        String secret = LogisticsConfig.APP_SECRET;
        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
        CainiaoWaybillIiGetRequest req = new CainiaoWaybillIiGetRequest();
        CainiaoWaybillIiGetRequest.WaybillCloudPrintApplyNewRequest waybillCloudPrintApplyNewRequest = new CainiaoWaybillIiGetRequest.WaybillCloudPrintApplyNewRequest();
        waybillCloudPrintApplyNewRequest.setCpCode(logisticCode);
        String sql = "SELECT m.*, c.name store_name, c.id store_id from M_TBRETAIL m, c_store c WHERE m.id = ? AND m.c_store_id = c.id";
        Map<String, Object> queryForMap;
        try {queryForMap=
            queryForMap = jdbcTemplate.queryForMap(sql, orderId);
            log.debug("getWaybill queryForMap={}", JSONObject.toJSONString(queryForMap));
        } catch (DataAccessException e) {
            e.printStackTrace();
            return "-1";
        }
        String name = (String) queryForMap.get("store_name");
        long storeId = ((BigDecimal) queryForMap.get("store_id")).longValue();
        CainiaoWaybillIiGetRequest.AddressDto addressDto = new CainiaoWaybillIiGetRequest.AddressDto();
        CainiaoWaybillIiSearchResponse.AddressDto responseAddress = jdbcTemplate.queryForObject(
                "SELECT c.id, c.province, c.city, c.distric, NVL(c.town, '') town, c.detail, c.name, c.phone from C_CNADDRESS c WHERE c.c_store_id = ? AND ROWNUM <= 1", (resultSet, i) -> {
                    CainiaoWaybillIiSearchResponse.AddressDto as = new CainiaoWaybillIiSearchResponse.AddressDto();
                    as.setProvince(resultSet.getString(2));
                    as.setCity(resultSet.getString(3));
                    as.setDistrict(resultSet.getString(4));
                    as.setTown(resultSet.getString(5));
                    as.setDetail(resultSet.getString(6));
                    return as;
                }, storeId);
        log.debug("responseAddress={}", JSONObject.toJSONString(responseAddress));
//        CainiaoWaybillIiSearchResponse.AddressDto responseAddress = getSenderInfo(appkey, secret, sessionKey, logisticCode, branchCode, name);
//        if (null == responseAddress) {
//            return "";
//        }
        addressDto.setCity(responseAddress.getCity());
        addressDto.setDetail(responseAddress.getDetail());
        addressDto.setDistrict(responseAddress.getDistrict());
        addressDto.setProvince(responseAddress.getProvince());
        addressDto.setTown(responseAddress.getTown());

        CainiaoWaybillIiGetRequest.UserInfoDto userInfoDto = new CainiaoWaybillIiGetRequest.UserInfoDto();
        userInfoDto.setAddress(addressDto);
//        userInfoDto.setMobile("");
//        userInfoDto.setName((String) queryForMap.get("RECEIVER_NAME"));
        userInfoDto.setMobile(senderPhone);
        userInfoDto.setName(name);
//        userInfoDto.setPhone("057123222");
        waybillCloudPrintApplyNewRequest.setSender(userInfoDto);
        List<CainiaoWaybillIiGetRequest.TradeOrderInfoDto> list5 = new ArrayList<>();
        CainiaoWaybillIiGetRequest.TradeOrderInfoDto tradeOrderInfoDto = new CainiaoWaybillIiGetRequest.TradeOrderInfoDto();
        list5.add(tradeOrderInfoDto);
//        tradeOrderInfoDto.setLogisticsServices("如不需要特殊服务，该值为空");
        //与业务字段无关，在批量调用时，需要保证每个对象的objectid不同，在获取到返回值后，可以通过比对出参中的objectId,可以得到与入参的对应关系
        tradeOrderInfoDto.setObjectId(String.valueOf(LocalDateTime.now().getSecond()));
        CainiaoWaybillIiGetRequest.OrderInfoDto orderInfoDto = new CainiaoWaybillIiGetRequest.OrderInfoDto();
        //渠道类型
        String platformType = (String) queryForMap.get("platform_type");
        String type = getOrderchannelsType(platformType);
        orderInfoDto.setOrderChannelsType(type);
        orderInfoDto.setTradeOrderList(Collections.singletonList((String) queryForMap.get("ORDER_NO")));
        orderInfoDto.setOutTradeOrderList(Collections.singletonList((String) queryForMap.get("DOCNO")));
//        orderInfoDto.setOutTradeSubOrderList("12,34,56,78");
        tradeOrderInfoDto.setOrderInfo(orderInfoDto);
        CainiaoWaybillIiGetRequest.PackageInfoDto packageInfoDto = new CainiaoWaybillIiGetRequest.PackageInfoDto();
        //包裹id，用于拆合单场景
        packageInfoDto.setId((String) queryForMap.get("DOCNO"));
        List<CainiaoWaybillIiGetRequest.Item> list12 = new ArrayList<>();
        CainiaoWaybillIiGetRequest.Item item = new CainiaoWaybillIiGetRequest.Item();
        list12.add(item);
        BigDecimal bigDecimal = (BigDecimal) queryForMap.get("TOT_QTY");
        item.setCount(bigDecimal.longValue());
        item.setName("服装日用百货");
        packageInfoDto.setItems(list12);
//        packageInfoDto.setVolume(1L);
//        packageInfoDto.setWeight(1L);
//        packageInfoDto.setTotalPackagesCount(10L);
//        packageInfoDto.setPackagingDescription("5纸3木2拖");
        packageInfoDto.setGoodsDescription("服装");
        tradeOrderInfoDto.setPackageInfo(packageInfoDto);
        CainiaoWaybillIiGetRequest.RecipientInfoDto recipientInfoDto = new CainiaoWaybillIiGetRequest.RecipientInfoDto();
        CainiaoWaybillIiGetRequest.AddressDto addressDto1 = new CainiaoWaybillIiGetRequest.AddressDto();
        addressDto1.setCity((String) queryForMap.get("CITY"));
        addressDto1.setDetail((String) queryForMap.get("ADDRESS"));
        addressDto1.setDistrict((String) queryForMap.get("RECEIVAREA"));
        addressDto1.setProvince((String) queryForMap.get("PROVINCE"));
//        addressDto1.setTown("望京街道");
        recipientInfoDto.setAddress(addressDto1);
        recipientInfoDto.setMobile((String) queryForMap.get("MOBILE"));
        recipientInfoDto.setName((String) queryForMap.get("RECEIVER_NAME"));
//        recipientInfoDto.setPhone("057123222");
        recipientInfoDto.setOaid((String) queryForMap.get("OAID"));
        recipientInfoDto.setTid((String) queryForMap.get("ORDER_NO"));
//        recipientInfoDto.setCaid("As268woscee");
        tradeOrderInfoDto.setRecipient(recipientInfoDto);
        String stdtemplates = getCainiaoCloudprintStdtemplates(logisticCode);
        log.debug("获取打印模板是={}",stdtemplates);
        tradeOrderInfoDto.setTemplateUrl(stdtemplates);
        tradeOrderInfoDto.setUserId(taobaoUserId);
        waybillCloudPrintApplyNewRequest.setTradeOrderInfoDtos(list5);
        log.debug("获取月结账号是"+branchCode);

        //添加月结账号
        if(branchCode.equals("021K1783756")) {
            waybillCloudPrintApplyNewRequest.setCustomerCode(branchCode);
            //https://support-cnkuaidi.taobao.com/doc.htm#?docId=121888&docType=1
            waybillCloudPrintApplyNewRequest.setProductCode("ed-m-0001");
        }

//        waybillCloudPrintApplyNewRequest.setStoreCode("553323");
//        waybillCloudPrintApplyNewRequest.setResourceCode("DISTRIBUTOR_978324");
//        waybillCloudPrintApplyNewRequest.setDmsSorting(false);
//        waybillCloudPrintApplyNewRequest.setThreePlTiming(false);
//        waybillCloudPrintApplyNewRequest.setNeedEncrypt(false);
//        waybillCloudPrintApplyNewRequest.setMultiPackagesShipment(false);
        req.setParamWaybillCloudPrintApplyNewRequest(waybillCloudPrintApplyNewRequest);
        CainiaoWaybillIiGetResponse response = client.execute(req, sessionKey);
        //System.out.println(response.getBody());
       log.debug("订单打印配置是2CainiaoWaybillIiGetResponse ={}", JSONObject.toJSONString(response));
        if (response.isSuccess()) {
            response.getBody();
            Optional<CainiaoWaybillIiGetResponse.WaybillCloudPrintResponse> first = response.getModules().stream().findFirst();
            if (first.isPresent()) {
                CainiaoWaybillIiGetResponse.WaybillCloudPrintResponse waybillCloudPrintResponse = first.get();
                String waybillCode = waybillCloudPrintResponse.getWaybillCode();
                String printData = waybillCloudPrintResponse.getPrintData();
                JSONObject jsonObject = JSONObject.parseObject(printData);
                String templateURL = jsonObject.getString("templateURL");
                JSONObject data = jsonObject.getJSONObject("data");
                JSONObject recipient = data.getJSONObject("recipient");
                JSONObject routingInfo = data.getJSONObject("routingInfo");
                JSONObject adsInfo = data.getJSONObject("adsInfo");
                JSONObject routingExtraInfo = data.getJSONObject("routingExtraInfo");
                JSONObject sender = data.getJSONObject("sender");
                JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("routingInfo", routingInfo);
               // jsonObject2.put("adsInfo", adsInfo);
                jsonObject2.put("routingExtraInfo", routingExtraInfo);
                jsonObject2.put("recipient", recipient);
                jsonObject2.put("sender", sender);
                jsonObject2.put("templateURL", templateURL);
                jsonObject2.put("waybillCode", waybillCode);
                String jsonString = jsonObject2.toJSONString();

                //重塑打印json
                JSONObject jsonprint = new JSONObject();
                jsonprint.put("cmd","print");
                jsonprint.put("requestID",waybillCode);
                JSONObject taskjson = new JSONObject();
                taskjson.put("taskID","1667543540904");
                taskjson.put("printer","");
                taskjson.put("previewType","pdf");
                taskjson.put("preview",false);
                JSONArray documents=new JSONArray();
                JSONObject jsondata=new JSONObject();
                jsondata.put("data",jsonObject2);
                jsondata.put("templateURL",templateURL);
                //增加备注字段
                JSONObject jsondataremark=new JSONObject();
                JSONObject remarkitem=new JSONObject();
                remarkitem.put("zdyContent","remark");
                jsondataremark.put("templateURL","https://cloudprint.cainiao.com/template/customArea/22937207/23");
                jsondataremark.put("data",remarkitem);

                JSONArray contents=new JSONArray();
                contents.add(jsondata);
                contents.add(jsondataremark);
                JSONObject  contentsjson=new JSONObject();
                contentsjson.put("contents",contents);
                contentsjson.put("documentID",waybillCode);
                documents.add(contentsjson);
                taskjson.put("documents",documents);
                jsonprint.put("task",taskjson);
                String jsonString2 = jsonprint.toJSONString();

                log.debug("print_data ={}", jsonString);
                log.debug("jsonprint_data ={}", jsonprint);
                sql = "update m_tbretail m set m.waybill_number = ?, PRINT_DATA = ?,PRINT_DATA2=?  where m.id = ?";
                jdbcTemplate.update(sql, waybillCode, jsonString,jsonString2,orderId);
                log.debug("update m_tbretail m set m.waybill_number = {} where m.id = {}", waybillCode, orderId);
                return waybillCode;
            }
        }else {
            String subMsg = response.getSubMsg();
            String subMsg2 =response.getSubMessage();
            String msg = response.getMsg();
            log.error("CainiaoWaybillIiGetResponse:{}, errMsg:{}", subMsg, msg);
            //return "ERROR:" + (StringUtils.hasLength(subMsg) ? subMsg : msg);
            return "ERROR:" +subMsg+"/"+subMsg2+"/"+msg;
        }
        return "";
    }

    public Map<String, Object> getWaybill(String docNo, Integer items, String logisticCode, Integer storeId, Address recipientAddress, List<CainiaoWaybillIiGetRequest.Item> goodsList) throws ApiException {
        Map<String, Object> map = new HashMap<>(5);
        List<WarehouseShip> byDocNo = warehouseRepository.findByDocNo(docNo);
        if (byDocNo.size() != 0) {
            WarehouseShip warehouseShip = byDocNo.stream().findFirst().orElse(null);
            log.debug("docNo已存在={}", docNo);
            map.put("code", 1);
            map.put("data", warehouseShip);
            map.put("errMsg", "docNo已存在！");
            return map;
        }

        String url = "http://gw.api.taobao.com/router/rest";
        String appkey = LogisticsConfig.APP_KEY;
        String secret = LogisticsConfig.APP_SECRET;
        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
        CainiaoWaybillIiGetRequest req = new CainiaoWaybillIiGetRequest();
        CainiaoWaybillIiGetRequest.WaybillCloudPrintApplyNewRequest waybillCloudPrintApplyNewRequest = new CainiaoWaybillIiGetRequest.WaybillCloudPrintApplyNewRequest();
        waybillCloudPrintApplyNewRequest.setCpCode(logisticCode);
        String sql = "SELECT x.id, x.access_token, x.app_id, x.useful_time, x.mark, x.taobao_user_id, x.taobao_user_nick, x.app_secret from xcx_access_token x  WHERE x.app_id = ?";
        TokenInfo tokenInfo = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            TokenInfo info = new TokenInfo();
            info.setId(rs.getInt("id"));
            info.setAccessToken(rs.getString("access_token"));
            info.setAppId(rs.getString("app_id"));
            info.setUsefulTime(rs.getDate("useful_time"));
            info.setMark(rs.getString("mark"));
            info.setTaobaoUserId(rs.getLong("taobao_user_id"));
            info.setTaobaoUserNick(rs.getString("taobao_user_nick"));
            info.setAppSecret(rs.getString("app_secret"));
            return info;
        }, appkey);
        Long taobaoUserId = tokenInfo.getTaobaoUserId();
        String accessToken = tokenInfo.getAccessToken();
        sql = "SELECT c.id, c.province, c.city, c.distric, NVL(c.town, '') town, c.detail, c.name, c.phone mobile from C_CNADDRESS c WHERE c.c_store_id = ? AND ROWNUM <= 1";
        List<Map<String, Object>> query = jdbcTemplate.queryForList(sql, storeId);
        if (query.size() == 0) {
            map.put("code", -1);
            map.put("data", "");
            map.put("errMsg", "发货地址没有维护！");
            return map;
        }
        Map<String, Object> objectMap = query.stream().findFirst().get();
        Address responseAddress = new Address();
        responseAddress.setCity((String) objectMap.get("city"));
        responseAddress.setDetail((String) objectMap.get("detail"));
        responseAddress.setDistrict((String) objectMap.get("distric"));
        responseAddress.setProvince((String) objectMap.get("province"));
        responseAddress.setTown((String) objectMap.get("town"));
        responseAddress.setMobile((String) objectMap.get("mobile"));
        responseAddress.setName((String) objectMap.get("name"));

        CainiaoWaybillIiGetRequest.AddressDto addressDto = new CainiaoWaybillIiGetRequest.AddressDto();
        addressDto.setCity(responseAddress.getCity());
        addressDto.setDetail(responseAddress.getDetail());
        addressDto.setDistrict(responseAddress.getDistrict());
        addressDto.setProvince(responseAddress.getProvince());
        addressDto.setTown(responseAddress.getTown());
        CainiaoWaybillIiGetRequest.UserInfoDto userInfoDto = new CainiaoWaybillIiGetRequest.UserInfoDto();
        userInfoDto.setAddress(addressDto);
        userInfoDto.setMobile(responseAddress.getMobile());
        userInfoDto.setName(responseAddress.getName());
//        userInfoDto.setPhone("057123222");
        waybillCloudPrintApplyNewRequest.setSender(userInfoDto);

        List<CainiaoWaybillIiGetRequest.TradeOrderInfoDto> list5 = new ArrayList<>();
        CainiaoWaybillIiGetRequest.TradeOrderInfoDto tradeOrderInfoDto = new CainiaoWaybillIiGetRequest.TradeOrderInfoDto();
        list5.add(tradeOrderInfoDto);
        tradeOrderInfoDto.setObjectId(String.valueOf(LocalDateTime.now().getSecond()));
        CainiaoWaybillIiGetRequest.OrderInfoDto orderInfoDto = new CainiaoWaybillIiGetRequest.OrderInfoDto();
        //渠道类型
        String type = "OTHERS";
        orderInfoDto.setOrderChannelsType(type);
        orderInfoDto.setTradeOrderList(Collections.singletonList(docNo + "-" + type));
        orderInfoDto.setOutTradeOrderList(Collections.singletonList(docNo));
        tradeOrderInfoDto.setOrderInfo(orderInfoDto);
        CainiaoWaybillIiGetRequest.PackageInfoDto packageInfoDto = new CainiaoWaybillIiGetRequest.PackageInfoDto();
        //包裹id，用于拆合单场景
        packageInfoDto.setId(docNo);
        packageInfoDto.setItems(goodsList);
        packageInfoDto.setGoodsDescription("服装百货");
        tradeOrderInfoDto.setPackageInfo(packageInfoDto);
        CainiaoWaybillIiGetRequest.RecipientInfoDto recipientInfoDto = new CainiaoWaybillIiGetRequest.RecipientInfoDto();
        CainiaoWaybillIiGetRequest.AddressDto addressDto1 = new CainiaoWaybillIiGetRequest.AddressDto();
        addressDto1.setCity(recipientAddress.getCity());
        addressDto1.setDetail(recipientAddress.getDetail());
        addressDto1.setDistrict(recipientAddress.getDistrict());
        addressDto1.setProvince(recipientAddress.getProvince());
        addressDto1.setTown(recipientAddress.getTown());
        recipientInfoDto.setAddress(addressDto1);
        recipientInfoDto.setMobile(recipientAddress.getMobile());
        recipientInfoDto.setName(recipientAddress.getName());
//        recipientInfoDto.setOaid("");
//        recipientInfoDto.setTid("");
        tradeOrderInfoDto.setRecipient(recipientInfoDto);
        String stdtemplates = getCainiaoCloudprintStdtemplates(logisticCode);
       // log.debug("获取打印模板是2={}",stdtemplates);
        tradeOrderInfoDto.setTemplateUrl(stdtemplates);
        tradeOrderInfoDto.setUserId(taobaoUserId);

        waybillCloudPrintApplyNewRequest.setTradeOrderInfoDtos(list5);
        //添加月结账号
        if(logisticCode.equals("LE04284890")){
            waybillCloudPrintApplyNewRequest.setCustomerCode("021K1783756");
            //https://support-cnkuaidi.taobao.com/doc.htm#?docId=121888&docType=1
            waybillCloudPrintApplyNewRequest.setProductCode("ed-m-0001");
        }
        req.setParamWaybillCloudPrintApplyNewRequest(waybillCloudPrintApplyNewRequest);
        CainiaoWaybillIiGetResponse response = client.execute(req, accessToken);
        //System.out.println(response.getBody());
        log.debug("订单打印配置是CainiaoWaybillIiGetResponse ={}", JSONObject.toJSONString(response));

        if (response.isSuccess()) {
          //  log.debug("获取物流单号成功！");
            Optional<CainiaoWaybillIiGetResponse.WaybillCloudPrintResponse> first = response.getModules().stream().findFirst();
            if (first.isPresent()) {
                CainiaoWaybillIiGetResponse.WaybillCloudPrintResponse waybillCloudPrintResponse = first.get();
                String waybillCode = waybillCloudPrintResponse.getWaybillCode();
                String printData = waybillCloudPrintResponse.getPrintData();
                JSONObject jsonObject = JSONObject.parseObject(printData);
                String templateURL = jsonObject.getString("templateURL");
                JSONObject data = jsonObject.getJSONObject("data");
                JSONObject recipient = data.getJSONObject("recipient");
                JSONObject routingInfo = data.getJSONObject("routingInfo");
                JSONObject adsInfo = data.getJSONObject("adsInfo");
                JSONObject routingExtraInfo = data.getJSONObject("routingExtraInfo");
                JSONObject sender = data.getJSONObject("sender");
                JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("routingInfo", routingInfo);
              //  jsonObject2.put("adsInfo", adsInfo);
                jsonObject2.put("routingExtraInfo", routingExtraInfo);
                jsonObject2.put("recipient", recipient);
                jsonObject2.put("sender", sender);
                jsonObject2.put("templateURL", templateURL);
                jsonObject2.put("waybillCode", waybillCode);
                String jsonString = jsonObject2.toJSONString();


                //重塑打印json
                JSONObject jsonprint = new JSONObject();
                jsonprint.put("cmd","print");
                jsonprint.put("requestID",waybillCode);
                JSONObject taskjson = new JSONObject();
                taskjson.put("taskID","1667543540904");
                taskjson.put("printer","");
                taskjson.put("previewType","pdf");
                taskjson.put("preview",false);
                JSONArray documents=new JSONArray();
                JSONObject jsondata=new JSONObject();
                jsondata.put("data",jsonObject2);
                jsondata.put("templateURL",templateURL);
                //增加备注字段
                JSONObject jsondataremark=new JSONObject();
                JSONObject remarkitem=new JSONObject();
                remarkitem.put("zdyContent","remark");
                jsondataremark.put("templateURL","https://cloudprint.cainiao.com/template/customArea/22937207/23");
                jsondataremark.put("data",remarkitem);

                JSONArray contents=new JSONArray();
                contents.add(jsondata);
                contents.add(jsondataremark);
                JSONObject  contentsjson=new JSONObject();
                contentsjson.put("contents",contents);
                contentsjson.put("documentID",waybillCode);
                documents.add(contentsjson);
                taskjson.put("documents",documents);
                jsonprint.put("task",taskjson);
                String jsonString2 = jsonprint.toJSONString();
                log.debug("print_data ={}", jsonString);
                log.debug("print_data2 ={}", jsonString2);
                WarehouseShip warehouseShip = new WarehouseShip();
                warehouseShip.setWaybillCode(waybillCode);
                warehouseShip.setDocNo(docNo);
                warehouseShip.setPrintDate(jsonString);
                warehouseShip.setPrintDate2(jsonString2);
                WarehouseShip ship = warehouseRepository.saveAndFlush(warehouseShip);
                map.put("code", 1);
                map.put("data", ship);
                map.put("errMsg", "SUCCESS!");
                return map;
            }
        }else {
            String subMsg = response.getSubMsg();
            String subMsg2 =response.getSubMessage();
            String msg = response.getMsg();
            map.put("code", -1);
            map.put("data", "");
            map.put("errMsg", "ERROR:" +subMsg+"/"+subMsg2+"/"+msg);
            return map;
        }
        map.put("code", -1);
        map.put("data", "");
        map.put("errMsg", "No Data Found!");
        return map;
    }

    /**
     * 获取菜鸟打印模板
     * @return
     * @throws ApiException
     */
    private String getCainiaoCloudprintStdtemplates(String logisticCode) throws ApiException {
        Optional<CaiNiaoStandardTemplate> caiNiaoStandardTemplate = caiNiaoStandardTemplateRepository.findByCpCode(logisticCode);
        CaiNiaoStandardTemplate standardTemplate = caiNiaoStandardTemplate.orElseGet(() -> {
            long count = caiNiaoStandardTemplateRepository.count();
            if (count == 0) {
                try {

                    List<CaiNiaoStandardTemplate> allCaiNiaoTemplate = getAllCaiNiaoTemplate();
                    List<CaiNiaoStandardTemplate> collect = allCaiNiaoTemplate.stream()
                            .filter(a -> a.getCpCode().equalsIgnoreCase(logisticCode)).collect(Collectors.toList());
                    Map<String, Long> map = collect.stream()
                            .collect(Collectors.toMap(CaiNiaoStandardTemplate::getCpCode, CaiNiaoStandardTemplate::getTemplateId));
                    System.out.println("map = " + map);

                    return collect.size() == 0 ? null : collect.get(0);
                } catch (ApiException e) {
                    log.error("getCainiaoCloudprintStdtemplates ERROR = ", e.getErrMsg());
                    e.printStackTrace();
                }
            }
            return null;
        });
       // log.debug("获取模板数据={}", "我看下结果5"+standardTemplate.toString());
        assert standardTemplate != null;
       // return "http://cloudprint.cainiao.com/template/standard/" + standardTemplate.getTemplateId();
        return standardTemplate.getTemplateUrl();
    }


    //获取快递公司标准打印模板
    public  String  getTemplates() throws ApiException {
        String url;
        url = "http://gw.api.taobao.com/router/rest";
        //TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
        DefaultTaobaoClient defaultTaobaoClient = new DefaultTaobaoClient(url, LogisticsConfig.APP_KEY, LogisticsConfig.APP_SECRET);
        CainiaoCloudprintStdtemplatesGetRequest req = new CainiaoCloudprintStdtemplatesGetRequest();
        CainiaoCloudprintStdtemplatesGetResponse rsp = defaultTaobaoClient.execute(req);
        System.out.println(rsp.getBody());
        return rsp.getBody().toString();
    }


        public List<CaiNiaoStandardTemplate> getAllCaiNiaoTemplate() throws ApiException {
       // private List<CaiNiaoStandardTemplate> getAllCaiNiaoTemplate() throws ApiException {
        String url;
        url = "http://gw.api.taobao.com/router/rest";
        DefaultTaobaoClient defaultTaobaoClient = new DefaultTaobaoClient(url, LogisticsConfig.APP_KEY, LogisticsConfig.APP_SECRET);
        CainiaoCloudprintStdtemplatesGetRequest cloudprintStdtemplatesGetRequest = new CainiaoCloudprintStdtemplatesGetRequest();
        CainiaoCloudprintStdtemplatesGetResponse execute = defaultTaobaoClient.execute(cloudprintStdtemplatesGetRequest);
        System.out.println("CainiaoCloudprintStdtemplatesGetResponse = " + execute);
        if (!execute.isSuccess()) {
            log.error("cainiao.cloudprint.stdtemplates ERROR={}", execute.getMessage());
            log.error("cainiao.cloudprint.stdtemplates ERROR={}", execute.getSubMsg());
            return new ArrayList<>();
        }
        //log.info("获取快递公司模板是"+execute.getResult().toString());
        List<CainiaoCloudprintStdtemplatesGetResponse.StandardTemplateResult> datas = execute.getResult().getDatas();

        List<CaiNiaoStandardTemplate> arrayList = new ArrayList<>();
        for (CainiaoCloudprintStdtemplatesGetResponse.StandardTemplateResult result :
                datas) {
            String cpCode = result.getCpCode();
            List<CainiaoCloudprintStdtemplatesGetResponse.StandardTemplateDo> standardTemplates = result.getStandardTemplates();
            for (CainiaoCloudprintStdtemplatesGetResponse.StandardTemplateDo standardTemplateDo :
                    standardTemplates) {
                Long standardWaybillType = standardTemplateDo.getStandardWaybillType();
                if (standardWaybillType != 1) {
                    continue;
                }
                Long standardTemplateId = standardTemplateDo.getStandardTemplateId();
                String standardTemplateName = standardTemplateDo.getStandardTemplateName();
                String standardTemplateUrl = standardTemplateDo.getStandardTemplateUrl();
                CaiNiaoStandardTemplate template = new CaiNiaoStandardTemplate();
                template.setCpCode(cpCode);
                template.setTemplateId(standardTemplateId);
                template.setTemplateName(standardTemplateName);
                template.setTemplateType(standardWaybillType);
                template.setTemplateUrl(standardTemplateUrl);
                arrayList.add(template);
            }
        }
        return caiNiaoStandardTemplateRepository.saveAll(arrayList);
    }

    /**
     * 获取发货人信息
     * @param appkey
     * @param secret
     * @param sessionKey
     * @param logisticCode
     * @param branchCode
     * @param name
     * @return
     * @throws ApiException
     */
    private CainiaoWaybillIiSearchResponse.AddressDto getSenderInfo(String appkey, String secret, String sessionKey, String logisticCode, String branchCode, String name) throws ApiException {
        String url = "http://gw.api.taobao.com/router/rest";
        System.out.println("appkey = [" + appkey + "], secret = [" + secret + "], sessionKey = [" + sessionKey + "], logisticCode = [" + logisticCode + "], branchCode = [" + branchCode + "]");
        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
        CainiaoWaybillIiSearchRequest cainiaoWaybillIiSearchRequest = new CainiaoWaybillIiSearchRequest();
        cainiaoWaybillIiSearchRequest.setCpCode(logisticCode);
        CainiaoWaybillIiSearchResponse execute = client.execute(cainiaoWaybillIiSearchRequest, sessionKey);
        log.debug("getSenderInfo={}", JSONObject.toJSONString(execute.getWaybillApplySubscriptionCols()));
        if (execute.isSuccess()) {
            Optional<CainiaoWaybillIiSearchResponse.AddressDto> optionalAddressDto = Optional.ofNullable(null);
            Stream<CainiaoWaybillIiSearchResponse.AddressDto> addressDtoStream = execute.getWaybillApplySubscriptionCols().stream()
                    .filter(u -> u.getCpCode().equalsIgnoreCase(logisticCode))
                    .flatMap(u -> u.getBranchAccountCols().stream()
                            .filter(waybillBranchAccount -> waybillBranchAccount.getQuantity() > 0
                                    && waybillBranchAccount.getBranchCode().equalsIgnoreCase(branchCode)))
                    .flatMap(s -> s.getShippAddressCols().stream());
            List<CainiaoWaybillIiSearchResponse.AddressDto> collect = addressDtoStream.collect(Collectors.toList());
            if (collect.size() > 1) {
                optionalAddressDto = collect.stream().filter(s -> s.getDetail().contains(name)).findFirst();
            }else {
                optionalAddressDto = collect.stream().findFirst();
            }
            assert optionalAddressDto.isPresent();
            return optionalAddressDto.get();
        }
        return null;
    }

    private String getOrderchannelsType(String platformType) {
        switch (platformType) {
            case "2":
                platformType = "TB";
                break;
            case "5":
                platformType = "TM";
                break;
            default:
                platformType = "OTHERS";
                break;
        }
        return platformType;
    }

    /**
     * 菜鸟组件打印信息
     * @param orderId
     * @return
     */
    public String getOrderInfo(Integer orderId) {
        String sql = "SELECT * from M_TBRETAIL m WHERE m.id = ?";
        String printData;
        String waybillCode;
        try {
            Map<String, Object> map = jdbcTemplate.queryForMap(sql, orderId);
            printData = (String) map.get("PRINT_DATA");
            waybillCode = (String) map.get("WAYBILL_NUMBER");
        } catch (DataAccessException e) {
            e.printStackTrace();
            log.error("M_TBRETAIL Data is not exist!!! ID={}", orderId);
            return "";
        }
        PrintView view = new PrintView();
        view.setCmd("print");
        view.setRequestID(waybillCode);
        view.setVersion("");
        PrintView.Task task = new PrintView.Task();
        task.setPreview(false);
        task.setPreviewType("pdf");
        //任务ID
        task.setTaskID(String.valueOf(LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli()));
        //如果为空，会使用默认打印机
        task.setPrinter("");
        PrintView.Documents documents = new PrintView.Documents();
        documents.setDocumentID(waybillCode);
        PrintView.Contents contents = new PrintView.Contents();
        JSONObject jsonObject = JSONObject.parseObject(printData);
        contents.setData(jsonObject);
        String templateURL = jsonObject.getString("templateURL");
        contents.setTemplateURL(templateURL);
        List<PrintView.Contents> collect = Stream.of(contents).collect(Collectors.toList());
        documents.setContents(collect);
        List<PrintView.Documents> documentList = Stream.of(documents).collect(Collectors.toList());
        task.setDocuments(documentList);
        view.setTask(task);
        String jsonString = JSONObject.toJSONString(view);
        log.debug("The printed waybillCode={}",  waybillCode);
        return jsonString;
    }

    public String getWaybillShipInfo(String cpCode, String waybillCode) throws ApiException {
        String url = "http://gw.api.taobao.com/router/rest";
        TaobaoClient client = new DefaultTaobaoClient(url, LogisticsConfig.APP_KEY, LogisticsConfig.APP_SECRET);
        CainiaoWaybillIiLogisticsdetailUrlGetRequest req = new CainiaoWaybillIiLogisticsdetailUrlGetRequest();
        req.setCpCode(cpCode);
        req.setWaybillCode(waybillCode);
        CainiaoWaybillIiLogisticsdetailUrlGetResponse rsp = client.execute(req);
        if (rsp.isSuccess()) {
            return rsp.getUrl();
        }
        return null;
    }


    public static void main(String[] args) throws URISyntaxException, ApiException {
//        String printData = "{\"data\":{\"_dataFrom\":\"waybill\",\"adsInfo\":{\"adId\":\"2284\",\"advertisementType\":\"COMMERCIAL\",\"bannerUrl\":\"http://ad-cdn.cainiao.com/1548/1628845466589.png\",\"miniBannerUrl\":\"http://ad-cdn.cainiao.com/1548/1628845461459.png\",\"trackUrl\":\"https://ad.cainiao.com/wqVYXoZ\"},\"cpCode\":\"ZTO\",\"extraInfo\":{},\"needEncrypt\":false,\"packageInfo\":{\"id\":\"TB2108280132\",\"items\":[{\"count\":1,\"name\":\"纪念日百货\"}],\"volume\":0,\"weight\":0},\"parent\":false,\"recipient\":{\"address\":{\"detail\":\"AES:Tq1Nc+VNfbr9P5d5X2NBX8Dqa7WckAZY930N5KUuHxn5pR8eJb5tGBOiKtqmVBYo\",\"province\":\"安徽省\"},\"mobile\":\"AES:F/e4PN8EOiNai1710oeb/w==\",\"name\":\"AES:ttg3NlMwUcJ5Iy3etEopow==\",\"tid\":\"2064929582593187960\"},\"routingInfo\":{\"consolidation\":{\"name\":\"合肥\"},\"origin\":{\"code\":\"55156\",\"name\":\"合肥庐阳产业园\"},\"receiveBranch\":{\"code\":\"55156\",\"name\":\"合肥庐阳产业园\"},\"routeCode\":\"D-14 15\",\"sortation\":{\"name\":\"460-\"},\"startCenter\":{},\"terminalCenter\":{}},\"sender\":{\"address\":{\"city\":\"合肥市\",\"detail\":\"天河路与天水路交口往南100米天河路1001号\",\"district\":\"庐阳区\",\"province\":\"安徽省\"},\"mobile\":\"15055190645\",\"name\":\"袁冬冬\"},\"shippingOption\":{\"code\":\"STANDARD_EXPRESS\",\"title\":\"标准快递\"},\"waybillCode\":\"75801888814108\"},\"signature\":\"MD:ah0OTptRzqe9KptVIWguDg==\",\"templateURL\":\"http://cloudprint.cainiao.com/template/standard/301\"}";
//        JSONObject jsonObject = JSONObject.parseObject(printData);
//        String templateURL = jsonObject.getString("templateURL");
//        JSONObject data = jsonObject.getJSONObject("data");
//        JSONObject recipient = data.getJSONObject("recipient");
//        JSONObject routingInfo = data.getJSONObject("routingInfo");
//        JSONObject sender = data.getJSONObject("sender");
//        JSONObject jsonObject2 = new JSONObject();
//        jsonObject2.put("routingInfo", routingInfo);
//        jsonObject2.put("recipient", recipient);
//        jsonObject2.put("sender", sender);
//        jsonObject2.put("templateURL", templateURL);
//        String jsonString = jsonObject2.toJSONString();
//        log.debug("print_data ={}", jsonString);
////        String sendstr = "{\"cmd\":\"print\",\"requestID\":\"123458976\",\"version\":\"1.0\",\"task\":{\"taskID\":\"7293666\",\"preview\":false,\"printer\":\"\",\"notifyMode\":\"allInOne\",\"previewType\":\"pdf\",\"documents\":[{\"documentID\":\"0123456789\",\"contents\":[{\"data\":{\"routingInfo\":{\"routeCode\":\"D-14 15\",\"sortation\":{\"name\":\"460-\"},\"startCenter\":{},\"origin\":{\"code\":\"55156\",\"name\":\"合肥庐阳产业园\"},\"receiveBranch\":{\"code\":\"55156\",\"name\":\"合肥庐阳产业园\"},\"consolidation\":{\"name\":\"合肥\"},\"terminalCenter\":{}},\"sender\":{\"address\":{\"province\":\"安徽省\",\"city\":\"合肥市\",\"district\":\"庐阳区\",\"detail\":\"天河路与天水路交口往南100米天河路1001号\"},\"mobile\":\"15055190645\",\"name\":\"袁冬冬\"},\"recipient\":{\"address\":{\"province\":\"安徽省\",\"detail\":\"AES:Tq1Nc+VNfbr9P5d5X2NBX8Dqa7WckAZY930N5KUuHxn5pR8eJb5tGBOiKtqmVBYo\"},\"mobile\":\"AES:F/e4PN8EOiNai1710oeb/w==\",\"name\":\"AES:ttg3NlMwUcJ5Iy3etEopow==\",\"tid\":\"2064929582593187960\"},\"templateURL\":\"http://cloudprint.cainiao.com/template/standard/301\",\"waybillCode\":\"0123456789\"},\"templateURL\":\"http://cloudprint.cainiao.com/cloudprint/template/getStandardTemplate.json?template_id=101&version=4\"}]}]}}";
//        String sendstr = "{\"cmd\":\"print\",\"requestID\":\"123458976\",\"version\":\"1.0\",\"task\":{\"taskID\":\"7293666\",\"preview\":false,\"printer\":\"\",\"previewType\":\"pdf\",\"documents\":[{\"documentID\":\"0123456789\",\"contents\":[{\"data\":{\"recipient\":{\"address\":{\"city\":\"杭州市\",\"detail\":\"良睦路999号乐佳国际大厦2号楼小邮局\",\"district\":\"余杭区\",\"province\":\"浙江省\",\"town\":\"\"},\"mobile\":\"13012345678\",\"name\":\"菜鸟网络\",\"phone\":\"057112345678\"},\"routingInfo\":{\"consolidation\":{\"name\":\"杭州\",\"code\":\"hangzhou\"},\"origin\":{\"name\":\"杭州\",\"code\":\"POSTB\"},\"sortation\":{\"name\":\"杭州\"},\"routeCode\":\"123A-456-789\"},\"sender\":{\"address\":{\"city\":\"杭州市\",\"detail\":\"文一西路1001号阿里巴巴淘宝城5号小邮局\",\"district\":\"余杭区\",\"province\":\"浙江省\",\"town\":\"\"},\"mobile\":\"13012345678\",\"name\":\"阿里巴巴\",\"phone\":\"057112345678\"},\"waybillCode\":\"0123456789\"},\"signature\":\"19d6f7759487e556ddcdd3d499af087080403277b7deed1a951cc3d9a93c42a7e22ccba94ff609976c5d3ceb069b641f541bc9906098438d362cae002dfd823a8654b2b4f655e96317d7f60eef1372bb983a4e3174cc8d321668c49068071eaea873071ed683dd24810e51afc0bc925b7a2445fdbc2034cdffb12cb4719ca6b7\",\"templateURL\":\"http://cloudprint.cainiao.com/cloudprint/template/getStandardTemplate.json?template_id=101&version=4\"}]}]}}";
//        WebSocketClientManager clientManager = WebSocketClientManager.getwebSocket();
//        clientManager.sendStr(sendstr);
//        String url = "http://gw.api.taobao.com/router/rest";
//        DefaultTaobaoClient defaultTaobaoClient = new DefaultTaobaoClient(url, LogisticsConfig.APP_KEY, LogisticsConfig.APP_SECRET);
//        CainiaoCloudprintStdtemplatesGetRequest cloudprintStdtemplatesGetRequest = new CainiaoCloudprintStdtemplatesGetRequest();
//        CainiaoCloudprintStdtemplatesGetResponse execute = defaultTaobaoClient.execute(cloudprintStdtemplatesGetRequest);
//        System.out.println("CainiaoCloudprintStdtemplatesGetResponse = " + JSONObject.toJSONString(execute));


        String printData = "{\"routingInfo\":{\"sortation\":{\"name\":\"460-\"},\"startCenter\":{},\"origin\":{\"code\":\"55156\",\"name\":\"合肥庐阳产业园\"},\"receiveBranch\":{},\"consolidation\":{\"name\":\"合肥\"},\"terminalCenter\":{}},\"sender\":{\"address\":{\"province\":\"安徽省\",\"city\":\"合肥市\",\"district\":\"庐阳区\",\"detail\":\"天河路与天水路交口往南100米天河路1001号\"},\"mobile\":\"13866756834\",\"name\":\"JNR淘宝店\"},\"recipient\":{\"address\":{\"province\":\"合肥市\",\"town\":\"合肥市\",\"city\":\"合肥市\",\"district\":\"合肥市\",\"detail\":\"合肥市\"},\"mobile\":\"13612341234\",\"name\":\"王二狗\",\"tid\":\"\"},\"waybillCode\":\"75814105412636\",\"templateURL\":\"http://cloudprint.cainiao.com/template/standard/301\"}";
        JSONObject jsonObject = JSONObject.parseObject(printData);
        System.out.println(jsonObject.toJSONString());


    }



}
