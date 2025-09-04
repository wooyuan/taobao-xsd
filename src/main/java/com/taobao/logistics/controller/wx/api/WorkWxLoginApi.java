package com.taobao.logistics.controller.wx.api;

import com.alibaba.fastjson.JSONObject;
import com.taobao.logistics.config.WorkWxConfig;
import com.taobao.logistics.entity.wx.RequisitionOrder;
import com.taobao.logistics.entity.wx.RequisitionUser;
import com.taobao.logistics.service.wx.WxApiServices;
import com.taobao.logistics.service.wx.WxOrderServices;
import com.taobao.logistics.utils.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by ShiShiDaWei on 2021/10/27.
 */
@Slf4j
@Controller
@RequestMapping("/wx/api")
public class WorkWxLoginApi {

    private static final String ERROR = "errMsg";

    @Autowired
    private WxApiServices wxApiServices;

    @Autowired
    private WxOrderServices wxOrderServices;



    /**
     * 获取WX的返回code
     * @param code
     * @return
     */
    @RequestMapping(value = "receiveCode")
    @ResponseBody
    public CommonResult receiveCode(String code, @RequestParam(defaultValue = "", name = "agentId") String agentId, String flag, HttpServletRequest request,
                                    @RequestParam(defaultValue = "1", name = "page") Integer page, @RequestParam(defaultValue = "10", name = "pageSize")Integer pageSize,
                                    @RequestParam(defaultValue = "1", name = "orderState") Integer orderState) {
        log.info("WeiXin Return -------code={}, flag={}", code, flag);

        String nowParams = JSONObject.toJSONString(request.getParameterMap());
        Map<String, Object> nowDataMap = new HashMap<String, Object>();
        nowDataMap.put("repeatParams", nowParams);
        nowDataMap.put("repeatTime", System.currentTimeMillis());
        log.debug("receiveCode================={}", JSONObject.toJSONString(nowDataMap));

        if (!StringUtils.hasLength(code)) {
            return new CommonResult(-1, "code不能未空");
        }
        if (!WorkWxConfig.WORK_WX_AgentId.equalsIgnoreCase(agentId)) {
            return new CommonResult(-1, "参数不正确");
        }
        String accessToken = wxApiServices.getWxAccessToken(agentId);
        if (accessToken.contains(ERROR)) {
            return new CommonResult(-1, accessToken.replaceAll(ERROR, ""));
        }
        RequisitionUser wxUserInfo = wxApiServices.getWxUserInfo(code, accessToken);
        if (null == wxUserInfo) {
            return new CommonResult(-1, "获取企业微信身份错误!");
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "creationdate");
        Pageable pageable = PageRequest.of(page-1, pageSize, sort);
        List<RequisitionOrder> order = wxOrderServices.getOrderList(wxUserInfo.getId(), orderState, pageable);
        return new CommonResult(0, "SUCCESS").withData("userInfo", wxUserInfo).withData("order", order);
    }







    @GetMapping(value = {"test"}, headers = {"jobNum"})
    @ResponseBody
    public String getUser(String jobNum, @PathVariable(required = false, name = "id") Integer id){
        System.out.println("jobNum = " + jobNum + ", id = " + id);
//        GoodsPic goodsPic = wxOrderServices.getGoodsPic("");
//        System.out.println("goodsPic = " + goodsPic.toString());

//        return wxApiServices.saveUser(jobNum);
        return ("jobNum = " + jobNum + ", id = " + id);
    }

    public static void main(String[] args) {
        System.out.println(StringUtils.hasText(null));
        System.out.println(StringUtils.hasText(""));
        System.out.println(StringUtils.hasLength(""));
        System.out.println(StringUtils.hasLength(null));

        HashMap<String, Object> map = new HashMap<>();
        map.put("滴一", "diyi ");
        map.put("滴二", "dier ");
        map.put("滴三", "disan ");
        Stream<HashMap<String, Object>> map1 = Stream.of(map);
        System.out.println(JSONObject.toJSONString(new CommonResult(1, "success").withData("111", 11).withData("list", map1.toArray())));
        RequisitionUser object = new RequisitionUser();
        object.setMobile("123");
        System.out.println(JSONObject.toJSONString(object));

    }




}
