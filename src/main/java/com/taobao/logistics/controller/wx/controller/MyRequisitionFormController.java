package com.taobao.logistics.controller.wx.controller;

import com.alibaba.fastjson.JSON;
import com.taobao.logistics.entity.wx.RequisitionOrder;
import com.taobao.logistics.entity.wx.RequisitionUser;
import com.taobao.logistics.service.wx.WxOrderServices;
import com.taobao.logistics.service.wx.WxUserServices;
import com.taobao.logistics.utils.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ShiShiDaWei on 2021/11/8.
 */
@Slf4j
@RestController
@RequestMapping("/wx/my")
public class MyRequisitionFormController {


    @Autowired
    private WxOrderServices wxOrderServices;

    @Autowired
    private WxUserServices wxUserServices;

    @GetMapping("/getMyReqInfo")
    public CommonResult getMyRequisitionBaseInfo(@RequestHeader(value = "jobNum", defaultValue = "-1") String jobNum){
        RequisitionUser user = wxUserServices.getUser(jobNum);
        if (null == user) {
            return new CommonResult(1001, "请授权登录");
        }
        Integer player = user.getPlayer();
        List<Map<String,Object>> infos;
        List<Map<String, Object>> dim12List;
        List<Map<String, Object>> areas;
        if (2 == player) {
            List<Map<String, Object>> storeList = wxUserServices.getStoreList();
            HashMap<String, Object> map = new HashMap<>();
            map.put("storeId", 1);
            map.put("storeName", "所有门店");
            storeList.add(map);
            infos = storeList.stream().sorted((o1, o2) -> {
                Integer storeId = (Integer) o1.get("storeId");
                Integer storeId2 = (Integer) o2.get("storeId");
                return storeId - storeId2;
            })
                    .collect(Collectors.toList());
            dim12List = wxOrderServices.getDistinctDim12List();
            areas = wxOrderServices.getDistinctProductAreas();
        }else {
            Integer id = user.getId();
            infos = wxUserServices.getStoreList(id);
            //dim12List = wxOrderServices.getDistinctDim12List(id);
            dim12List = wxOrderServices.getDistinctDim12List();
            //areas = wxOrderServices.getDistinctProductAreas(id);
            areas = wxOrderServices.getDistinctProductAreas();
            log.debug("====={}", JSON.toJSONString(infos));
            log.debug("====={}", JSON.toJSONString(dim12List));
            log.debug("====={}", JSON.toJSONString(areas));
        }

        List<Map<String,Object>> state = new ArrayList<>();
        HashMap<String, Object> map = new HashMap<>();
        map.put("status", 1);
        map.put("statusAttr", "未处理");
        HashMap<String, Object> map2 = new HashMap<>();
        map2.put("status", 2);
        map2.put("statusAttr", "已处理");
        state.add(map);
        state.add(map2);

//        List<List<Map<String, Object>>> collect = Stream.of(infos, dim12List, state, areas).collect(Collectors.toList());
        return new CommonResult(0, "SUCCESS").withData("firstClass", dim12List)
                .withData("stores", infos).withData("state", state).withData("areas", areas);
    }


