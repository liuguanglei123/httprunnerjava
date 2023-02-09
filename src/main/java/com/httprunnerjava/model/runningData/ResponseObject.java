package com.httprunnerjava.model.runningData;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.httprunnerjava.Parse;
import com.httprunnerjava.builtin.Comparator;
import com.httprunnerjava.exception.*;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import com.httprunnerjava.model.component.atomsComponent.response.Validator;
import com.httprunnerjava.model.lazyLoading.LazyString;
import com.httprunnerjava.utils.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Data
@Slf4j
public class ResponseObject {

    @Data
    public class ValidatorDict {
        //TODO：这里可以分析一下，checkValue和expectValue到底有哪些类型
        // string num(int float) boolean 还有其他的吗？
        private String comparator;
        private String check;
        private Object checkValue;
        private String expect;
        private Object expectValue;
        private String message;
        private String checkResult;
    }


    private static String currentRespBody;

    private Integer statusCode;
    //TODO：下面两个对象需要做成jsonObjec形式
    private Object headers;
    private Object cookies;
    private Object body;
    private Object srcBody;

    private Response srcRespObj;

    private String respObjStrEntity;

    private List<ValidatorDict> validation_results = new ArrayList<>();

    public ResponseObject(Response srcRespObj) {
        this.srcRespObj = srcRespObj;
        this.statusCode = srcRespObj.code();
        this.headers = srcRespObj.headers();
        this.cookies = srcRespObj.header("cookie");
        Optional.ofNullable(ResponseObject.getCurrentRespBody()).ifPresent( respBody ->{
            try{
                body = JSONObject.parse(ResponseObject.getCurrentRespBody());
            }catch (Exception e){
                log.error("reponseBody无法成功解析为json，请确认响应是否正确。");
                HrunExceptionFactory.create("E20009");
            }
        });
    }

    public void validate(List<Validator> validators,
                         Variables variables,
                         Class<?> functionsMapping){
        if(validators == null || validators.size() == 0){
            return ;
        }
        boolean validatePass = true;
        List<String> failures = new ArrayList<>();
        for(Validator each : validators) {
            String check_item = each.getCheckItem();
            Object check_item_valued;
            //TODO: 低优先级 这里可能会报错，还没有测试过
            if (check_item.contains("$")) {
                check_item_valued = (new LazyString(check_item)).parse(variables, functionsMapping).getEvalValue();
            }

            check_item_valued = _search_jmespath(check_item);

            String assert_method = each.getComparator();

            Method assert_func = null;
            try {
                assert_func = LazyString.getMappingFunction(assert_method, functionsMapping);
            }catch (Exception e) {
                log.error(String.valueOf(e.getStackTrace()));
                HrunExceptionFactory.create("E20008");
            }

            // expect item
            Object expectValue;
            Object expect_item = each.getExpectValue();
            if(expect_item instanceof String){
                expectValue = (new LazyString((String)expect_item)).parse(variables, functionsMapping).getEvalValue();
            }else {
                expectValue = expect_item;
            }
            // message
//                String message = each.getMessage();
            //TODO: 低优先级 parse message with config/teststep/extracted variables
//                        message = parse_data(message, variables_mapping, functionsMapping)
//                validate_msg = f"assert {check_item} {assert_method} {expectValue}({type(expectValue).__name__})"
//                validator_dict = {
//                        "comparator": assert_method,
//                        "check": check_item,
//                        "checkValue": checkValue,
//                        "expect": expect_item,
//                        "expectValue": expectValue,
//                        "message": message,
//            }

            //TODO:日志需要优化
            String validate_msg = String.format("assert %s %s %s(type is %s)",
                    check_item,
                    assert_method,
                    expectValue,
                    expectValue == null ? "NULL" : expectValue.getClass());

            try{
                if(check_item_valued == null){
                    if( expectValue == null
                            || String.valueOf(expectValue).toUpperCase().equals("NULL")
                            || String.valueOf(expectValue).equals("None")) {
                        ;
                    }else{
                        log.error("要导出的变量："+ check_item + "不存在");
                        HrunExceptionFactory.create("E20004");
                    }
                } else {
                    Comparator<?> comparator = new Comparator(check_item_valued);
                    assert_func.invoke(comparator, check_item_valued, expectValue);
                }
                validate_msg += "\t==> pass";
                log.info(validate_msg);
            }catch (Exception e) {
                validatePass = false;
                if (e instanceof InvocationTargetException) {
                    Throwable targetEx =((InvocationTargetException)e).getTargetException();
                    if(targetEx instanceof CompareError){
                        validate_msg += "\t==> fail \n";
                        validate_msg += String.format("check_item : %s \n",check_item);
                        validate_msg += String.format("checkValue : %s \n",check_item_valued);
                        validate_msg += String.format("assert_method : %s \n",assert_method);
                        validate_msg += String.format("expectValue : %s \n",expectValue);
                        log.error(validate_msg);
                        failures.add(validate_msg);
                    }else{
                        //如果捕获的异常不属于HrunBizException，则是反射方法执行时出错，需要输出详细的错误信息
                        log.error("执行过程中出现比对错误，该错误非hrun可捕获的问题，stackTrace信息为：\n" + HrunBizException.toStackTrace(e));
                    }
                } else {
                    log.error("执行过程中出现比对错误，原因是：" + e.getMessage());
                }
            }
        }

        //TODO:异常抛出需要优化，failures目前还是空的
        if(!validatePass){
            throw new ValidationFailureException(
                    String.join("\n", failures.toString())
            );
        }
    }

