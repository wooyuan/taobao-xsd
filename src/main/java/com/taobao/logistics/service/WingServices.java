package com.taobao.logistics.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
//import com.taobao.api.request.CainiaoCloudprintStdtemplatesGetRequest;
//import com.taobao.api.request.CainiaoWaybillIiGetRequest;
//import com.taobao.api.request.CainiaoWaybillIiLogisticsdetailUrlGetRequest;
//import com.taobao.api.request.CainiaoWaybillIiSearchRequest;
//import com.taobao.api.response.CainiaoCloudprintStdtemplatesGetResponse;
//import com.taobao.api.response.CainiaoWaybillIiGetResponse;
//import com.taobao.api.response.CainiaoWaybillIiLogisticsdetailUrlGetResponse;
//import com.taobao.api.response.CainiaoWaybillIiSearchResponse;
import com.taobao.logistics.config.LogisticsConfig;
import com.taobao.logistics.entity.*;
import com.taobao.logistics.repository.CaiNiaoStandardTemplateRepository;
import com.taobao.logistics.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 *  wing 平台物流接口
 */

@Slf4j
@Service
public class WingServices {


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CaiNiaoStandardTemplateRepository caiNiaoStandardTemplateRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;



    /*
     * waybillCode 物流单号
     * 打印模板解析重构
     * 获取打印模板后添加自定义打印区域备注
     * */
    public JSONObject printjson(JSONObject jsonObject,String waybillCode){
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
        return jsonprint;
    }

    /*
     * 获取token
     *  @param appkey
     * @param secret
     * @param logisticCode 快递公司编码
     * */
//    public String getaccessToken(String appkey,String secret,String logisticCode){
//        String url = "http://gw.api.taobao.com/router/rest";
//        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
//        CainiaoWaybillIiGetRequest req = new CainiaoWaybillIiGetRequest();
//        CainiaoWaybillIiGetRequest.WaybillCloudPrintApplyNewRequest waybillCloudPrintApplyNewRequest = new CainiaoWaybillIiGetRequest.WaybillCloudPrintApplyNewRequest();
//        waybillCloudPrintApplyNewRequest.setCpCode(logisticCode);
//        String sql = "SELECT x.id, x.access_token, x.app_id, x.useful_time, x.mark, x.taobao_user_id, x.taobao_user_nick, x.app_secret from xcx_access_token x  WHERE x.app_id = ?";
//        String accessToken ="";
//        try {
//            SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
//            dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
//            dataSource.setUrl("jdbc:oracle:thin:@10.100.21.181:1521/orcl");
//            dataSource.setUsername("neands3");
//            dataSource.setPassword("abc123");
//
//            // 创建JdbcTemplate实例
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//
//            TokenInfo tokenInfo = jdbcTemplate.queryForObject(sql,
//                    (rs, rowNum) -> {
//                        TokenInfo info = new TokenInfo();
//                        info.setId(rs.getInt("id"));
//                        info.setAccessToken(rs.getString("access_token"));
//                        info.setAppId(rs.getString("app_id"));
//                        info.setUsefulTime(rs.getDate("useful_time"));
//                        info.setMark(rs.getString("mark"));
//                        info.setTaobaoUserId(rs.getLong("taobao_user_id"));
//                        info.setTaobaoUserNick(rs.getString("taobao_user_nick"));
//                        info.setAppSecret(rs.getString("app_secret"));
//                        return info;
//                    }, appkey);
//            accessToken = tokenInfo.getAccessToken();
//        }catch (Exception e){
//            System.out.println(e);
//        }
//
//        return  accessToken;
//    }

    /*
     *  发货信息获取
     * */
    public Address  getbySql(String wms_store_id){
        String sql = "SELECT c.id, c.province, c.city, c.distric, NVL(c.town, '') town, c.detail, " +
                "c.name, c.phone mobile from WMS_STORE@cloud_151 c WHERE c.id= ? ";
        List<Map<String, Object>> query = jdbcTemplate.queryForList(sql, wms_store_id);
        Address responseAddress = new Address();
        if(query.size()>0){
            Map<String, Object> objectMap = query.stream().findFirst().get();
            responseAddress.setCity((String) objectMap.get("city"));
            responseAddress.setDetail((String) objectMap.get("detail"));
            responseAddress.setDistrict((String) objectMap.get("distric"));
            responseAddress.setProvince((String) objectMap.get("province"));
            responseAddress.setTown((String) objectMap.get("town"));
            responseAddress.setMobile((String) objectMap.get("mobile"));
            responseAddress.setName((String) objectMap.get("name"));
            responseAddress.setLine(1);
        }
        return responseAddress;
    }

