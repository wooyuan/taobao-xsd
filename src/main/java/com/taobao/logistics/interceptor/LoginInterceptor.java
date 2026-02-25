package com.taobao.logistics.interceptor;

import com.taobao.logistics.config.LoginConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private LoginConfig loginConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!loginConfig.isEnabled()) {
            return true;
        }

        String requestURI = request.getRequestURI();
        
        String contextPath = request.getContextPath();
        String loginPage = contextPath + "/login";
        
        if (requestURI.equals(loginPage) || 
            requestURI.startsWith("/api/login") || 
            requestURI.startsWith("/static/") ||
            requestURI.startsWith("/layui/") ||
            requestURI.startsWith("/audio/") ||
            requestURI.endsWith(".css") ||
            requestURI.endsWith(".js") ||
            requestURI.endsWith(".png") ||
            requestURI.endsWith(".jpg") ||
            requestURI.endsWith(".gif") ||
            requestURI.endsWith(".ico") ||
            requestURI.endsWith(".woff") ||
            requestURI.endsWith(".woff2") ||
            requestURI.endsWith(".ttf") ||
            requestURI.endsWith(".eot") ||
            requestURI.endsWith(".svg") ||
            requestURI.endsWith(".mp3")) {
            return true;
        }

        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");

        if (user != null) {
            return true;
        }

        if (requestURI.startsWith("/api/")) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录，请先登录\"}");
        } else {
            response.sendRedirect(loginPage);
        }
        
        return false;
    }
}
