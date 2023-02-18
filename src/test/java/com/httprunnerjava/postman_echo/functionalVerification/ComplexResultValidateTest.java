package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ComplexResultValidateTest extends HttpRunner {
    private Config config = new Config("ComplexResultValidateTest")
            .variables("{'foo1':'config_bar1','foo2':'config_bar2','expect_foo1':'config_bar1','expect_foo2': 'config_bar2'}")
            .base_url("http://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{

        add(new RunRequest("post raw text")
                .withVariables("{'foo1': 'bar12', 'foo3': 'bar32'}")
                .post("/post")
                .withHeaders("{'User-Agent': 'HttpRunner/3.0', 'Content-Type': 'text/plain'}")
                .withData("This is expected to be sent back as part of response body: $foo1-$expect_foo2-$foo3.")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual(
                        "body.data",
                        "This is expected to be sent back as part of response body: bar12-$expect_foo2-bar32."
                )
                .assertEqual("body.json", "NULL")
                .assertTypeMatch("body.json", "NULL")
        );

        add(new RunRequest("post raw text")
                .withVariables("{'foo1': 'bar12', 'foo3': 'bar32'}")
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson("{accountIds:[],userIds:null,customerIds:[{geren:null},{shangjia:null}]}")
                .validate()
                .assertEqual("status_code", 200)
                .jsonEqual("body.json", "{\"accountIds\":[],\"customerIds\":[{},{}]}")
        );

        add(new RunRequest("post raw text")
                .withVariables("{'foo1': 'bar12', 'foo3': 'bar32'}")
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .validate()
                .assertEqual("status_code", 200)
                .jsonEqual("body.data", "{}")
        );
    }};

}