    /*
     *  收货人信息
     * */
    public Address  getrecipientAddress(String docno){
        String sql = "select * from  ";
        List<Map<String, Object>> query = jdbcTemplate.queryForList(sql, docno);
        Address responseAddress = new Address();
        if(query.size()>0){
            Map<String, Object> objectMap = query.stream().findFirst().get();
            responseAddress.setCity((String) objectMap.get("city"));
            responseAddress.setDetail((String) objectMap.get("detail"));
            responseAddress.setDistrict((String) objectMap.get("distric"));
            responseAddress.setProvince((String) objectMap.get("province"));
            responseAddress.setTown((String) objectMap.get("town"));
            responseAddress.setMobile((String) objectMap.get("mobile"));
            responseAddress.setName((String) objectMap.get("name"));
            responseAddress.setLine(1);
        }
        return responseAddress;
    }

    /*
     * 拼接发货信息获取物流订单
     */
    public Map<String, Object> getWaybill(String docNo) throws ApiException {
        Map<String, Object> map = new HashMap<>(5);
        String wms_store_id="123123";//发货信息获取
        String logisticCode= "123123";//平台编号
        JSONArray items2 = new JSONArray();
//        List<CainiaoWaybillIiGetRequest.Item> goodsList = items2.toJavaList(CainiaoWaybillIiGetRequest.Item.class);
//        if (goodsList.size() == 0) {
//            CainiaoWaybillIiGetRequest.Item item = new CainiaoWaybillIiGetRequest.Item();
//            item.setName("纪念日百货");
//            item.setCount(1L);
//            goodsList.add(item);
//        }
//        String accessToken=getaccessToken(LogisticsConfig.APP_KEY,LogisticsConfig.APP_SECRET,logisticCode);
//
//        Address responseAddress= getbySql(wms_store_id);
//        CainiaoWaybillIiGetRequest.AddressDto addressDto = new CainiaoWaybillIiGetRequest.AddressDto();
//        addressDto.setCity(responseAddress.getCity());
//        addressDto.setDetail(responseAddress.getDetail());
//        addressDto.setDistrict(responseAddress.getDistrict());
//        addressDto.setProvince(responseAddress.getProvince());
//        addressDto.setTown(responseAddress.getTown());
//        CainiaoWaybillIiGetRequest.UserInfoDto userInfoDto = new CainiaoWaybillIiGetRequest.UserInfoDto();
//        userInfoDto.setAddress(addressDto);
//        userInfoDto.setMobile(responseAddress.getMobile());
//        userInfoDto.setName(responseAddress.getName());
//        CainiaoWaybillIiGetRequest.WaybillCloudPrintApplyNewRequest waybillCloudPrintApplyNewRequest = new CainiaoWaybillIiGetRequest.WaybillCloudPrintApplyNewRequest();
//        waybillCloudPrintApplyNewRequest.setSender(userInfoDto);
//        List<CainiaoWaybillIiGetRequest.TradeOrderInfoDto> list5 = new ArrayList<>();
//        CainiaoWaybillIiGetRequest.TradeOrderInfoDto tradeOrderInfoDto = new CainiaoWaybillIiGetRequest.TradeOrderInfoDto();
//        list5.add(tradeOrderInfoDto);
//        tradeOrderInfoDto.setObjectId(String.valueOf(LocalDateTime.now().getSecond()));
//        CainiaoWaybillIiGetRequest.OrderInfoDto orderInfoDto = new CainiaoWaybillIiGetRequest.OrderInfoDto();
//        //渠道类型
//        String type = "OTHERS";
//        orderInfoDto.setOrderChannelsType(type);
//        orderInfoDto.setTradeOrderList(Collections.singletonList(docNo + "-" + type));
//        orderInfoDto.setOutTradeOrderList(Collections.singletonList(docNo));
//        tradeOrderInfoDto.setOrderInfo(orderInfoDto);
//        CainiaoWaybillIiGetRequest.PackageInfoDto packageInfoDto = new CainiaoWaybillIiGetRequest.PackageInfoDto();
//        //包裹id，用于拆合单场景

//        packageInfoDto.setId(docNo);
//        packageInfoDto.setItems(goodsList);
//        packageInfoDto.setGoodsDescription("服装百货");
//        tradeOrderInfoDto.setPackageInfo(packageInfoDto);
//        CainiaoWaybillIiGetRequest.RecipientInfoDto recipientInfoDto = new CainiaoWaybillIiGetRequest.RecipientInfoDto();
//        CainiaoWaybillIiGetRequest.AddressDto addressDto1 = new CainiaoWaybillIiGetRequest.AddressDto();
//        addressDto1.setCity(recipientAddress.getCity());
//        addressDto1.setDetail(recipientAddress.getDetail());
//        addressDto1.setDistrict(recipientAddress.getDistrict());
//        addressDto1.setProvince(recipientAddress.getProvince());
//        addressDto1.setTown(recipientAddress.getTown());
//        recipientInfoDto.setAddress(addressDto1);
//        recipientInfoDto.setMobile(recipientAddress.getMobile());
//        recipientInfoDto.setName(recipientAddress.getName());
//
//        tradeOrderInfoDto.setRecipient(recipientInfoDto);
//        String stdtemplates = getCainiaoCloudprintStdtemplates(logisticCode);
//       // log.debug("获取打印模板是2={}",stdtemplates);
//        tradeOrderInfoDto.setTemplateUrl(stdtemplates);
//        tradeOrderInfoDto.setUserId(taobaoUserId);
//        waybillCloudPrintApplyNewRequest.setTradeOrderInfoDtos(list5);
//        req.setParamWaybillCloudPrintApplyNewRequest(waybillCloudPrintApplyNewRequest);
//        CainiaoWaybillIiGetResponse response = client.execute(req, accessToken);
//        //System.out.println(response.getBody());
//        log.debug("订单打印配置是CainiaoWaybillIiGetResponse ={}", JSONObject.toJSONString(response));
//        if (response.isSuccess()) {
//            log.debug("获取物流单号成功！");
//            Optional<CainiaoWaybillIiGetResponse.WaybillCloudPrintResponse> first = response.getModules().stream().findFirst();
//            if (first.isPresent()) {
//                CainiaoWaybillIiGetResponse.WaybillCloudPrintResponse waybillCloudPrintResponse = first.get();
//                String waybillCode = waybillCloudPrintResponse.getWaybillCode();
//                String printData = waybillCloudPrintResponse.getPrintData();
//                JSONObject jsonObject = JSONObject.parseObject(printData);
//                JSONObject jsonprint=printjson(jsonObject,waybillCode);//打印数据重新解析
//                String jsonString2 = jsonprint.toJSONString();
//                log.debug("print_data2 ={}", jsonString2);
//                WarehouseShip warehouseShip = new WarehouseShip();
//                warehouseShip.setWaybillCode(waybillCode);
//                warehouseShip.setDocNo(docNo);
//                warehouseShip.setPrintDate2(jsonString2);
//                WarehouseShip ship = warehouseRepository.saveAndFlush(warehouseShip);
//                //更新目标表
//                map.put("code", 1);
//                map.put("data", ship);
//                map.put("errMsg", "SUCCESS!");
//                return map;
//            }
//        }else {
//            String subMsg = response.getSubMsg();
//            String subMsg2 =response.getSubMessage();
//            String msg = response.getMsg();
//            map.put("code", -1);
//            map.put("data", "");
//            map.put("errMsg", "ERROR:" +subMsg+"/"+subMsg2+"/"+msg);
//            return map;
//        }
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
        String url="";
//        url = "http://gw.api.taobao.com/router/rest";
//        //TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
//        DefaultTaobaoClient defaultTaobaoClient = new DefaultTaobaoClient(url, LogisticsConfig.APP_KEY, LogisticsConfig.APP_SECRET);
//        CainiaoCloudprintStdtemplatesGetRequest req = new CainiaoCloudprintStdtemplatesGetRequest();
//        CainiaoCloudprintStdtemplatesGetResponse rsp = defaultTaobaoClient.execute(req);
//        System.out.println(rsp.getBody());
//        return rsp.getBody().toString();
        return url;
    }


