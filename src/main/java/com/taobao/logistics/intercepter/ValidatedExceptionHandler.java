package com.taobao.logistics.intercepter;

import com.alibaba.fastjson.JSON;
import com.taobao.logistics.utils.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author ShiShiDaWei
 */
@Slf4j
@RestControllerAdvice//增强的 Controller
public class ValidatedExceptionHandler {

    /**
     * 处理 json 请求体调用接口校验失败抛出的异常
     * @param exception
     * @return
     */
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult exceptionHandler(MethodArgumentNotValidException exception) {

        StringBuilder stringBuilder = new StringBuilder();
        BindingResult bindingResult = exception.getBindingResult();
        if (bindingResult.hasErrors()) {
            List<ObjectError> allErrors = bindingResult.getAllErrors();
            if (allErrors.size() != 0) {
                allErrors.forEach(objectError -> {
                    FieldError fieldError= (FieldError) objectError;
                    String field = fieldError.getField();
                    Object value = fieldError.getRejectedValue();
                    String msg = fieldError.getDefaultMessage();
                    String message = String.format("错误字段：%s，错误值：%s，原因：%s；", field, value, msg);

                    log.warn("Bad Request Parameters: dto entity [{}],field [{}],message [{}]",fieldError.getObjectName(), fieldError.getField(), fieldError.getDefaultMessage());
                    stringBuilder.append(message).append("\r\n");
                });
            }
        }
        return new CommonResult(HttpStatus.BAD_REQUEST.value(), stringBuilder.toString());
    }


    //处理 form data方式调用接口校验失败抛出的异常
    @ExceptionHandler(BindException.class)
    public CommonResult bindExceptionHandler(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        List<String> collect = fieldErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList());
        return new CommonResult(-1, JSON.toJSONString(collect));
    }

    //  处理 json 请求体调用接口校验失败抛出的异常
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public CommonResult methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
//        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
//        List<String> collect = fieldErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList());
//        return new CommonResult(HttpStatus.BAD_REQUEST.value(), JSON.toJSONString(collect));
//    }

    //  处理单个参数校验失败抛出的异常
    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResult constraintViolationExceptionHandler(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        List<String> collect = constraintViolations.stream().map(o -> o.getMessage()).collect(Collectors.toList());
        return new CommonResult(-1, JSON.toJSONString(collect));

    }

    // 处理以上处理不了的其他异常
    @ExceptionHandler(Exception.class)
    public CommonResult exceptionHandler(Exception e) {
        return new CommonResult(-1, e.getMessage());
    }





}

