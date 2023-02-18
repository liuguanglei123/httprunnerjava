package com.httprunnerjava.utils;

import com.alibaba.fastjson.JSONObject;
import com.httprunnerjava.exception.HrunExceptionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CommonUtils {

    static Pattern absolute_http_url_regexp_compile = Pattern.compile("^https://.*|^http://.*");

    /**
     * 深复制一个实例，实例需要继承Serializable接口
     */
    public static <T extends Serializable> T deepcopy_obj(T data){
        //深复制，源码用的是python中自带的deepcopy通过递归实现的，这里通过字节流的复制实现的，百度到的方法
        /*deepcopy dict data, ignore file object (_io.BufferedReader)*/
        if(data == null) {
            return data;
        }

        return clone(data);
    }

    public static <T extends Serializable> T clone(T obj) {
        T clonedObj = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            clonedObj = (T) ois.readObject();
            ois.close();
        } catch (Exception e) {
            log.error(String.valueOf(e.toString()));
        }
        return clonedObj;
    }

    public static Map parseJsonStrToMap(String str){
        try {
            Map json = JSONObject.parseObject(str, Map.class);
            return json;
        }catch (Exception e){
            log.error("解析JSON中出现错误，待解析的字符串是：" + str);
            throw e;
        }
    }

    public static String buildUrl(String base_url,String path){
        if(absolute_http_url_regexp_compile.matcher(path).matches()){
            return path;
        }
        else if(base_url != null && !base_url.equals("")){
            if(base_url.matches(".*/")) {
                base_url = base_url.substring(0, base_url.length() - 1);
            }

            if(path.matches("/.*")) {
                path = path.substring(1);
            }

            return String.format("%s/%s",base_url,path);
        }
        else{
            HrunExceptionFactory.create("E00002");
            return null;
        }
    }

    public static List<Map<String,Object>> genCartesianProduct(List<List<Map<String,Object>>> args){
        if(args == null || args.size() == 0){
            return new ArrayList<>();
        }else if(args.size() == 1){
            return args.get(0);
        }

        List<Map<String,Object>> productList = new ArrayList<>();
        if(args.size()==2){
            if(args.get(0).size()==0 || args.get(1).size()==0){
                HrunExceptionFactory.create("E00010");
            }

            for(int index=0; index<args.get(0).size(); index++){
                for(int index2=0; index2<args.get(1).size(); index2++) {
                    Map<String,Object> temp = new HashMap<>();
                    temp.putAll(args.get(0).get(index));
                    temp.putAll(args.get(1).get(index2));
                    productList.add(temp);
                }
            }
        }else{
            List<Map<String,Object>> other_value_cartesian = genCartesianProduct(args.subList(1,args.size()));
            List<List<Map<String,Object>>> parsedArgs = Stream.of(other_value_cartesian,args.get(0)).collect(Collectors.toList());
            return genCartesianProduct(parsedArgs);
        }

        return productList;
    }

    public static List<List> cartesianProduct(List... lists) {
        List first = transformation(lists[0]);
        for (int i = 0; i < lists.length - 1; i++) {
            first = match(first, lists[i + 1]);
        }
        return first;
    }

    public static List<List> match(List<List> lists, List container1) {
        List<List> r = new ArrayList<>();
        for (List list : lists) {
            for (Object o : container1) {
                List list1 = new ArrayList();
                list1.addAll(list);
                list1.add(o);
                r.add(list1);
            }
        }
        return r;
    }

    /**
     * 将[a,b,c]转换成
     * [
     * [a],[b],[c],[d]
     * ]
     * why?
     * cos array便于做插入操作
     */
    private static List<List> transformation(List container) {
        List<List> r = new ArrayList<>();
        for (Object o : container) {
            List list = new ArrayList();
            list.add(o);
            r.add(list);
        }
        return r;
    }

}