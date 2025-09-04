package com.taobao.logistics.controller.wx.controller;

import com.alibaba.fastjson.JSONObject;
import com.taobao.logistics.entity.wx.GoodsPic;
import com.taobao.logistics.entity.wx.RequisitionOrder;
import com.taobao.logistics.entity.wx.RequisitionUser;
import com.taobao.logistics.intercepter.RepeatSubmit;
import com.taobao.logistics.service.wx.WxOrderServices;
import com.taobao.logistics.service.wx.WxUserServices;
import com.taobao.logistics.utils.CommonResult;
import com.taobao.logistics.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ShiShiDaWei on 2021/11/1.
 */
@Slf4j
@Validated
@RestController
@RequestMapping(value = "/wx/requisition")
public class RequisitionFormController {

    @Value("${hswing.linux-upload}")
    private String LINUX_UPLOAD_PATH;

    @Value("${hswing.profile}")
    private String LOCAL_PATH;

    @Value("${hswing.os-name}")
    private String OS_NAME;


    @Autowired
    private WxOrderServices wxOrderServices;

    @Autowired
    private WxUserServices wxUserServices;



    @PostMapping("upload")
    public CommonResult uploadFile(MultipartFile[] files){
        if (files.length == 0) {
            return new CommonResult(-1, "No File");
        }
        List<GoodsPic> uploadFileMD5 = FileUtils.getUploadFileMD5(files);
        if (uploadFileMD5.size() == 0) {
            return new CommonResult(-1, "上传图片过大！");
        }
        ArrayList<GoodsPic> pics = new ArrayList<>();
        for (int i = 0; i < uploadFileMD5.size(); i++) {
            GoodsPic pic = uploadFileMD5.get(i);
            String picMD5 = pic.getGoodsPicMD5();
            int count = wxOrderServices.getCountFile(picMD5);
            if (count > 0) {
                GoodsPic goodsPic = wxOrderServices.getGoodsPic(picMD5);
                pics.add(goodsPic);
                continue;
            }
//            List<GoodsPic> goodsPics = Stream.of(file).collect(Collectors.toList());
            MultipartFile f = files[i];
            if (f.isEmpty() || f.getSize() == 0) {
                return new CommonResult(-1, "No File");
            }
            String path;
            if (OS_NAME.toLowerCase().contains("win")) {
                path = LOCAL_PATH;
            }else {
                path = LINUX_UPLOAD_PATH;
            }
            GoodsPic gp = FileUtils.uploadFile(f, path);
            GoodsPic goodsPic = wxOrderServices.uploadFile(gp);
            pics.add(goodsPic);
        }
        return new CommonResult(0, "SUCCESS").withData("pictures", pics);
    }

    @PostMapping("uploadFh")
    public CommonResult uploadFileFh(MultipartFile[] files){
        if (files.length == 0) {
            return new CommonResult(-1, "No File");
        }
        List<GoodsPic> uploadFileMD5 = FileUtils.getUploadFileMD5(files);
        if (uploadFileMD5.size() == 0) {
            return new CommonResult(-1, "上传图片过大！");
        }
        ArrayList<GoodsPic> pics = new ArrayList<>();
        for (int i = 0; i < uploadFileMD5.size(); i++) {
            GoodsPic pic = uploadFileMD5.get(i);
            String picMD5 = pic.getGoodsPicMD5();
            MultipartFile f = files[i];
            if (f.isEmpty() || f.getSize() == 0) {
                return new CommonResult(-1, "No File");
            }
            String path;
            if (OS_NAME.toLowerCase().contains("win")) {
                path = LOCAL_PATH;
            }else {
                path = LINUX_UPLOAD_PATH;
            }
            GoodsPic gp = FileUtils.uploadFile(f, path);
            //GoodsPic goodsPic = wxOrderServices.uploadFile(gp);
            pics.add(gp);
        }
        return new CommonResult(0, "SUCCESS").withData("pictures", pics);
    }


