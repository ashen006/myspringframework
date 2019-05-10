package com.curry.controller;

import com.curry.annotation.MyAutowrited;
import com.curry.annotation.MyController;
import com.curry.annotation.MyQualifier;
import com.curry.annotation.MyRequestMapping;
import com.curry.service.IMySelfService;

/**
 * @Author: Curry
 * @Date: 2019/4/21 16:42
 */
@MyController
@MyRequestMapping("/hello")
public class MySelfController {

    @MyAutowrited
    @MyQualifier("helloService")
    private IMySelfService iMySelfService;

    @MyRequestMapping("/sayHello")
    public String sayHello(){
        /**
         * 具体的业务处理逻辑
         */
        System.out.println(iMySelfService.hello());
        return "hello";
    }
}
