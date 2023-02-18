package com.httprunnerjava.model.component.moleculesComponent;

import com.httprunnerjava.model.Enum.MethodEnum;
import com.httprunnerjava.model.component.atomsComponent.request.Headers;
import com.httprunnerjava.model.component.atomsComponent.request.Params;
import com.httprunnerjava.model.component.atomsComponent.request.ReqJson;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
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
public class TRequest implements Serializable {
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
        Optional.ofNullable(headers).ifPresent(h -> h.parse(variablesMapping, functionsMapping));
        Optional.ofNullable(params).ifPresent(p -> p.parse(variablesMapping, functionsMapping));
        Optional.ofNullable(reqJson).ifPresent(r -> r.parse(variablesMapping, functionsMapping));
        Optional.ofNullable(data).ifPresent(d -> d.parse(variablesMapping, functionsMapping));
        Optional.ofNullable(url).ifPresent(u -> u.parse(variablesMapping, functionsMapping));
        return this;
    }

    public String logDetail() {
        return "\n" + "headers: " + Optional.ofNullable(headers).map(Headers::toString).orElse("NULL") + "\n" +
                "params: " + Optional.ofNullable(params).map(Params::toString).orElse("NULL") + "\n" +
                "reqJson: " + Optional.ofNullable(reqJson).map(ReqJson::toString).orElse("NULL") + "\n" +
                "data: " + Optional.ofNullable(data).map(LazyContent::toString).orElse("NULL") + "\n" +
                "timeout: " + Optional.ofNullable(timeout).map(Object::toString).orElse("NULL") + "\n" +
                "url: " + Optional.ofNullable(url).map(Object::toString).orElse("NULL") + "\n";
    }
}