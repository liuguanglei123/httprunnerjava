package com.httprunnerjava.model.component.atomsComponent.response;

import com.alibaba.fastjson.JSONArray;
import com.httprunnerjava.exception.HrunExceptionFactory;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Validator {
    private String checkItem;
    private Object expectValue;
    private String message;
    private String comparator;
    private String excludeItemStr;
    private List<String> excludeItem;

    public Validator(String comparator,String checkItem,Object expectValue,String message){
        this.comparator = comparator;
        this.checkItem = checkItem;
        this.expectValue = expectValue;
        this.message = message;
    }

    public Validator(String comparator,String checkItem,Object expectValue,String message, String excludeItemStr){
        this.comparator = comparator;
        this.checkItem = checkItem;
        this.expectValue = expectValue;
        this.message = message;
        this.excludeItemStr = excludeItemStr;
        excludeItem = new ArrayList<>();
        if(excludeItemStr.startsWith("[") && excludeItemStr.endsWith("]")){
            try {
                excludeItem = JSONArray.parseArray(excludeItemStr, String.class);
            }catch (Exception e){
                HrunExceptionFactory.create("E00007");
            }
        }
        else{
            //TODO:如果这里是变量的话，还需要解析一下的
            excludeItem.add(excludeItemStr);
        }
    }
}

