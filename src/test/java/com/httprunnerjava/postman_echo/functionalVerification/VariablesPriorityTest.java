package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import com.httprunnerjava.model.component.RunTestCase;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class VariablesPriorityTest extends HttpRunner {

    private Config config = new Config("config_name with variables,the viriables is $$foo: $foo1")
            .variables("{'key1':'value1-1','key2':'value2-1'}")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunTestCase("嵌套的testcase")
                .withVariables("{'key1':'value1-2','key2':'value2-2'}")
                .call(SingleRequestStep2.class)
                .export("['key3','key4']")
        );
        add(new RunRequest("普通的用例步骤")
                .get("/get")
                .withParams("{'key3': '$key3','key4': '$key4'}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.key3", "value1-2")
                .assertEqual("body.args.key4", "value2-2")
        );

    }};
}

