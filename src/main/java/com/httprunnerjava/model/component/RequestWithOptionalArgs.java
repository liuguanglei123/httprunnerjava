package com.httprunnerjava.model.component;

import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.atomsComponent.request.*;
import com.httprunnerjava.model.lazyLoading.LazyString;

public class RequestWithOptionalArgs extends Step{
    public RequestWithOptionalArgs(Step stepContext){
        super(stepContext);
    }

    public RequestWithOptionalArgs withParams(String paramsStr){
        getRequest().getParams().update(new Params(paramsStr));
        return this;
    }

    public RequestWithOptionalArgs withHeaders(String srcHeaders){
        getRequest().getHeaders().update(new Headers(srcHeaders));
        return this;
    }

    public RequestWithOptionalArgs setVerify(boolean verify){
        getRequest().setVerify(verify);
        return this;
    }

    public RequestWithOptionalArgs setAllowRedirects(boolean allowRedirects){
        getRequest().setAllowRedirects(allowRedirects);
        return this;
    }

    public RequestWithOptionalArgs withData(String data){
        getRequest().setData(new LazyString(data));
        return this;
    }

    public RequestWithOptionalArgs withJson(String json){
        getRequest().setReqJson(new ReqJson(json));
        return this;
    }

    public StepRequestValidation validate(){
        return new StepRequestValidation(this);
    }

    public StepRequestExtraction extract(){
        return new StepRequestExtraction(this);
    }

    public RequestWithOptionalArgs teardownHook(String hook){
        getTeardownHooks().add(hook);
        return this;
    }

    public RequestWithOptionalArgs teardownHookNoThrowException(String hook){
        getTeardownHooks().addNoThrowException(hook);
        return this;
    }
}
