package com.httprunnerjava.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.*;

@Slf4j
public class JsonUtils {

    /**
     * 默认的json比对方法，默认使用的宽松模式，555678
     * @param json1
     * @param json2
     */
    public static void compareJson(String json1, String json2) {
        compareJson(json1,json2,JSONCompareMode.LENIENT);
    }

    public static void compareJson(String json1, String json2, JSONCompareMode jsonCompareMode){
        log.info("json1: " + json1);
        log.info("json2: " + json2);

        try{
            //先解析为jsonArray格式，判断是否相等
            JSONArray jsonArray1 = JSONArray.parseArray(json1);
            JSONArray jsonArray2 = JSONArray.parseArray(json2);
            String jsonArrayStr1 = jsonArray1.toJSONString();
            String jsonArrayStr2 = jsonArray2.toJSONString();
            JSONAssert.assertEquals(jsonArrayStr1, jsonArrayStr2, jsonCompareMode);
        }catch (Exception e){
            try{
                //再尝试解析为jsonObject格式，判断是否相等
                JSONObject jsonObject1 = JSONObject.parseObject(json1);
                JSONObject jsonObject2 = JSONObject.parseObject(json2);
                String jsonObjectStr1 = jsonObject1.toJSONString();
                String jsonObjectStr2 = jsonObject2.toJSONString();
                JSONAssert.assertEquals(jsonObjectStr1, jsonObjectStr2, jsonCompareMode);
            }catch (Exception e1){
                throw new AssertionError("要比对的json不一致!");
            }
        }
    }


//    public static void compareJson(Object json1, Object json2) {
//        if((json1 == null && json2 == null) || (json1 != null && json1.equals(json2))) {
//            return;
//        }
//
//        if(json1 instanceof String && json2 instanceof String){
//            try{
//                compareJson((String)json1,(String)json2);
//                return;
//            }catch(JSONException e){
//                throw new CompareError("要比对的json不一致!");
//            }
//        }
//
//        if (json1 instanceof JSONObject){
//            if(json2 instanceof String) {
//                compareJson(
//                        JSON.toJSONString((JSONObject) json1, SerializerFeature.WRITE_MAP_NULL_FEATURES),
//                        (String) json2
//                );
//            } else {
//                compareJson((JSONObject) json1, (JSONObject) json2);
//            }
//        } else if(json1 instanceof JSONArray){
//            if(json2 instanceof String) {
//                compareJson(
//                        JSON.toJSONString((JSONArray) json1, SerializerFeature.WRITE_MAP_NULL_FEATURES),
//                        (String) json2
//                );
//            } else {
//                compareJson((JSONArray) json1, (JSONArray) json2);
//            }
//        } else {
//            throw new CompareError("要比对的json不一致!");
//        }
//    }
//
//    public static void compareJson(JSONArray jsonArray1, JSONArray jsonArray2) {
//        if(jsonArray1.size() != jsonArray2.size()){
//            throw new CompareError("要比对的json不一致!");
//        }
//
//        Iterator i1 = jsonArray1.iterator();
//        while (i1.hasNext()) {
//            Object temp1 = i1.next();
//            Boolean isSame = false;
//            Iterator i2 = jsonArray2.iterator();
//            while (i2.hasNext()){
//                try{
//                    compareJson(temp1, i2.next());
//                }catch(CompareError e){
//                    continue;
//                }
//                isSame = true;
//                break;
//            }
//            if(!isSame) {
//                throw new CompareError("要比对的json不一致!");
//            }
//        }
//    }

//    public static void compareJson(JSONObject jsonObject1, JSONObject jsonObject2) {
//        if(jsonObject1.size() != jsonObject2.size()){
//            throw new CompareError("要比对的json不一致!");
//        }
//
//        Iterator<String> iterator = jsonObject1.keySet().iterator();
//        while (iterator.hasNext()) {
//            String key = iterator.next();
//
//            Object value1 = jsonObject1.get(key);
//            Object value2 = jsonObject2.get(key);
//            compareJson(value1, value2);
//        }
//    }

    public static void containJsonArray(JSONArray check_value, JSONArray expect_value, String key){
        boolean notContain = false;
        for (Object o1 : expect_value) {
            if (!check_value.contains(o1)) {
                log.error("不一致：key  " + o1 + " 接口返回的check_value并不包含该key");
                notContain = true;
            }
        }
        if(notContain) {
            throw new AssertionError("JSON比对结果不一致");
        }
    }

    public static void notContainJsonArray(JSONArray check_value, JSONArray expect_value, String key){
        boolean isContain = false;
        for (Object o1 : expect_value) {
            if (check_value.contains(o1)) {
                log.error("不一致：key  " + o1 + " 接口返回的check_value并不包含该key");
                isContain = true;
            }
        }
        if(isContain) {
            throw new AssertionError("JSON比对结果不一致");
        }
    }

    public static Object getByNumKey(JSON data, String key){
        //两种形式，一种是传入jsonobject，key是数字型的字符串
        // 另一种是传入jsonarray，key是数字
        if(data instanceof JSONArray){
            return ((JSONArray)data).get(Integer.parseInt(key));
        }else {
            return ((JSONObject)data).get(key);
        }
    }

    public static JSON getSubJson(JSON data, String key){
        try{
            data = ((JSONObject)data).getJSONObject(key);
        }catch (Exception e){
            data = ((JSONObject)data).getJSONArray(key);
        }
        return data;
    }

    public static Map parseJsonStrToMap(String str){
        try {
            Map json = JSONObject.parseObject(str, Map.class);
            return json;
        }catch (Exception e){
            log.error("解析JSON中出现错误，请尝试增加或修改引号或单引号，待解析的字符串是：" + str);
            throw e;
        }
    }
}
