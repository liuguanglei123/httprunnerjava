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
public class ExportTest extends HttpRunner {

    private Config config = new Config("extract导出变量测试，该用例集合主要针对：上一个用例导出的变量供下一个使用，并不涉及用例集合的变量的导出")
            .base_url("https://postman-echo.com")
            .verify(false);

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("执行用例，将变量结果进行导出")
                .withVariables("{'foo1': 'bar11'}")
                .get("/get")
                .withParams("{'foo1': '$foo1'}")
                .extract()
                .withJmespath("body.args.foo1", "extractVar1")
                .withJmespath("body.args.foo3", "extractVar2")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "bar11")
        );

        add(new RunRequest("check extract")
                .get("/get")
                .withParams("{'foo2': '$extractVar1','foo3': '$extractVar2','foo4': 'existVar'}")
                .validate()
                .assertEqual("body.args.foo2", "bar11")
                .assertEqual("body.args.foo3", "null")
                .assertEqual("body.args.foo4", "existVar")
        );

    }};
}

