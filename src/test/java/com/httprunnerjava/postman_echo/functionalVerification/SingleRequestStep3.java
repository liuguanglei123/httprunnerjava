package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SingleRequestStep3 extends HttpRunner {

    private Config config = new Config("简单验证不同的请求方式，包括get post delet put")
            .withLocalDebug(true)
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("get with params")
                .withVariables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}'}")
                .get("/get")
                .withParams("{'foo1': '$foo1', 'foo2': '$foo2', 'sum_v': '$sum_v','num-param': 12345}")
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}','header-num':12345}")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "$foo1")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.foo2", "$foo2")
        );

        add(new RunRequest("get with params")
                .withVariables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}'}")
                .post("/post")
                .withParams("{'foo1': '$foo1', 'foo2': '$foo2', 'sum_v': '$sum_v','num-param': 12345}")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson(
                        "{accountIds:[\"QWER\"],userIds:12345,customerIds:[{geren:1},{shangjia:2}]}"
                )
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "$foo1")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.foo2", "$foo2")
                .assertEqual("body.data.accountIds.0", "QWER")
                .assertEqual("body.data.customerIds.0.geren", 1)
        );

        add(new RunRequest("get with params")
                .withVariables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}'}")
                .put("/put")
                .withParams("{'foo1': '$foo1', 'foo2': '$foo2', 'sum_v': '$sum_v','num-param': 12345}")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson(
                        "{accountIds:[\"QWER\"],userIds:12345,customerIds:[{geren:1},{shangjia:2}]}"
                )
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "$foo1")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.foo2", "$foo2")
                .assertEqual("body.data.accountIds.0", "QWER")
                .assertEqual("body.data.customerIds.0.geren", 1)
        );

        add(new RunRequest("get with params")
                .withVariables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}'}")
                .delete("/delete")
                .withParams("{'foo1': '$foo1', 'foo2': '$foo2', 'sum_v': '$sum_v','num-param': 12345}")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson(
                        "{accountIds:[\"QWER\"],userIds:12345,customerIds:[{geren:1},{shangjia:2}]}"
                )
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "$foo1")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.foo2", "$foo2")
                .assertEqual("body.data.accountIds.0", "QWER")
                .assertEqual("body.data.customerIds.0.geren", 1)
        );


    }};
}

