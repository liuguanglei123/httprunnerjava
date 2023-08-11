package com.httprunnerjava.model.component.moleculesComponent;

import com.httprunnerjava.model.Enum.MethodEnum;
import com.httprunnerjava.model.component.atomsComponent.request.Headers;
import com.httprunnerjava.model.component.atomsComponent.request.Params;
import com.httprunnerjava.model.component.atomsComponent.request.ReqJson;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import com.httprunnerjava.model.component.intf.LogAble;
import com.httprunnerjava.model.component.intf.ParseAble;
import com.httprunnerjava.model.lazyLoading.LazyContent;
import com.httprunnerjava.model.lazyLoading.LazyString;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
public class TRequest implements Serializable, ParseAble, LogAble {
    private MethodEnum method;
    private LazyString url;
    private Params params;
    private Headers headers;
    private ReqJson reqJson;
    private LazyString data;
    //TODO:
    // private Cookies cookies;
    private Float timeout;
    private boolean allowRedirects;
    private boolean verify;
    private Object upload; //TODO：上传文件

    public TRequest(String method, String url) {
        this.method = MethodEnum.getMethodEnum(method);
        this.url = new LazyString(url);
        this.headers = new Headers();
        this.params = new Params();
    }

    public void setData(LazyString data) {
        this.data = data;
    }

    public void setReqJson(ReqJson reqJson) {
        this.reqJson = reqJson;
    }

    public TRequest parse(Variables variablesMapping, Class functionsMapping) {
        List<ParseAble> parseAbles = new ArrayList<>(Arrays.asList(headers,params,reqJson,data,url));
        parseAbles.stream().forEach( each ->{
            Optional.ofNullable(each).ifPresent( e -> e.parse(variablesMapping, functionsMapping));
        });
        return this;
    }

    public String logDetail() {
        StringBuffer result = new StringBuffer("\n");

        List<Object> logObjects = new ArrayList<>(
                Arrays.asList(method,url,params,headers,reqJson,data,timeout,allowRedirects,verify,upload)
        );
        logObjects.stream().forEach( each ->{
            result.append(Optional.ofNullable(each).map( e -> e.toString()).orElse("NULL"));
            result.append("\n");
        });

        return result.toString();
    }
}