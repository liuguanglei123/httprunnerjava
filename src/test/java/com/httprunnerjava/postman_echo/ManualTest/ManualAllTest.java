package com.httprunnerjava.postman_echo.ManualTest;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ManualAllTest extends HttpRunner {
    private Config config = new Config("ManualAllTest")
            .base_url("https://postman-echo.com");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("get with params")
                .withVariables("{'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}'}")
                .get("/get")
                .withParams("{'userId': '$userId', 'foo2': '$foo2', 'sum_v': '$sum_v','num-param': 12345}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}'}")
                .extract()
                .withJmespath("body.args.foo2", "foo3")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.userId", "$userId")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.foo2", "$foo2")
        );
    }};
}
