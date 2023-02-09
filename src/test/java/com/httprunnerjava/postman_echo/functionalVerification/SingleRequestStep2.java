package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SingleRequestStep2 extends HttpRunner {

    private Config config = new Config("最普通的测试用例,其中参数$$var1的值为: $var1，该值是从env文件中加载的")
            .variables("{'key1':'value1-3','key2':'value2-3'}")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("get with params")
                .withVariables("{'key1':'value1-4','key2':'value2-4'}")
                .get("/get")
                .withParams("{'key1': '$key1', 'key2': '$key2'}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .extract()
                .withJmespath("body.args.key1", "key3")
                .withJmespath("body.args.key2", "key4")
                .validate()
                .assertEqual("status_code", 200)
        );
    }};
}

