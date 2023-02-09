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
public class TestCaseRefrenceStep extends HttpRunner {

    private Config config = new Config("testcase reference")
            .variables("{'key1':'config_value1','key2':'config_value2'}")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("普通的用例步骤")
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

        add(new RunTestCase("嵌套的testcase")
                .withVariables("{'key1': 'testcase_ref_bar1', 'key2': 'testcase_ref_bar2'}")
                .setupHook("${setup_hooks()}")
                .setupHookNoThrowException("${NoExistFunc($foo1)}")
                .call(SingleRequestStep2.class)
                .export("['key3']")
                .teardownHook("teardown_hooks()")
                .teardownHookNoThrowException("${NoExistFunc()}")
        );

        add(new RunRequest("普通的用例步骤")
                .get("/get")
                .withParams("{'foo1': '$key3'}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "value1-4")
        );
    }};
}