    @PostMapping("/list")
    public CommonResult getAllRequisitionList(@RequestHeader(value = "jobNum", defaultValue = "-1") String jobNum, @RequestParam(defaultValue = "1", name = "page") Integer page, @RequestParam(defaultValue = "10", name = "pageSize")Integer pageSize,
                                              Integer status, Integer storeId, Integer mDim12Id, String productArea,
                                              @DateTimeFormat(pattern="yyyy-MM-dd") Date startTime, @DateTimeFormat(pattern="yyyy-MM-dd") Date endTime){
        System.out.println("jobNum = [" + jobNum + "], page = [" + page + "], pageSize = [" + pageSize + "], status = [" + status + "], storeId = [" + storeId + "], mDim12Id = [" + mDim12Id + "], productArea = ["
                + productArea + "], startTime = [" + startTime + "], endTime = [" + endTime + "]");
        RequisitionUser user = wxUserServices.getUser(jobNum);
        if (null == user) {
            return new CommonResult(1001, "请授权登录");
        }
//        RequisitionOrder order = new RequisitionOrder();
//        order.setReqUserId(user.getId());
//        order.setmDim12Id(mDim12Id);
//        order.setProductArea(productArea);
//        order.setStoreId(storeId);
//        order.setStatus(status);
//        Example<RequisitionOrder> example = Example.of(order);
//        List<RequisitionOrder> orderList = wxOrderServices.getOrderList(example,
//                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "modifieddate")));
        LocalDate localDateEnd = endTime.toInstant().atOffset(ZoneOffset.of("+8")).toLocalDate();
//        LocalDate localDateStart = startTime.toInstant().atOffset(ZoneOffset.of("+8")).toLocalDate();
//        if (!localDateEnd.isEqual(localDateStart)) {
//            localDateEnd = localDateEnd.plusDays(1);
//        }
        LocalDateTime of = LocalDateTime.of(localDateEnd, LocalTime.MAX);
        endTime = Date.from(of.atZone(ZoneId.systemDefault()).toInstant());
        System.out.println("LocalDateTime = " + of);
        Integer player = user.getPlayer();
       // System.out.println("用户角色是  " + player);
        if (2 == player) {
            List<RequisitionOrder> orderList = wxOrderServices.getOrderList(2, mDim12Id, storeId, productArea, "Y",
                    startTime, endTime, status, PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "creationdate")));
            int totalSize = wxOrderServices.getCountOrder(2, mDim12Id, storeId, productArea, "Y",
                    startTime, endTime, status);

            return new CommonResult(0, "SUCCESS").withData("requisitionOrder", orderList).withData("totalPage", totalSize % pageSize > 0 ? totalSize/pageSize + 1 : totalSize/pageSize);
        }
//        List<RequisitionOrder> orderList = wxOrderServices.getOrderList(user.getId(), 2, mDim12Id, storeId, productArea, "Y",
//                startTime, endTime, status, PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "creationdate")));
        List<RequisitionOrder> orderList = wxOrderServices.getOrderList(2, mDim12Id, storeId, productArea, "Y",
                startTime, endTime, status, PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "creationdate")));


//        int totalSize = wxOrderServices.getCountOrder(user.getId(), 2, mDim12Id, storeId, productArea, "Y",
//                startTime, endTime, status);
        int totalSize = wxOrderServices.getCountOrder( 2, mDim12Id, storeId, productArea, "Y",
                startTime, endTime, status);
        return new CommonResult(0, "SUCCESS").withData("requisitionOrder", orderList).withData("totalPage", totalSize % pageSize > 0 ? totalSize/pageSize + 1 : totalSize/pageSize);
    }

    @PostMapping("/playerSubmit")
    public CommonResult submitRequisitonOrder(@RequestHeader(value = "jobNum", defaultValue = "-1") String jobNum,
                                              @RequestParam(defaultValue = "-1", name = "reqId") Integer reqId, String message,String picUrl){
        RequisitionUser user = wxUserServices.getUser(jobNum);
        if (null == user) {
            return new CommonResult(1001, "请授权登录");
        }
        if (!StringUtils.hasText(message)) {
            message = "";
        }
        if (!StringUtils.hasText(picUrl)) {
            picUrl = "";
        }
        wxOrderServices.updateRequisionById2(reqId, message,picUrl);
        return new CommonResult(0, "SUCCESS");
    }


    @PostMapping("/playerMdstatusSubmit")
    public CommonResult submitMdstatusOrder(@RequestHeader(value = "jobNum", defaultValue = "-1") String jobNum,
                                              @RequestParam(defaultValue = "-1", name = "reqId") Integer reqId, String mdstatus){
        wxOrderServices.updateMdstatusById(reqId, mdstatus);
        return new CommonResult(0, "SUCCESS");
    }



}
