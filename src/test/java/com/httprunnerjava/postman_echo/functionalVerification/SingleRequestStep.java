package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SingleRequestStep extends HttpRunner {

    private Config config = new Config("最普通的测试用例,其中参数$$var1的值为: $var1，该值是从env文件中加载的")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("get with params")
                .withVariables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}'}")
                .get("/get")
                .withParams("{'foo1': '$foo1', 'foo2': '$foo2', 'sum_v': '$sum_v','num-param': 12345}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .extract()
                .withJmespath("body.args.foo2", "foo3")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "$foo1")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.foo2", "$foo2")
        );
    }};
}

