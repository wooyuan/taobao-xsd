package com.taobao.logistics.config;

import com.taobao.logistics.intercepter.impl.SameUrlDataInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Created by ShiShiDaWei on 2021/11/01.
 */
@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    @Value("${hswing.profile}")
    private String path;


    /**
     * 设置静态资源路径
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/resources", "/META-INF/resources");
//        registry.addResourceHandler("/wx/**")
//                .addResourceLocations("file:E:"+ File.separator + "App_download" + File.separator + "MP_verify_EfpnvJ3nAa2HHE0x.txt");
        registry.addResourceHandler("/wx/img/**")
                .addResourceLocations("file:" + path);
        System.out.println("=====" + System.getProperty("os.name") + "=====");
//        registry.addResourceHandler("/wx/img/**")
//                .addResourceLocations("file:"+System.getProperties().getProperty("user.home") + File.separator + "Pictures/爱壁纸UWP/美女/");
        super.addResourceHandlers(registry);
    }

    /**
     * 支持CORS跨域访问
     */
    @Override
    protected void addCorsMappings(CorsRegistry registry) {
        super.addCorsMappings(registry);
        registry.addMapping("/**")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowedOrigins("*");
    }




    //    @Bean
//    LoginInterceptor loginInterceptor() {
//        return new LoginInterceptor();
//    }

//    @Override
//    protected void addInterceptors(InterceptorRegistry registry) {
//        super.addInterceptors(registry);
//        // addPathPatterns 用于添加拦截规则
//        // excludePathPatterns 用户排除拦截
//        registry.addInterceptor(loginInterceptor())
//                .addPathPatterns("/**")
//                .excludePathPatterns("/img/**", "/err", "/static/**", "/api/login/**", "/wx/**");
//
//    }



}
