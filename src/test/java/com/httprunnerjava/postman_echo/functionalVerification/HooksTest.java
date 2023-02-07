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
public class HooksTest extends HttpRunner {

    private Config config = new Config("config_name with variables,the viriables is $$foo: $foo1")
            .variables("{'foo1':'config_bar1','foo2':'config_bar2','expect_foo1':'config_bar1'," +
                    "'expect_foo2': 'config_bar2', 'user_id': '4Fxxxxx'}")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("get with params")
                .setupHook("${setup_hooks()}")
                //以下两行写法类型是不支持的,setuphook中不支持注入变量
//                .setupHook("{'accountId': '${getAccountId(3FXXXXXX)}'}")
//                .setupHook("{'userId': '${getUserId($user_id)}'}")
                .setupHookNoThrowException("${NoExistFunc($foo1)}")
                .setupHookNoThrowException("{'userId': '${getUserId($user_id1)}'}")
                .withVariables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}'}")
                .get("/get")
                .withParams("{'foo1': '$foo1', 'foo2': '$foo2', 'sum_v': '$sum_v'}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}'}")
                .teardownHook("teardown_hooks()")
                .teardownHookNoThrowException("${NoExistFunc()}")
                .extract()
                .withJmespath("body.args.foo2", "foo3")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "bar11")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.foo2", "bar21")
        );

        add(new RunTestCase("request with testcase reference")
                .withVariables("{'foo1': 'testcase_ref_bar1', 'expect_foo1': 'testcase_ref_bar1'}")
                .setupHook("${sleep(0.1)}")
                .setupHookNoThrowException("${NoExistFunc(0.1)}")
                .call(SingleRequestStep.class)
                .teardownHook("${sleep(0.2)}")
                .teardownHookNoThrowException("${NoExistFunc(0.2)}")
                .export("['foo3']")
        );
    }};
}

