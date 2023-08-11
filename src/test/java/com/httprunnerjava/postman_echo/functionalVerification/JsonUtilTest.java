package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
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

        add(new RunRequest("对于json中空值的处理")
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson(
                        "{'foo2': 'bar23','jsondata':[" +
                                "{'key1':'value1','key2':'value2'}," +
                                "{'key11':'value11','key22':null}," +
                                "{'key111':'value111','key222':null}" +
                                "]," +
                                "'jsondata2':[]}"
                )                .validate()
                .assertEqual("status_code", 2001)
                .jsonEqualWithStrictMode("body.json","{\"foo2\": \"bar23\",\"jsondata\":[{\"key1\":\"value1\",\"key2\":\"value2\"}," +
                        "{\"key11\":\"value11\",\"key22\":null},{\"key111\":\"value111\",\"key222\":null}],\"jsondata2\":[]}")
        );

        add(new RunRequest("普通的json格式校验，json中用单引号表示双引号")
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson(
                        "{'foo2': 'bar23','jsondata':[" +
                                "{'key1':'value1','key2':'value2'}," +
                                "{'key11':'value11','key22':'value22'}," +
                                "{'key111':'value111','key222':'value222'}" +
                                "]}"
                )
                .validate()
                .assertEqual("status_code", 200)
                .jsonEqual("body.json","{'foo2': 'bar23','jsondata':[{'key1':'value1','key2':'value2'}," +
                        "{'key11':'value11','key22':'value22'},{'key111':'value111','key222':'value222'}]}")
        );

        add(new RunRequest("普通的json格式校验，json中为双引号")
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson(
                        "{'foo2': 'bar23','jsondata':[" +
                                "{'key1':'value1','key2':'value2'}," +
                                "{'key11':'value11','key22':'value22'}," +
                                "{'key111':'value111','key222':'value222'}" +
                                "]}"
                )                .validate()
                .assertEqual("status_code", 200)
                .jsonEqual("body.json","{\"foo2\": \"bar23\",\"jsondata\":[{\"key1\":\"value1\",\"key2\":\"value2\"}," +
                        "{\"key11\":\"value11\",\"key22\":\"value22\"},{\"key111\":\"value111\",\"key222\":\"value222\"}]}")
        );

        add(new RunRequest("宽松模式校验，如果返回值的map中多出来一个值，预期是没有的，校验可以通过")
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson(
                        "{'foo2': 'bar23','jsondata':[" +
                                "{'key1':'value1','key2':'value2'}," +
                                "{'key11':'value11','key22':'value22'}," +
                                "{'key111':'value111','key222':'value222'," +
                                //比预期多出来如下内容
                                "'key333':'value333'}" +
                                "],\"foo4\":\"value444\"}"
                )                .validate()
                .assertEqual("status_code", 200)
                .jsonEqual("body.json","{\"foo2\": \"bar23\",\"jsondata\":[{\"key1\":\"value1\",\"key2\":\"value2\"}," +
                        "{\"key11\":\"value11\",\"key22\":\"value22\"},{\"key111\":\"value111\",\"key222\":\"value222\"}]}")
        );

        add(new RunRequest("宽松模式校验，如果list中多出来值，校验不可以通过")
                .post("/post")
                .withHeaders(
                        "{'Content-Type': 'application/json'}"
                )
                .withJson(
                        "{'foo2': 'bar23','jsondata':[" +
                                "{'key1':'value1','key2':'value2'}," +
                                "{'key11':'value11','key22':'value22'}," +
                                "{'key111':'value111','key222':'value222'}" +
                                "]}"
                )                .validate()
                .assertEqual("status_code", 200)
                .jsonNotEqual("body.json","{\"foo2\": \"bar23\",\"jsondata\":[{\"key1\":\"value1\",\"key2\":\"value2\"}," +
                        "{\"key11\":\"value11\",\"key22\":\"value22\"},{\"key111\":\"value111\",\"key222\":\"value222\"}]," +
                        "\"foo3\":\"value333\" }")
        );



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
