package com.httprunnerjava.model.component.atomsComponent.request;

import com.httprunnerjava.model.component.intf.ParseAble;
import com.httprunnerjava.model.lazyLoading.LazyContent;
import com.httprunnerjava.model.lazyLoading.LazyString;
import com.httprunnerjava.utils.CommonUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class Headers implements Serializable, ParseAble {
    private HashMap<String, LazyContent> content = new HashMap<>();

    public Headers(){}

    public Headers(String str){
        this(CommonUtils.parseJsonStrToMap(str));
    }

    public Headers(Map<Object,Object> rawHeaders) {
        if(rawHeaders == null || rawHeaders.size() == 0){
            return;
        }

        for (Map.Entry entry : rawHeaders.entrySet()) {
            if (entry.getValue() instanceof String) {
                if (String.valueOf(entry.getKey()).toLowerCase().contains("content-type") || String.valueOf(entry.getKey()).toLowerCase().contains("contenttype"))
                    content.put("Content-Type", new LazyString(String.valueOf(entry.getValue())));
                else
                    content.put(String.valueOf(entry.getKey()), new LazyString(String.valueOf(entry.getValue())));
            }
            else
                content.put(String.valueOf(entry.getKey()), new LazyContent(entry.getValue()));
        }
    }

    public Headers(okhttp3.Headers rawHeaders){
        rawHeaders.forEach( each -> {
            if(each.getSecond() instanceof String) {
                content.put(String.valueOf(each.getFirst()), new LazyString(String.valueOf(each.getSecond())));
            } else {
                content.put(String.valueOf(each.getFirst()), new LazyContent(each.getSecond()));
            }
        });
    }

    public Headers parse(Variables variablesMapping, Class function) {
        if(this.content == null || this.content.size() == 0) {
            return this;
        }

        for(LazyContent value : content.values()){
            if(value instanceof LazyString) {
                ((LazyString) value).parse(variablesMapping, function);
            }
        }
        return this;
    }

    public Boolean isEmpty(){
        return (content == null || content.size() == 0);
    }

    public Map<String,String> toMap(){
        Map<String,String> headerMap = this.content.entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey(), entry -> String.valueOf(Optional.ofNullable(entry.getValue().getEvalValue()).orElse(""))
                )
        );

        return headerMap;
    }

    public void update(Headers headers){
        this.getContent().putAll(headers.getContent());
    }

    public void set(String key,String value){
        this.content.putIfAbsent(key,new LazyString(value));
    }

    @Override
    public String toString(){
        StringBuffer result = new StringBuffer("{\n");
        this.getContent().forEach( (k,v) -> result.append("    " + k + ":" + v.getEvalValue() + "\n"));
        result.append("\n}");
        return result.toString();
    }
}