    @RepeatSubmit
    @RequestMapping("search")
    public CommonResult searchProduct(@Validated @Pattern(regexp = "^[1-9]\\d{1,13}$", message = "条码无效") String no) {
//        if (!StringUtils.hasLength(no)) {
//            return new CommonResult(-1, "no is Null");
//        }
        RequisitionOrder requisitionOrder;
        try {
            requisitionOrder = wxOrderServices.getProduct(no);
            if (null == requisitionOrder) {
                return new CommonResult(-1, "No Data");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return new CommonResult(-1, "No Data");
        }
        return new CommonResult(0, "SUCCESS").withData("requisition", requisitionOrder);
    }


    @RepeatSubmit
    @PostMapping("create")
    public CommonResult createRequisition(@RequestHeader(value = "jobNum", defaultValue = "-1") String jobNum, RequisitionOrder requisitionOrder, String s,
                                          @RequestParam(value = "picIds", required = false) List<Integer> picIds) {
        System.out.println("jobNum = [" + jobNum + "], requisitionOrder = [" + JSONObject.toJSONString(requisitionOrder) + "], picIds = [" + picIds + "]");
        int countUser = wxUserServices.getCountUser(jobNum);
        if (countUser < 1) {
//            return new AjaxResult(AjaxResult.Type.ERROR, "User is Null");
            return new CommonResult(-1, "User is Null");
        }
        RequisitionUser user = wxUserServices.getUser(jobNum);
        RequisitionOrder reqOrder = wxOrderServices.getProduct(requisitionOrder.getProductNo());
        requisitionOrder.setReqUserId(user.getId());
        requisitionOrder.setStatus(1);
        requisitionOrder.setStoreId(user.getStoreId());
        requisitionOrder.setModifieddate(new Date());
        requisitionOrder.setmDim12Id(reqOrder.getmDim12Id());
        requisitionOrder.setmProductId(reqOrder.getmProductId());
        requisitionOrder.setmAttributesetinstanceId(reqOrder.getmAttributesetinstanceId());
        requisitionOrder.setmSize(reqOrder.getmSize());
        requisitionOrder.setStoreCode(user.getStoreCode());
        requisitionOrder.setStorename(user.getStoreName());//add otteryuan
        requisitionOrder.setUsername(user.getName());//add otteryuan
        requisitionOrder.setMdstatus("未收货");//add otteryuan 收货状态

//        requisitionOrder.setCreationdate(new Date());
//        if (StringUtils.hasText(s)) {
//            JSONArray objects = JSONObject.parseArray(s);
//            List<Integer> integers = objects.toJavaList(Integer.class);
//            List<GoodsPic> picList2 = wxOrderServices.getGoodsPicList(integers);
//            requisitionOrder.setPicList(picList2);
//        }
        RequisitionOrder re = wxOrderServices.createRequisiontionOrder(requisitionOrder);
//        return AjaxResult.success(re);
        return new CommonResult(0, "SUCCESS").withData("requisitionOrder", re);
    }


    /**
     * 创建完成查询列表
     * @param jobNum
     * @return
     */
    @GetMapping("list")
    public CommonResult getRequisitionOrderList(@RequestHeader(value = "jobNum", defaultValue = "-1") String jobNum){
        RequisitionUser user = wxUserServices.getUser(jobNum);
        if (null == user) {
            return new CommonResult(1001, "请授权登录");
        }
        List<RequisitionOrder> orderList = wxOrderServices.getOrderList(user.getId(), 1);
        return new CommonResult(0, "SUCCESS").withData("requisitionOrder", orderList);
    }


    @PostMapping("submit/{orderState}")
    public CommonResult submitRequisition(@RequestHeader(value = "jobNum", defaultValue = "-1") String jobNum, @PathVariable(value = "orderState") Integer orderState,
                                          @RequestParam(value = "orderIds", required = false) List<Integer> orderIds) {
        System.out.println("orderState = [" + orderState + "], orderId = [" + JSONObject.toJSONString(orderIds) + "]");
        if (null == orderIds) {
            RequisitionUser user = wxUserServices.getUser(jobNum);
            if (null == user) {
                return new CommonResult(1001, "请授权登录");
            }
            Integer countOrder = wxOrderServices.getCountOrder(user.getId(), 1, "Y");
            System.out.println(countOrder);
            orderIds = wxOrderServices.getUnOrderList(user.getId(), 1);
        }
        if (orderState == 2) {
            wxOrderServices.submitRequisitionOrder(orderState, orderIds);
        }
//            for (Integer orderId :
//                    orderIds) {
//                wxOrderServices.submitRequisitionOrder(orderState, orderId);
//            }
        return new CommonResult(0, "SUCCESS");
    }


    @PostMapping("delete/{isactive}")
    public CommonResult submitRequisition(@PathVariable(value = "isactive") String isactive, Integer orderId) {
        System.out.println("isactive = [" + isactive + "], orderId = [" + orderId + "]");
        if (null != orderId && "N".equalsIgnoreCase(isactive)) {
            wxOrderServices.deleteRequisitionOrder(isactive.toUpperCase(), orderId);
        }
        return new CommonResult(0, "SUCCESS");
    }


    @GetMapping("pageable")
    public CommonResult getList() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createDate");
        List<GoodsPic> picList = wxOrderServices.getGoodsPicList(PageRequest.of(1, 1, sort));
        List<GoodsPic> picList2 = wxOrderServices.getGoodsPicList(PageRequest.of(2, 1, sort));
        return new CommonResult(0, "SUCCESS").withData("item1", picList).withData("item2", picList2);
    }


    public static void main(String[] args) throws Exception {
        File file = new File("C:\\Users\\ShiShiDaWei\\Desktop\\123123.txt");
        String fileName = file.getName();
        Double l = file.length() / 1024D;
        System.out.println("文件"+fileName+"的大小是："+ l);

    }




}
