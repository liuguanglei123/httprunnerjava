package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.model.Config;
import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.RunRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PostValidate extends HttpRunner {

    private Config config = new Config("PostValidate")
            .variables("{'foo1':'session_bar1'}")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("post form data using json 1")
                .withVariables(
                        "{'foo2': 'bar23','jsondata':[" +
                                    "{'key1':'value1','key2':'value2'}," +
                                    "{'key11':'value11','key22':'value22'}," +
                                    "{'key111':'value111','key222':'value222'}" +
                                "]}"
                )
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson("$jsondata")
                .validate()
                .assertEqual("status_code", 200)
                .listContains("body.json","[{'key1':'value1','key2':'value2'},{'key11':'value11','key22':'value22'},{'key111':'value111','key222':'value222'}]")

        );

        /* add(new RunRequest("post form data using json 2")
                .withVariables(
                        "{'foo2': 'bar23','jsondata':[11,22,33,44,55,66,77,88,99]}"
                )
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson("$jsondata")
                .validate()
                .assertEqual("status_code", 200)
                .listContains("body.json","[11,22,33,44,55,66]")
                .notListEmpty("body.json")
        );

        add(new RunRequest("post form data using json 3")
                .withVariables(
                        "{'foo2': 'bar23','jsondata':[]}"
                )
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson("$jsondata")
                .validate()
                .assertEqual("status_code", 200)
                .listEmpty("body.data")
        );

        add(new RunRequest("post form data using json 4")
                .withVariables(
                        "{'userId': '123456789','rId':'2'}"
                )
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson("{\n" +
                        "    \"userIds\":[$userId],\n" +
                        "    \"id\":$rId,\n" +
                        "    \"orgId\":'${getAccountId($userId)}'\n" +
                        "}")
                .validate()
                .assertEqual("status_code", 200)
        );

        add(new RunRequest("post form data using json 2")
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson("[11,22,33,44,55,66,77,88,99]")
                .validate()
                .assertEqual("status_code", 200)
                .listContains("body.json","[11,22,33,44,55,66]")
                .assertEqual("body.json.4","55")
                .notListEmpty("body.json")
        );

        add(new RunRequest("post form data using json 4")
                .withVariables(
                        "{'userId': '123456789','rId':'2'}"
                )
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson("{\n" +
                        "    \"userIds\":[34567],\n" +
                        "    \"id\":23,\n" +
                        "    \"orgId\":'4FXXXXXX'\n" +
                        "}")
                .validate()
                .assertEqual("status_code", 200)
        );

        add(new RunRequest("post form data")
                .withVariables("{'foo2': 'bar23'}")
                .post("/post")
                .withHeaders("{'User-Agent': 'HttpRunner/3.0','Content-Type': 'application/x-www-form-urlencoded'}")
                .withData("foo1=$foo1&foo2=$foo2")
                .validate()
                .assertEqual("status_code", 200)
                .assertEqual("body.form.foo1", "session_bar1")
                .assertEqual("body.form.foo2", "bar23")
        );*/
    }};

}
