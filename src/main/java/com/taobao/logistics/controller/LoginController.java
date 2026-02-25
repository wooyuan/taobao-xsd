package com.taobao.logistics.controller;

import com.taobao.logistics.config.LoginConfig;
import com.taobao.logistics.utils.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private LoginConfig loginConfig;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public AjaxResult login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        if (!loginConfig.isEnabled()) {
            return AjaxResult.success("登录验证已禁用");
        }

        if (loginConfig.validateUser(username, password)) {
            session.setAttribute("user", username);
            session.setMaxInactiveInterval(loginConfig.getSessionTimeout() * 60);
            return AjaxResult.success("登录成功");
        } else {
            return AjaxResult.error("用户名或密码错误");
        }
    }

    @PostMapping("/api/logout")
    @ResponseBody
    public AjaxResult logout(HttpSession session) {
        session.removeAttribute("user");
        session.invalidate();
        return AjaxResult.success("退出成功");
    }

    @GetMapping("/api/checkLogin")
    @ResponseBody
    public AjaxResult checkLogin(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user != null) {
            return AjaxResult.success("已登录", user);
        } else {
            return AjaxResult.error("未登录");
        }
    }
}
