package com.httprunnerjava.model;

import com.httprunnerjava.DefaultDebugtalk;
import com.httprunnerjava.model.component.atomsComponent.request.Variables;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProjectMeta {

    private String RootDir;

    private static Class<?> functions;

    public void setFunctions(Class<?> cls) {
        functions = cls;
    }

    public Class<?> getFunctions() {
        return functions;
    }

    private Map<String,Object> envVar = new HashMap<>();

    public ProjectMeta() {
        //TODO:默认加载debugtalk作为内部function对象
        functions = DefaultDebugtalk.class;
    }

}
