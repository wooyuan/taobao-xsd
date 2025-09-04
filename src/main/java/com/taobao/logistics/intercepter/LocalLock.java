package com.taobao.logistics.intercepter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by ShiShiDaWei on 2021/11/2.
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
//是被用来指定自定义注解是否能随着被定义的java文件生成到javadoc文档当中
@Documented
@Inherited
public @interface LocalLock {

    String key() default "";

    /**
     * 过期时间 集成 redis 需要用到
     * @return
     */
    int expire() default 5;

}
