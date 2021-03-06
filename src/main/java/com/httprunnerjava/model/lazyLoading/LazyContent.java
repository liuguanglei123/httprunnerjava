package com.httprunnerjava.model.lazyLoading;

import com.httprunnerjava.Parse;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;

@Data
@Slf4j
public class LazyContent<T> implements Serializable {

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
        else
            return new LazyContent(obj);
    }

    public Set<String> extractVariables(){
        // extract all variables in content recursively.
        // TODO：这里应该支持加载list set map 等类型的数据的，，但是现在不支持需要后续支持
        if(rawValue instanceof String){
            return Parse.regexFindallVariables((String)rawValue);
        }

        return new HashSet<>();
    }

    //用来计算实际懒加载对象所存储的值，但是实际上对于LazyContent对象来说没啥用，只针对于LazyString类型需要进行计算
    //这里写一个对象也只是为了兼容LazyContent调用的时候不会报错
    public LazyContent parse(Variables variablesMapping, Class<?> functionsMapping) {
        if(rawValue instanceof List){
            for(LazyContent each : (List<LazyContent>)rawValue){
                each.parse(variablesMapping,functionsMapping);
            }
        }else if(rawValue instanceof Map){
            for(Map.Entry<LazyContent,LazyContent> each : ((Map<LazyContent,LazyContent>)rawValue).entrySet()){
                each.getKey().parse(variablesMapping,functionsMapping);
                each.getValue().parse(variablesMapping,functionsMapping);
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
