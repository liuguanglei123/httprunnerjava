package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class VariablesTest extends HttpRunner {

    private Config config = new Config("变量测试用例集合，用于测试变量的优先级，懒加载等")
            .variables("{'key1': 'configVarValues1','key2':'configVarValues2','key3':'configVarValues3'}")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("测试变量的优先级1：step变量优先级高于config变量")
                .withVariables("{'key1': 'stepVarValues1', 'key2': 'stepVarValues2', 'sum_v': '${sum_two(1,2)}'}")
                .get("/get")
                .withParams("{'param1': '$key1', 'param2': '$key2', 'sum_v': '$sum_v','num-param': 12345}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .extract()
                .withJmespath("body.args.param2", "foo3")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.param1", "stepVarValues1")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.param2", "stepVarValues2")
        );

        add(new RunRequest("测试url中的变量解析")
                .withVariables("{'key3': 'stepVarValues3'}")
                .get("/get?param1=$key1&param3=$key3")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.param3", "stepVarValues3")
                .assertEqual("body.args.param1", "configVarValues1")
        );

        add(new RunRequest("测试带有{}的变量解析")
                .withVariables("{'key4': '${key1}_value4'}")
                .get("/get")
                .withParams("{'param4': '$key4','param5':'${key4}_value5'}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.param4", "configVarValues1_value4")
                .assertEqual("body.args.param5", "configVarValues1_value4_value5")
        );

        add(new RunRequest("测试带有特殊符号的变量")
                .withVariables("{'key11': 'configVarValues1'}")
                .post("/post")
                .withJson("{'param6':'$key1+value6','param7':'$key1+value7','sum_v': '${sum_two(1,2)}+xxxxxxx'}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345,'Content-Type': 'application/json'}")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.data.param6", "configVarValues1+value6")
                .assertEqual("body.data.param7", "$key11+value7")
                .assertEqual("body.data.sum_v", "1002+xxxxxxx")
        );
    }};
}

