package com.httprunnerjava.model.component.atomsComponent.request;


import com.alibaba.fastjson.JSON;
import com.httprunnerjava.exception.HrunExceptionFactory;
import com.httprunnerjava.exception.ParseError;
import com.httprunnerjava.exception.VariableNotFound;
import com.httprunnerjava.model.component.moleculesComponent.TRequest;
import com.httprunnerjava.model.lazyLoading.LazyContent;
import com.httprunnerjava.model.lazyLoading.LazyString;
import com.httprunnerjava.utils.CommonUtils;
import com.httprunnerjava.utils.JsonUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

//TODO: variables 还有一种表现形式是直接使用${gen_variables()}这种方式获取的，需要特殊处理实现
@Data
@NoArgsConstructor
@Slf4j
public class Variables implements Serializable {
    public HashMap<String, LazyContent> content = new HashMap<String, LazyContent>();

    //TODO：低优先级 这里单独存放了request 和 resp ，后面看看能否优化一下
    //    public Request request;
    //    public Response resp;

    public Variables(String str){
        this(JsonUtils.parseJsonStrToMap(str));
    }

    public Variables(Map<String,Object> raw_variables) {
        for (Map.Entry entry : raw_variables.entrySet()) {
            if (entry.getValue() instanceof String)
                content.put(String.valueOf(entry.getKey()), new LazyString(String.valueOf(entry.getValue())));
            else if(entry.getValue() instanceof LazyContent)
                content.put(String.valueOf(entry.getKey()),(LazyContent)entry.getValue());
            else if(entry.getValue() instanceof List)//可能list中的每个值都是laztString，所以要用loadObject进行识别判断
                content.put(String.valueOf(entry.getKey()), LazyContent.loadObject(entry.getValue()));
            else if(entry.getValue() instanceof Map)//可能list中的每个值都是laztString，，所以要用loadObject进行识别判断
                content.put(String.valueOf(entry.getKey()), LazyContent.loadObject(entry.getValue()));
            else
                content.put(String.valueOf(entry.getKey()), new LazyContent(entry.getValue()));
        }
    }

    public Variables update(final Variables anotherVar){
        Optional.ofNullable(anotherVar).ifPresent( a -> {
            this.getContent().putAll(anotherVar.getContent());
        });
        return this;
    }

    public Variables update(String key, Object value){
        this.content.put(key, new LazyContent(value));
        return this;
    }

    public Variables update(Map param){
        Optional.ofNullable(param).ifPresent(
                a -> this.getContent().putAll(new Variables(param).getContent())
        );
        return this;
    }

    public Variables putIfAbsent(Map<String,Object> param){
        Optional.ofNullable(param).ifPresent( p -> {
                    param.entrySet().forEach(e -> {
                        if (e.getValue() instanceof String)
                            this.getContent().putIfAbsent(e.getKey(), new LazyString(String.valueOf(e.getValue())));
                        else if (e.getValue() instanceof LazyContent)
                            this.getContent().putIfAbsent(e.getKey(), (LazyContent) e.getValue());
                        else if (e.getValue() instanceof LazyString)
                            this.getContent().putIfAbsent(e.getKey(), (LazyString) e.getValue());
                        else
                            this.getContent().putIfAbsent(e.getKey(), new LazyContent(e.getValue()));
                    });
        });
        return this;
    }

    public Integer size(){
        return this.content.size();
    }

    public Set<String> keySet(){
        return this.content.keySet();
    }

    public LazyContent<?> get(String key){
        return this.content.get(key);
    }

    public Variables parse() throws ParseError {
        return parse(null);
    }

    //TODO:FunctionsMapping未实现
    /**
     * parse_variables_mapping方法的作用是什么？
     *         比如传入的是  {"foo1": "testcase_config_bar1", "foo2": "testcase_config_bar2", "foo3": "this is $foo2"}
     *         foo3引用了foo2变量，最终会解析，返回这样的数据
     *         {"foo1": "testcase_config_bar1", "foo2": "testcase_config_bar2", "foo3": "this is testcase_config_bar2"}
     *
     *
     */
    public Variables parse(Class<?> functionsMapping) throws ParseError {
        Variables parsedVariables = new Variables();

        while(!Objects.equals(parsedVariables.size(), size())){
            for(String varName : keySet()){
                if(parsedVariables.keySet().contains(varName)) {
                    continue;
                }

                LazyContent<?> varValue = get(varName);
                Set<String> varKeys = varValue.extractVariables();

                if (varKeys.contains(varName)) {
                    log.error("参数" + varName + "存在重复包含，无法继续执行");
                    HrunExceptionFactory.create("E10001");
                }

                List<String> notDefinedVariables =
                        varKeys.stream().filter( e ->
                                !keySet().contains(e)
                            ).collect(Collectors.toList());

                if(!notDefinedVariables.isEmpty()){
                    log.error("参数:" + varName + " 不存在，请确认！！！");
                    HrunExceptionFactory.create("E10002");
                }

                Object parsedValue = null;
                try{
                    parsedValue = varValue.parse(parsedVariables,functionsMapping).getEvalValue();
                }catch(VariableNotFound e){
                    continue;
                }

                // TODO:其实这里的逻辑有点重复，以后可以进行调整，在前面的逻辑中，一个LazyString实际上已经被解析完成了，
                //  但是这里又重新定义了一次，下次如果需要用到这个变量，又要重新解析一次了
                if(parsedValue instanceof Map || parsedValue instanceof List) {
                    parsedVariables.put(varName, JSON.toJSONString(parsedValue));
                } else {
                    parsedVariables.put(varName, parsedValue);
                }
            }
        }

        return parsedVariables;
    }

    // 支持不同的set方法，向content中put新值
    // 比如直接传入一个新的map
    public void put(Map<String,Object> raw_variables){
        for(Map.Entry<String,Object> entry : raw_variables.entrySet()){
            if (entry.getValue() instanceof String)
                content.put(entry.getKey(), new LazyString(String.valueOf(entry.getValue())));
            else
                content.put(entry.getKey(), new LazyContent(entry.getValue()));
        }
    }

    // 或者传入一对key和value
    public void put(String key, Object value){
        Map tmpMap = new HashMap<String,Object>();
        tmpMap.put(key,value);
        put(tmpMap);
    }

    public static Variables mergeVariables(Variables variablesToBeOverridden, Variables variables){
        Variables variables1 = CommonUtils.deepcopy_obj(variablesToBeOverridden);
        Variables variables2 = CommonUtils.deepcopy_obj(variables);
        variables1.update(variables2);
        return variables1;
    }

    public Map<String,Object> toMap(){
        Map<String,Object> result = new HashMap<>();
        for(Map.Entry<String,LazyContent> each : this.getContent().entrySet()){
            result.put(each.getKey(),each.getValue().getEvalValue());
        }
        return result;
    }
}
