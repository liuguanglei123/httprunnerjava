package com.httprunnerjava.model;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.component.StepRefCase;
import com.httprunnerjava.model.component.StepRequestValidation;
import com.httprunnerjava.model.component.atomsComponent.request.Hooks;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import com.httprunnerjava.model.component.atomsComponent.response.Export;
import com.httprunnerjava.model.component.atomsComponent.response.Validator;
import com.httprunnerjava.model.component.intf.CallAble;
import com.httprunnerjava.model.component.moleculesComponent.TRequest;
import com.httprunnerjava.model.lazyLoading.LazyString;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

@Data
public class Step implements Serializable {

    final public static List<String> supportMethod = Arrays.asList("GET","POST");

    // 步骤的名字
    private String name;

    // 封装好的请求体
    private TRequest request;

    private String method;

    private String url;

    // http请求的响应
//    private Response response;

    // 嵌套的testcase
    private Class<? extends HttpRunner> testcase;

    //TODO：原版httprunner中，testcas还有可能是个str格式，暂时并未实现
    private String testcasestr;

    // TODO：好像是实现错了的东西，后面确认下是否可以删除
    private CallAble testcasecall;

    // 步骤中的变量指定
    private Variables variables;

    // 钩子函数
    private Hooks setupHooks = new Hooks();

    private Hooks teardownHooks = new Hooks();

    // 需要导出的键值对，key为导出的变量，value为导出的结构体位置
    private HashMap<String, String> extract;

    // TODO：待议
    private Export export;

    // 验证内容
    private List<Validator> validators = new ArrayList<>();

    // TODO：验证脚本，原版自带，暂不实现
    private List<String> validate_script;

    public Step(String name) {
        this.name = name;
        variables = new Variables();
        extract = new HashMap<>();
    }

    public TRequest request() {
        return getRequest();
    }

    public Object testcase() {
        return getTestcasecall();
    }

    public Step(Step step){
        name = step.getName();
        request = step.getRequest();
        testcase = step.getTestcase();
        testcasestr = step.getTestcasestr();
        testcasecall = step.getTestcasecall();
        variables = step.getVariables();
        setupHooks = step.getSetupHooks();
        teardownHooks = step.getTeardownHooks();
        extract = step.getExtract();
        export = step.getExport();
        validators = step.getValidators();
        validate_script = step.getValidate_script();
        apiPath = step.getRequest() == null ? "" : step.getRequest().getUrl().getRawValue();
        httpType = step.getRequest() == null ? "" : step.getRequest().getMethod().getMethod();
    }

    @Override
    public String toString(){
        StringBuffer result = new StringBuffer()
                .append("ApolloRunDate [")
                .append("httpType=" + Optional.ofNullable(httpType).get() + ",")
                .append("apiPath=" + Optional.ofNullable(apiPath).get() + ",")
                .append("name=" + Optional.ofNullable(name).orElse("") + ",")
                .append("request=" + Optional.ofNullable(request).orElse(new TRequest()).toString() + ",")
                .append("variables=" + Optional.ofNullable(variables).orElse(new Variables()).toString() + ",")
                .append("setupHooks=" + Optional.ofNullable(setupHooks).orElse(new Hooks()).toString() + ",")
                .append("teardownHooks=" + Optional.ofNullable(teardownHooks).orElse(new Hooks()).toString() + ",")
                .append("extract=" + Optional.ofNullable(extract).orElse(new HashMap<>()).toString() + ",")
                .append("validators=" + Optional.ofNullable(validators).orElse(new ArrayList<>()).toString())
                .append("]");
//                .append("testcase=" + Optional.ofNullable(testcase).orElse(new H).toString() + "\n")
//                .append("testcasestr=" + Optional.ofNullable(testcasestr).orElse("").toString() + "\n")
//                .append("testcasecall=" + Optional.ofNullable(response).orElse(new Response()).toString() + "\n")
//                .append("export=" + Optional.ofNullable(export).orElse(new Export()).toString() + ",\n")
//                .append("validate_script: " + Optional.ofNullable(response).orElse(new Response()).toString() + "\n");
        return result.toString();
    }

    public String apiPath;

    public String httpType;
}
