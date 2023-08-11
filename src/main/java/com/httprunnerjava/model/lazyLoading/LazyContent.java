package com.httprunnerjava.model.lazyLoading;

import com.httprunnerjava.Parse;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import com.httprunnerjava.model.component.intf.ParseAble;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;

@Data
@Slf4j
public class LazyContent<T> implements Serializable, ParseAble {

    // 懒加载内容的原值
    public T rawValue;

    public LazyContent(T t){
        this.rawValue = t;
    }

    public static LazyContent loadObject(Object obj){
        if (obj instanceof String)
            return new LazyString(String.valueOf(obj));
        else if(obj instanceof LazyContent)
            return (LazyContent)obj;
        else if(obj instanceof List){
            List<Object> varList =  new ArrayList<>();
            for (Object each : (List)obj)
                varList.add(loadObject(each));
            return new LazyContent(varList);
        }
        else if(obj instanceof Map){
            //TODO：低优先级 感觉在实际的请求中，这里Map的key都会是String，所以下面的map能不能改成String？
            Map<Object,Object> varMap= new HashMap<>();
            for(Map.Entry each :((Map<?, ?>) obj).entrySet()){
                varMap.put(loadObject(each.getKey()),loadObject(each.getValue()));
            }
            return new LazyContent(varMap);
        }
        else {
            return new LazyContent(obj);
        }
    }

    /**
     * 递归提取解析的字符串中的变量名，比如this is $foo1, and that is $foo2，经过解析后会返回foo1 foo2
     * @return 从string中解析出来的变量名
     * TODO:TEST 需要验证一下一个string中包含多个变量的场景
     */
    public Set<String> extractVariables(){
        // info：原版这里是支持list map等多种数据类型的，但是因为java的强类型原因，这里不好支持，暂时不做了，后续看情况支持
        if(rawValue instanceof String){
            return Parse.regexFindallVariables((String)rawValue);
        }

        return new HashSet<>();
    }

    /**
     * 用来计算实际懒加载对象所存储的值，对于LazyContent类型的值，会进入如下逻辑，对于LazyString类型的值，会进入LazyString中重写的parse方法
     * @param variablesMapping 加载过程中用到的变量集合
     * @param functionsMapping 自定义方法
     * @return 返回LazyContent本身，对于LazyString类型的变量来说，其parsedValue等值是计算过的
     */
    public LazyContent<T> parse(Variables variablesMapping, Class<?> functionsMapping) {
        if (rawValue instanceof List) {
            for (LazyContent<T> each : (List<LazyContent<T>>) rawValue) {
                each.parse(variablesMapping, functionsMapping);
            }
        } else if (rawValue instanceof Map) {
            for (Map.Entry<LazyContent<T>, LazyContent<T>> each : ((Map<LazyContent<T>, LazyContent<T>>) rawValue).entrySet()) {
                each.getKey().parse(variablesMapping, functionsMapping);
                each.getValue().parse(variablesMapping, functionsMapping);
            }
        }

        return this;
    }

    /**
     * 获得LazyContent对象的实际值，如果对象是LazyString类型，返回其实际值realvalue，否则返回raw_value
     */
    public Object getEvalValue(){
        if(rawValue instanceof List){
            List<Object> result = new ArrayList<>();
            for(LazyContent each : (List<LazyContent>)rawValue){
                result.add(each.getEvalValue());
            }
            return result;
        }else if(rawValue instanceof Map){
            for(Map.Entry<Object,Object> one : ((Map<Object,Object>) rawValue).entrySet()){
                if(one.getKey() instanceof String && !(one.getValue() instanceof LazyContent))
                    return this.rawValue;
                else
                    break;
            }
            Map<Object,Object> result = new HashMap<>();
            for(Map.Entry<LazyContent,LazyContent> each : ((Map<LazyContent,LazyContent>)rawValue).entrySet()){
                result.put(each.getKey().getEvalValue(),each.getValue().getEvalValue());
            }
            return result;
        }else
            return getRawValue();
    }

}