    public Object _search_jmespath(String expr){
        JSONObject resp_obj_meta = new JSONObject();
        resp_obj_meta.put("status_code", statusCode);
        resp_obj_meta.put("headers", headers);
        resp_obj_meta.put("cookies", cookies);
        resp_obj_meta.put("body",body);

        return _search(expr, resp_obj_meta);
    }

    public Object _search(String expr, JSON data){
        String tempExpr = expr;

        try{
            String[] strArray = tempExpr.split("\\.");
            if(tempExpr.length() == 0)
                return "";

            if(strArray.length == 1){
                if(strArray[0].matches("\\d+")){
                    return JsonUtils.getByNumKey(data,strArray[0]);
                }
                else {
                    return ((JSONObject)data).get(strArray[0]);
                }
            }else{
                if(strArray[0].matches("\\d+")) {
                    data = (JSON)JsonUtils.getByNumKey(data,strArray[0]);
                    expr = expr.substring(strArray[0].length() + 1);
                    return _search(expr, data);
                }else{
                    data = JsonUtils.getSubJson(data,strArray[0]);
                    expr = expr.substring(strArray[0].length()+1);
                    return _search(expr, data);
                }
            }
        }catch(Exception e){
            throw new VariableNotFound("需要导出的变量未找到，变量的路径为" + expr);
        }

    }

    public Variables extract(HashMap<String,String> extractors, Variables variables_mapping, Class<?> functionsMapping) {
        if(extractors == null || extractors.size() == 0) {
            return new Variables();
        }

        HashMap<String,Object> extract_mapping = new HashMap<>();
        for(Map.Entry<String,String> each : extractors.entrySet()){
            String str = new LazyString(each.getValue()).parse(variables_mapping, functionsMapping).getEvalString();
            Object field_value = _search_jmespath(str);
            if(field_value == null) {
                log.error("reponse中的" + str + "结果为null，需确认！！！");
            }
            extract_mapping.put(each.getKey(),field_value);
        }

        log.info("extract mapping: {}", extract_mapping);
        return new Variables(extract_mapping);
    }

    public static void setCurrentRespBody(String body){
        currentRespBody = body;
    }

    public static String getCurrentRespBody(){
        return currentRespBody;
    }

    public String logDetail(){
        return "\n" + "headers: " + Optional.ofNullable(statusCode).map(Object::toString).orElse("NULL") + "\n" +
                "params: " + Optional.ofNullable(headers).map(Object::toString).orElse("NULL") + "\n" +
                "req_json: " + Optional.ofNullable(body).map(Object::toString).orElse("NULL") + "\n";

    }




}
