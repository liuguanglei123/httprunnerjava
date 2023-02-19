package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.annotation.Parameters;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Parameters(mapStr = "{" +
        "                'param1':'5SFXXXXXXXX'," +
        "                'param2':12345," +
        "                'param3':['foo1','foo2']," +
        "                'param4':['soo1','soo2']" +
        "            }")
public class ParametersTest1 extends HttpRunner {

    private Config config = new Config("最普通的测试用例,其中参数$$var1的值为: $var1，该值是从env文件中加载的")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("get with params")
                .get("/get")
                .withParams("{'foo1': '$param1', 'foo2': '$param2', 'foo3': '$param3','foo4': '$param4'}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "$param1")
                .assertEqual("body.args.foo2", "${toStr($param2)}")
                .assertEqual("body.args.foo3", "$param3")
                .assertEqual("body.args.foo4", "$param4")
        );

        add(new RunRequest("get with params222")
                .get("/get")
                .withParams("{'foo1': '$param1', 'foo2': '$param2', 'foo3': '$param3','foo4': '$param4'}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "$param1")
                .assertEqual("body.args.foo2", "${toStr($param2)}")
                .assertEqual("body.args.foo3", "$param3")
                .assertEqual("body.args.foo4", "$param4")
        );
    }};
}

