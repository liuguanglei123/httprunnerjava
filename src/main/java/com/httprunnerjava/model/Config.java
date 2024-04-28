package com.httprunnerjava.model;

import com.httprunnerjava.model.Enum.RetryTypeEnum;
import com.httprunnerjava.model.component.atomsComponent.response.Export;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import com.httprunnerjava.model.lazyLoading.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Config {
    // 重试时间
    public static Long waitTime = 10000L;
    // 重试次数
    public static int maxRetryCount = 2;

    // 一个HttpRunner中仅有一个Config，该名字即为整个测试类的名字
    private LazyString name;

    // TODO: 原版HttpRunner的变量，需要考虑是否删除
    private Boolean verify;

    // 用例的全局url域名
    private LazyString baseUrl;

    // 用例的全局参数，Config中参数的优先级低于用例步骤中的优先级
    private Variables variables;

    // 用例执行后需要导出的内容，详细请查看test目录下的示例
    private Export export;

    // TODO：
    //  文件路径，待实现
    private LazyString path;

    // TODO:
    //  执行权重，待实现
    private Integer weight;

    // 是否开启proxy模式，开启后会自动设置127.0.0.0:8888代理，所有的请求都会经过代理转发
    private Boolean isProxy = false;

    // 单个用例执行出错后是否继续执行
    private Boolean resumeAfterException = false;

    // 毫秒数
    private Long timeOut;

    private RetryTypeEnum retryType;

    public Config(String name) {
        this.name = new LazyString(name);
        this.variables = new Variables();
        this.baseUrl = new LazyString("");
        this.verify = false;
        this.export = new Export();
        this.weight = 1;
        this.timeOut = 5000L;
        this.retryType = RetryTypeEnum.SINGLESTEP;
        waitTime = 10000L;
        maxRetryCount = 2;
    }

    public Config variables(String variablesStr) {
        Variables tmpVar = new Variables(variablesStr);
        this.variables.update(tmpVar);
        return this;
    }

    public Config base_url(String base_url) {
        this.baseUrl = new LazyString(base_url);
        return this;
    }

    public Config verify(Boolean verify) {
        this.verify = verify;
        return this;
    }

    public Config export(String export_var_name_str) {
        this.export.update(new Export(export_var_name_str));
        return this;
    }

    public Config withLocalDebug(Boolean isProxy) {
        if (isProxy) {
            log.warn("已开启代理模式，所有请求将请求到 127.0.0.1:8888，请确认代理服务器状态。");
        }

        this.isProxy = isProxy;
        return this;
    }

    public Config withRetryType(RetryTypeEnum retryType) {
        this.retryType = retryType;
        log.debug("当前重试模式为：" + retryType.getRetryDescription());

        return this;
    }

    public Config withCatchAllExpection(Boolean resumeAfterException) {
        this.resumeAfterException = resumeAfterException;
        return this;
    }

    public Config withTimeOut(Long timeOut) {
        this.timeOut = timeOut;
        return this;
    }

    public void updateVariables(Variables var) {
        variables.update(var);
    }

    public Config waitTime(Long waitTime) {
        Config.waitTime = waitTime;
        return this;
    }

    public Config maxRetryCount(int maxRetryCount) {
        Config.maxRetryCount = maxRetryCount;
        return this;
    }


}
