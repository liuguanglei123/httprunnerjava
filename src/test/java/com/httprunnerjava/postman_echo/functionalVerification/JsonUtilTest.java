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
public class JsonUtilTest extends HttpRunner{
    private Config config = new Config("config_name with variables,the viriables is $$var1: $var1")
            .variables("{'var1':'config_var1'}")
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
                .withJson("${loadResourcesFileAsString(/request_methods/requestBody1)}")
                .validate()
                .assertEqual("status_code", 200)
                .jsonEqual("body.json","${loadResourcesFileAsString(/request_methods/requestBody2)}")
        );
    }};
}
