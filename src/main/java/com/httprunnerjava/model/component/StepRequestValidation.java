package com.httprunnerjava.model.component;


import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.atomsComponent.response.Validator;

public class StepRequestValidation extends Step{

    public StepRequestValidation(Step stepContext){
        super(stepContext);
    }

    public StepRequestValidation assertEqual(String jmesPath, Object expectedValue){
        return assertEqual(jmesPath,expectedValue,null);
    }

    public StepRequestValidation assertEqual(String jmesPath, Object expectedValue, String message) {
        getValidators().add(new Validator("objectEequals",jmesPath,expectedValue,message));
        return this;
    }

    public StepRequestValidation jsonEqual(String jmesPath, Object expectedValue){
        getValidators().add(new Validator("jsonEquals",jmesPath, expectedValue, null));
        return this;
    }

    public StepRequestValidation jsonEqual(String jmesPath, Object expectedValue, String excludeItemStr){
        getValidators().add(new Validator("jsonEquals",jmesPath, expectedValue, null, excludeItemStr));
        return this;
    }

    public StepRequestValidation listEmpty(String jmesPath){
        return listEmpty(jmesPath, null);
    }

    public StepRequestValidation listEmpty(String jmesPath, String message) {
        getValidators().add(new Validator("listEmpty",jmesPath, null, message));
        return this;
    }

    public StepRequestValidation notListEmpty(String jmesPath){
        return notListEmpty(jmesPath, null);
    }

    public StepRequestValidation notListEmpty(String jmesPath, String message) {
        getValidators().add(new Validator("notListEmpty",jmesPath, null, message));
        return this;
    }

    public StepRequestValidation listContains(String jmesPath, Object expectedValue){
        return listContains(jmesPath,expectedValue, null);
    }

    public StepRequestValidation listContains(String jmesPath, Object expectedValue, String message) {
        getValidators().add(new Validator("listContains",jmesPath,expectedValue,message));
        return this;
    }

    public StepRequestValidation listSize(String jmesPath, Object expectedValue) {
        getValidators().add(new Validator("listSize",jmesPath,expectedValue, null));
        return this;
    }

    public StepRequestValidation assertTypeMatch(String jmesPath, Object expectedValue){
        return typeMatch(jmesPath,expectedValue,null);
    }

    public StepRequestValidation typeMatch(String jmesPath, Object expectedValue, String message) {
        getValidators().add(new Validator("typeMatch",jmesPath,expectedValue,message));
        return this;
    }
}
