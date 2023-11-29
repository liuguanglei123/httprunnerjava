package com.httprunnerjava.model.component.atomsComponent.request;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.httprunnerjava.exception.HrunExceptionFactory;
import com.httprunnerjava.model.component.intf.ParseAble;
import com.httprunnerjava.model.lazyLoading.LazyString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.testng.util.Strings;

import java.io.Serializable;
import java.util.Objects;

@Data
@Slf4j
public class ReqJson implements Serializable, ParseAble {

    private static Integer MAP_OBJ_TYPE = 1;

    private static Integer LIST_OBJ_TYPE = 2;

    private static Integer STRING_OBJ_TYPE = 3;

    private JSONObject mapObj;

    private JSONArray listObj;

    private LazyString strObj;

    private Integer type;

    public ReqJson(String raw_req_json){
        try{
            mapObj = JSONObject.parseObject(raw_req_json);
            type = MAP_OBJ_TYPE;
        }catch (JSONException e1){
            try{
                listObj = JSONArray.parseArray(raw_req_json);
                type = LIST_OBJ_TYPE;
            }catch (Exception e2){
                strObj = new LazyString(raw_req_json);
                type = STRING_OBJ_TYPE;
            }
        }
    }

    public ReqJson parse(Variables variables_mapping, Class functions_mapping) {
        if(Objects.equals(type, STRING_OBJ_TYPE)) {
            String parsedStringObj = strObj.parse(variables_mapping, functions_mapping).getEvalString();
            parseSrcString(parsedStringObj,strObj);
        }else if(Objects.equals(type, MAP_OBJ_TYPE)){
            this.mapObj = parseJson(mapObj,variables_mapping, functions_mapping);
        }else{
            this.listObj = parseJson(listObj,variables_mapping, functions_mapping);
        }
        return this;
    }

    public JSONObject parseJson(JSONObject jsonObject, Variables variables_mapping, Class functions_mapping){
        JSONObject result = new JSONObject();
        if(jsonObject == null ||jsonObject.size() == 0 )
            return result;

        for(String s : jsonObject.keySet()){
            String newKey = new LazyString(s).parse(variables_mapping, functions_mapping).getEvalString();
            if(jsonObject.get(s) instanceof JSONArray){
                result.put(newKey,parseJson((JSONArray)jsonObject.get(s), variables_mapping, functions_mapping));
            }else if(jsonObject.get(s) instanceof JSONObject){
                result.put(newKey,parseJson((JSONObject)jsonObject.get(s), variables_mapping, functions_mapping));
            }else if(jsonObject.get(s) instanceof String){
                String newValue = new LazyString((String)jsonObject.get(s)).parse(variables_mapping, functions_mapping).getEvalString();
                result.put(newKey, newValue);
            }else {
                result.put(newKey,jsonObject.get(s));
            }
        }
        return result;
    }

    public JSONArray parseJson(JSONArray jsonArray, Variables variables_mapping, Class functions_mapping){
        JSONArray result = new JSONArray();
        for(Object s : jsonArray){
            if(s instanceof JSONArray){
                result.add(parseJson((JSONArray)s, variables_mapping, functions_mapping));
            }else if(s instanceof JSONObject){
                result.add(parseJson((JSONObject)s, variables_mapping, functions_mapping));
            }else if(s instanceof String){
                String newValue = new LazyString((String)s).parse(variables_mapping, functions_mapping).getEvalString();
                result.add(newValue);
            }else {
                result.add(s);
            }
        }

        return result;
    }

    public void parseSrcString(String parsedStringObj,LazyString strObj){
        try{
            mapObj = JSONObject.parseObject(parsedStringObj);
            type = MAP_OBJ_TYPE;
        }catch (Exception e1){
            try{
                listObj = JSONArray.parseArray(parsedStringObj);
                type = LIST_OBJ_TYPE;
            }catch (Exception e2){
                log.error("解析json出现问题，该字符串无法解析成JSONObject或者JSONArray，" +
                        "该报错可能是因为json串中引号使用不当造成的");
                log.error("待解析的源字符串是：" + strObj.getRawValue());
                log.error("待解析的目标字符串是：" + parsedStringObj);
                HrunExceptionFactory.create("E00003");
            }
        }
    }

    public String getEvalString(){
        if(Objects.equals(type, STRING_OBJ_TYPE))
            return this.getStrObj().getEvalString();
        else if(Objects.equals(type, LIST_OBJ_TYPE))
            return JSON.toJSONString(this.listObj,SerializerFeature.WriteNullStringAsEmpty);
        else
            return JSON.toJSONString(this.mapObj,SerializerFeature.WriteMapNullValue);
    }

    public static Boolean isNull(ReqJson reqJson){
        return reqJson == null ||
               ( reqJson.getStrObj() == null && reqJson.getMapObj() == null && reqJson.getListObj() == null);
    }



}
