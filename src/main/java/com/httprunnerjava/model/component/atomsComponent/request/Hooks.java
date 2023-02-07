package com.httprunnerjava.model.component.atomsComponent.request;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.httprunnerjava.exception.HrunExceptionFactory;
import com.httprunnerjava.model.Enum.HookType;
import com.httprunnerjava.model.lazyLoading.LazyString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class Hooks {

    private List<HookString> content = new ArrayList<>();

    public void add(String rawHook){
        try {
            JSONObject parsedStr = JSONObject.parseObject(rawHook);
            if(parsedStr instanceof Map && parsedStr.size() == 1) {
                HookString hookString = new HookString(HookType.MapHook, parsedStr.toJSONString(),false);
                content.add(hookString);
            }else{
                log.error("Invalid hook format: " + rawHook);
            }
        } catch (JSONException e) {
            HookString hookString = new HookString(HookType.StringHook, rawHook, false);
            content.add(hookString);
        }
    }

    public void addNoThrowException(String rawHook){
        try {
            JSONObject parsedStr = JSONObject.parseObject(rawHook);
            if(parsedStr instanceof Map && parsedStr.size() == 1) {
                HookString hookString = new HookString(HookType.MapHook, parsedStr.toJSONString(),true);
                content.add(hookString);
            }else{
                log.error("Invalid hook format: " + rawHook);
            }
        } catch (JSONException e) {
            HookString hookString = new HookString(HookType.StringHook, rawHook, true);
            content.add(hookString);
        }
    }

    @Data
    public class HookString{
        private HookType type;
        private LazyString funcHook;
        private Map<LazyString, LazyString> mapHook;
        //钩子函数失败后，是否可以跳过继续执行该case的标记
        private Boolean noThrowException;

        public HookString(HookType type, String hookStr,Boolean noThrowException){
            if(type.equals(HookType.StringHook)) {
                this.type = HookType.StringHook;
                funcHook = new LazyString(hookStr);
                this.noThrowException = noThrowException;
            } else if(type.equals(HookType.MapHook)){
                mapHook = new HashMap<>();
                this.type = HookType.MapHook;
                JSONObject temp = JSONObject.parseObject(hookStr);
                for(Map.Entry<String,Object> each : temp.entrySet()){
                    LazyString key = new LazyString(each.getKey());
                    LazyString value = new LazyString(each.getValue().toString());
                    mapHook.put(key,value);
                }
                this.noThrowException = noThrowException;
            }
        }

        @Override
        public String toString() {
            switch (type) {
                case StringHook:
                    return funcHook.getRawValue();
                case MapHook:
                    return "{" + mapHook.keySet().iterator().next().toString() + ":" +
                            getMapHook().values().iterator().next().getRawValue() + "}";
                default:
                    HrunExceptionFactory.create("E10003");
                    return "error!!!";
            }
        }
    };
}
