package com.httprunnerjava.model.lazyLoading;

import com.httprunnerjava.Loader;
import com.httprunnerjava.exception.HrunExceptionFactory;
import com.httprunnerjava.exception.VariableNotFound;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import com.httprunnerjava.builtin.Comparator;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.httprunnerjava.Parse.*;

@Slf4j
public class LazyString extends LazyContent<String>{

    public LazyString(String rawStr){
        super(rawStr);
    }

    //解析后的字符串
    private String parsedString;

    private Object parsedValue;

    /**
     * 用来计算实际懒加载对象所存储的值，对于LazyContent类型的值，会进入如下逻辑，对于LazyString类型的值，会进入LazyString中重写的parse方法
     * @param variablesMapping 加载过程中用到的变量集合
     * @param functionsMapping 自定义方法
     * @return 返回LazyContent本身，对于LazyString类型的变量来说，其parsedValue等值是计算过的
     */
    @Override
    public LazyString parse(Variables variablesMapping, Class functionsMapping) {
        if(variablesMapping == null){
            variablesMapping = new Variables();
        }

        parseString(variablesMapping,functionsMapping);
        return this;
    }

    public LazyString parseString(Variables variablesMapping, Class functionsMapping){
    /*Examples:
        >>> rawparsedString = "abc${add_one($num)}def"
        >>> variables_mapping = {"num": 3}
        >>> functions_mapping = {"add_one": lambda x: x + 1}
        >>> parseparsedString(rawparsedString, variables_mapping, functions_mapping)
            "abc4def"
     */

        Integer matchStartPosition = 0;
        String remainParsedString = "";
        matchStartPosition = rawValue.indexOf("$");
        if(matchStartPosition != -1){
            parsedString = rawValue.substring(0,matchStartPosition);
            remainParsedString = rawValue.substring(matchStartPosition);
        }else{
            parsedString = rawValue;
            return this;
        }


        while(remainParsedString.length() != 0){
            Matcher dollarMatch = dolloar_regex_compile.matcher(remainParsedString);
            if(dollarMatch.matches()){
                remainParsedString = remainParsedString.substring(2);
                parsedString += "$";
                continue;
            }

            Matcher funcMatch = function_regex_compile.matcher(remainParsedString);
            if(funcMatch.matches()){
                String func_name = funcMatch.group(2);
                //TODO:string整体为一个方法名时，会出现死循环
                Method func = getMappingFunction(func_name, functionsMapping);

                String func_params_str = funcMatch.group(3);
                Map<String, Object> function_meta = parseFunctionParams(func_params_str);
                List<LazyContent> args = (ArrayList<LazyContent>)function_meta.get("argsList");
                Map kwargs = (Map)function_meta.get("kwargsMap");
                List<Object> parsed_args = args.stream().map( arg ->
                        arg.parse(variablesMapping, functionsMapping).getEvalValue()
                ).collect(Collectors.toList());

                Object funcEvalValue = null;
                try{
                    if(isBuiltInFunc(func_name)){
                        Comparator<?> comparator = new Comparator(parsed_args.get(0));
                        funcEvalValue = func.invoke(comparator,parsed_args.get(0),parsed_args.get(1));
                    }else{
                        List<Object> funcParams = new ArrayList<Object>();
                        for(Object each : parsed_args){
                            if(each instanceof LazyString){
                                funcParams.add((((LazyString)each).parse(variablesMapping,functionsMapping)).getEvalString());
                            } else {
                                funcParams.add(String.valueOf(each));
                            }
                        }
                        Object obj = functionsMapping.newInstance();
                        if(funcParams.size() == 0){
                            funcEvalValue =  func.invoke(obj);
                        }else {
                            funcEvalValue = func.invoke(obj, funcParams.toArray());
                        }
                    }
                }catch(Exception e){
                    log.error("反射方法执行错误，错误原因是：" + e.getMessage());
                    HrunExceptionFactory.create("E20001");
                }

                String funcRawStr = "${" + func_name + "(" + func_params_str + ")}";
                if(funcRawStr.equals(rawValue)){
                    this.parsedValue = funcEvalValue;
                    this.parsedString += String.valueOf(funcEvalValue);
                    return this;
                }

                this.parsedString += String.valueOf(funcEvalValue);
                remainParsedString = remainParsedString.substring(funcMatch.group(1).length());
            }

            Matcher var_match = variable_regex_compile.matcher(remainParsedString);
            if(var_match.matches()){
                String varName;
                if(var_match.group(1) == null || var_match.group(1).equals(""))
                    varName = var_match.group(2);
                else
                    varName = var_match.group(1);

                Object varValue = getMappingVariable(varName, variablesMapping);

                String varRawStr1 = "${" + varName + "}";
                String varRawStr2 = "$" + varName;
                if(varRawStr1.equals(rawValue) || varRawStr2.equals(rawValue)){
                    this.parsedString = String.valueOf(varValue);
                    this.parsedValue = varValue;
                    return this;
                }

                this.parsedString += String.valueOf(varValue);
                if(remainParsedString.startsWith("${")){
                    remainParsedString = remainParsedString.substring(varName.length() + 3);
                } else {
                    remainParsedString = remainParsedString.substring(varName.length() + 1);
                }
            }

            matchStartPosition = remainParsedString.indexOf("$");
            if(matchStartPosition == 0){
                log.error("无法解析的字符串" + rawValue);
                log.error("字符串中出现$但却无法解析，不属于$$ 变量或者方法的任何一种类型，请检查。");
                HrunExceptionFactory.create("E00005");
            }
            if(matchStartPosition != -1){
                this.parsedString += remainParsedString.substring(0,matchStartPosition);;
                remainParsedString = remainParsedString.substring(matchStartPosition);
            }else{
                this.parsedString += remainParsedString;
                return this;
            }
        }
        return this;
    }

