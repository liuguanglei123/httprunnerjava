package com.httprunnerjava.model.component.intf;

import com.httprunnerjava.model.component.atomsComponent.request.Variables;

public interface ParseAble {
    ParseAble parse(Variables variablesMapping, Class<?> functionsMapping);
}
