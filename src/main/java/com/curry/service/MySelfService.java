package com.curry.service;

import com.curry.annotation.MyService;

/**
 * @Author: Curry
 * @Date: 2019/4/21 16:46
 */
@MyService("helloService")
public class MySelfService implements IMySelfService{
    public String hello() {
        return "hello curry。。。。。。";
    }
}