    public static Method getMappingFunction(String functionName,Class functionsMapping) {
        //如果这里仅仅是根据方法名去查询，场景有三：
        // 1.查询的方法是buildin方法，比如equals less_than等
        // 2.查询的方法是debugtalk.java内部定义的方法，但是该方法没有参数
        // 3.查询的方法是debugtalk.java内部定义的方法，但是该方法是有参数的，但是这里仅仅是根据方法名去查询了
        // 并没有做参数类型的匹配，也就是说，debugtalk文件不支持方法的重载
        Method method = null;

        //TODO:hrun这里采用的是多层调用，获取builtin模块的各个方法，但是java与此不同的是，builtin中的
        // comparator类是一个泛型类，经过测试，虽然能够获取到对应名字的方法，但是执行的时候，需要先实例化一个对象才可以
        try{
            Class<?> built_in_functions = Loader.loadBuiltinFunctions();
            Class[] classed = new Class[2];
            classed[0] = Object.class;
            classed[1] = Object.class;
            method = built_in_functions.getMethod(functionName,classed);
            if(method != null) {
                return method;
            }
        }catch(Exception e){
            log.debug(String.format("方法 %s 在hrun内置类中没有找到,将在debugtalk.java中继续寻找", functionName));
        }

        //TODO：debugtalk中的函数不支持函数重载
        for (Method each : functionsMapping.getMethods()) {
            if (each.getName().equals(functionName)) {
                method = each;
                return method;
            }
        }

        try {
            Class<?> loadFileClass = Loader.loadLoadFileClass();
            for (Method each : loadFileClass.getMethods()) {
                if (each.getName().equals(functionName)) {
                    method = each;
                    return method;
                }
            }
        } catch (Exception e) {
                log.debug(String.format("方法 %s 在LoadFile中没有找到", functionName));
        }

        if(method == null) {
            log.error("方法 " + functionName + " 不存在");
            HrunExceptionFactory.create("E20002");
        }

        return null;
    }

    public static Map<String, Object> parseFunctionParams(String params) {
        Map<String, Object> functionMeta = new HashMap<String, Object>() {{
            put("argsList", new ArrayList<LazyContent>());
            put("kwargsMap", new HashMap<String, Object>());
        }};

        String paramsStr = params.trim();
        if (params.equals("")) {
            return functionMeta;
        }

        String[] args_list = paramsStr.split(",");
        for (String arg : args_list) {
            arg = arg.trim();
            if (arg.contains("=")) {
                String[] keyvalue = arg.split("=");
                if (keyvalue.length > 2) {
                    HrunExceptionFactory.create("E00006");
                }
                    //TODO:((Map) function_meta.get("kwargsMap")).put(keyvalue[0].trim(), parse_string_value(keyvalue[1].trim()));
            } else {
                if(arg instanceof String) {
                    ((List) functionMeta.get("argsList")).add(new LazyString(arg));
                } else {
                    ((List) functionMeta.get("argsList")).add(new LazyContent(arg));
                }
            }
        }
        return functionMeta;
    }

    @Override
    public Object getEvalValue(){
        return parsedValue == null ?
                ( parsedString == null? getRawValue() : parsedString) : parsedValue;
    }

    public String getEvalString(){
        return parsedString == null ? getRawValue():parsedString;
    }

    public static Boolean isBuiltInFunc(String func_name){
        if(Arrays.asList(",less_than".split(",")).contains(func_name)){
            return true;
        }
        return false;
    }

    public Object getParsedValue(){
        return this.parsedValue;
    }

    public Object getMappingVariable(String variable_name,Variables variables_mapping){
        try {
            return variables_mapping.get(variable_name).getEvalValue();
        }catch (Exception e){
            throw new VariableNotFound(String.format("%s not found in %s",variable_name,variables_mapping));
        }
    }

}
