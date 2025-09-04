package com.taobao.logistics.controller;

import com.taobao.logistics.service.OrderWaybillServices;
import com.taobao.logistics.service.WingServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author ShiShiDaWei
 * @date 2021/8/14
 查询伯俊数据电子面单接口
 */
@Slf4j
@Controller
@RequestMapping(value = "/code",method={RequestMethod.POST,RequestMethod.GET})
public class IndexController {
    private static final String url = "https://oauth.taobao.com/authorize?response_type=code&client_id=32976438&redirect_uri=http://cainiao.enjoyme.cn:9213/code/find&state=111222&view=web";

    @Autowired
    private OrderWaybillServices orderWaybillServices;


    @GetMapping(path = "/login/{logisticCode}/{branchCode}/{orderId}/{taobaoStoreName}/{isRedirect}/{senderPhone}/{key}")
    public String index(Model model, RedirectAttributes attributes, @PathVariable(name = "logisticCode") String logisticCode,
                        @PathVariable(value = "branchCode") String branchCode, @PathVariable(value = "taobaoStoreName") String taobaoStoreName,
                        @PathVariable(value = "orderId") String orderId, @PathVariable(value = "isRedirect") Integer isRedirect,
                        @PathVariable(value = "senderPhone") String senderPhone, @PathVariable(value = "key") String key){
        System.out.println("model = [" + model + "], logisticCode = [" + logisticCode + "], branchCode = [" + branchCode + "], taobaoStoreName = [" + taobaoStoreName + "], orderId = [" + orderId + "]");
        log.info("model = [" + model + "], logisticCode = [" + logisticCode + "], branchCode = [" + branchCode + "], taobaoStoreName = [" + taobaoStoreName + "], orderId = [" + orderId + "]");
        if (!StringUtils.hasLength(logisticCode) || !StringUtils.hasLength(branchCode)
                || !StringUtils.hasLength(taobaoStoreName) || !StringUtils.hasLength(orderId)) {
            return "err";
        }
        //http://cainiao.enjoyme.cn:9213/code/login/ZTO/55156/140449/mday旗舰店/0/15055190645/32976438
        String str = logisticCode + "_" + branchCode + "_" + orderId + "_" + taobaoStoreName + "_" + senderPhone + "_" + key;
        if (isRedirect == 0) {
            attributes.addAttribute("state", str);
            attributes.addAttribute("code", "");
            attributes.addAttribute("appkey", key);
            return "redirect:/code/find";
        }
        model.addAttribute("redirect_url", url.replace("111222", str));
        return "index_taobao";
    }



    @RequestMapping(value = "/print.html")
    ModelAndView printWaybill(Integer orderId, String waybillCode) {
//        System.out.println("OrderId = [" + orderId + "], waybillCode = [" + waybillCode + "]");
        ModelAndView print = new ModelAndView();
        if (null == orderId) {
            print.setViewName("err");
            return print;
        }
        String printmessage = orderWaybillServices.getOrderInfo(orderId);
        print.addObject("printmessage", printmessage);
        print.setViewName("print_test");
        return print;
    }

//获取打印模板
    @RequestMapping(value = "/getTemplates")
    public String  getTemplates() {
        String tmp="";
        try{
            WingServices n =new WingServices();
            tmp=n.getTemplates();
        } catch (Exception e) {
            System.out.println("测试"+e.toString());
        }
        return tmp;
    }







}
