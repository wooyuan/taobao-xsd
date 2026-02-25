package com.taobao.logistics.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.taobao.logistics.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converters.add(0, stringConverter);
        
        FastJsonHttpMessageConverter fastJsonConverter = new FastJsonHttpMessageConverter();
        fastJsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
        converters.add(1, fastJsonConverter);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/api/login", "/api/logout", "/api/checkLogin",
                        "/static/**", "/layui/**", "/audio/**");
    }

    @Override
    public void addResourceHandlers(@NonNull org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/layui/**")
                .addResourceLocations("classpath:/static/layui/");
        registry.addResourceHandler("/audio/**")
                .addResourceLocations("classpath:/static/audio/");
    }
}