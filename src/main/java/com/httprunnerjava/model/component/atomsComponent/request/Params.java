package com.httprunnerjava.model.component.atomsComponent.request;

import com.httprunnerjava.model.component.intf.ParseAble;
import com.httprunnerjava.model.lazyLoading.LazyContent;
import com.httprunnerjava.model.lazyLoading.LazyString;
import com.httprunnerjava.utils.CommonUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class Params implements Serializable, ParseAble {

    private HashMap<String, LazyContent<?>> content = new HashMap<>();

    public Params(){}

    public Params(String str){
        this(CommonUtils.parseJsonStrToMap(str));
    }

    public Params(Map<Object,Object> rawParams) {
        if(rawParams == null || rawParams.size() == 0){
            return;
        }
        
        for (Map.Entry<Object,Object> entry : rawParams.entrySet()) {
            if (entry.getValue() instanceof String )
                content.put(String.valueOf(entry.getKey()), new LazyString(String.valueOf(entry.getValue())));
            else
                content.put(String.valueOf(entry.getKey()), new LazyContent<>(entry.getValue()));
        }
    }

    public Params parse(Variables variables_mapping,Class function) {
        if(this.content == null || this.content.size() == 0)
            return this;

        for(LazyContent<?> value : content.values()){
            if(value instanceof LazyString)
                ((LazyString)value).parse(variables_mapping, function);
        }

        return this;
    }

    public void update(Params params){
        this.getContent().putAll(params.getContent());
    }
}