    public List<CaiNiaoStandardTemplate> getAllCaiNiaoTemplate() throws ApiException {
        // private List<CaiNiaoStandardTemplate> getAllCaiNiaoTemplate() throws ApiException {
        String url;
        url = "http://gw.api.taobao.com/router/rest";
//        DefaultTaobaoClient defaultTaobaoClient = new DefaultTaobaoClient(url, LogisticsConfig.APP_KEY, LogisticsConfig.APP_SECRET);
//        CainiaoCloudprintStdtemplatesGetRequest cloudprintStdtemplatesGetRequest = new CainiaoCloudprintStdtemplatesGetRequest();
//        CainiaoCloudprintStdtemplatesGetResponse execute = defaultTaobaoClient.execute(cloudprintStdtemplatesGetRequest);
//        System.out.println("CainiaoCloudprintStdtemplatesGetResponse = " + execute);
//        if (!execute.isSuccess()) {
//            log.error("cainiao.cloudprint.stdtemplates ERROR={}", execute.getMessage());
//            log.error("cainiao.cloudprint.stdtemplates ERROR={}", execute.getSubMsg());
//            return new ArrayList<>();
//        }
//        //log.info("获取快递公司模板是"+execute.getResult().toString());
//        List<CainiaoCloudprintStdtemplatesGetResponse.StandardTemplateResult> datas = execute.getResult().getDatas();
//
//        List<CaiNiaoStandardTemplate> arrayList = new ArrayList<>();
//        for (CainiaoCloudprintStdtemplatesGetResponse.StandardTemplateResult result :
//                datas) {
//            String cpCode = result.getCpCode();
//            List<CainiaoCloudprintStdtemplatesGetResponse.StandardTemplateDo> standardTemplates = result.getStandardTemplates();
//            for (CainiaoCloudprintStdtemplatesGetResponse.StandardTemplateDo standardTemplateDo :
//                    standardTemplates) {
//                Long standardWaybillType = standardTemplateDo.getStandardWaybillType();
//                if (standardWaybillType != 1) {
//                    continue;
//                }
//                Long standardTemplateId = standardTemplateDo.getStandardTemplateId();
//                String standardTemplateName = standardTemplateDo.getStandardTemplateName();
//                String standardTemplateUrl = standardTemplateDo.getStandardTemplateUrl();
//                CaiNiaoStandardTemplate template = new CaiNiaoStandardTemplate();
//                template.setCpCode(cpCode);
//                template.setTemplateId(standardTemplateId);
//                template.setTemplateName(standardTemplateName);
//                template.setTemplateType(standardWaybillType);
//                template.setTemplateUrl(standardTemplateUrl);
//                arrayList.add(template);
//            }
//        }

//        return caiNiaoStandardTemplateRepository.saveAll(arrayList);

        return new ArrayList<>();
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
    private Object getSenderInfo(String appkey, String secret, String sessionKey, String logisticCode, String branchCode, String name) throws ApiException {

   //     private CainiaoWaybillIiSearchResponse.AddressDto getSenderInfo(String appkey, String secret, String sessionKey, String logisticCode, String branchCode, String name) throws ApiException {
        String url = "http://gw.api.taobao.com/router/rest";
        System.out.println("appkey = [" + appkey + "], secret = [" + secret + "], sessionKey = [" + sessionKey + "], logisticCode = [" + logisticCode + "], branchCode = [" + branchCode + "]");
//        TaobaoClient client = new DefaultTaobaoClient(url, appkey, secret);
//        CainiaoWaybillIiSearchRequest cainiaoWaybillIiSearchRequest = new CainiaoWaybillIiSearchRequest();
//        cainiaoWaybillIiSearchRequest.setCpCode(logisticCode);
//        CainiaoWaybillIiSearchResponse execute = client.execute(cainiaoWaybillIiSearchRequest, sessionKey);
//        log.debug("getSenderInfo={}", JSONObject.toJSONString(execute.getWaybillApplySubscriptionCols()));
//        if (execute.isSuccess()) {
//            Optional<CainiaoWaybillIiSearchResponse.AddressDto> optionalAddressDto = Optional.ofNullable(null);
//            Stream<CainiaoWaybillIiSearchResponse.AddressDto> addressDtoStream = execute.getWaybillApplySubscriptionCols().stream()
//                    .filter(u -> u.getCpCode().equalsIgnoreCase(logisticCode))
//                    .flatMap(u -> u.getBranchAccountCols().stream()
//                            .filter(waybillBranchAccount -> waybillBranchAccount.getQuantity() > 0
//                                    && waybillBranchAccount.getBranchCode().equalsIgnoreCase(branchCode)))
//                    .flatMap(s -> s.getShippAddressCols().stream());
//            List<CainiaoWaybillIiSearchResponse.AddressDto> collect = addressDtoStream.collect(Collectors.toList());
//            if (collect.size() > 1) {
//                optionalAddressDto = collect.stream().filter(s -> s.getDetail().contains(name)).findFirst();
//            }else {
//                optionalAddressDto = collect.stream().findFirst();
//            }
//            assert optionalAddressDto.isPresent();
//            return optionalAddressDto.get();
//        return url;
 //       }
        return null;
    }
    //订单类型
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
//        TaobaoClient client = new DefaultTaobaoClient(url, LogisticsConfig.APP_KEY, LogisticsConfig.APP_SECRET);
//        CainiaoWaybillIiLogisticsdetailUrlGetRequest req = new CainiaoWaybillIiLogisticsdetailUrlGetRequest();
//        req.setCpCode(cpCode);
//        req.setWaybillCode(waybillCode);
//        CainiaoWaybillIiLogisticsdetailUrlGetResponse rsp = client.execute(req);
//        if (rsp.isSuccess()) {
//            return rsp.getUrl();
//        }
        return null;
    }

    //测试获取打印模板
    public static void main(String[] args) throws URISyntaxException, ApiException {

        try{
            WingServices n =new WingServices();
            n.getTemplates();
        } catch (Exception e) {
            System.out.println("测试"+e.toString());
        }


    }
}
