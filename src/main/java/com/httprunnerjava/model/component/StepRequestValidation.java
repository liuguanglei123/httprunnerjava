package com.httprunnerjava.model.component;


import com.httprunnerjava.model.Step;
import com.httprunnerjava.model.component.atomsComponent.response.Validator;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class StepRequestValidation extends Step{

    public StepRequestValidation(Step stepContext){
        super(stepContext);
    }

    public StepRequestValidation assertEqual(String jmesPath, Object expectedValue){
        return assertEqual(jmesPath,expectedValue,null);
    }

    public StepRequestValidation assertEqual(String jmesPath, Object expectedValue, String message) {
        getValidators().add(new Validator("objectEquals",jmesPath,expectedValue,message));
        return this;
    }

    /**
     * 宽松的比对模式，在map中，只要实际值包含了预期值中的key即可，但是list不可以扩展
     * @param jmesPath response中的返回值内容
     * @param expectedValue 期望值
     * @return
     */
    public StepRequestValidation jsonEqual(String jmesPath, String expectedValue){
        getValidators().add(new Validator("jsonEquals",jmesPath, expectedValue, null));
        return this;
    }

    /**
     * 严格的比对模式，在map中不允许扩展，必须完全一致
     * @param jmesPath response中的返回值内容
     * @param expectedValue 期望值
     * @return
     */
    public StepRequestValidation jsonEqualWithStrictMode(String jmesPath, String expectedValue){
        getValidators().add(new Validator("jsonEqualsWithStrictMode",jmesPath, expectedValue, null));
        return this;
    }

    //TODO:严格的顺序，不可以扩展等待实现

    /**
     * 严格的比对模式，在map中不允许扩展，必须完全一致
     * @param jmesPath response中的返回值内容
     * @param expectedValue 期望值
     * @return
     */
    public StepRequestValidation jsonNotEqual(String jmesPath, String expectedValue){
        getValidators().add(new Validator("jsonNotEquals",jmesPath, expectedValue, null));
        return this;
    }

//    public StepRequestValidation jsonEqual(String jmesPath, String expectedValue, String excludeItemStr){
//        getValidators().add(new Validator("jsonEquals",jmesPath, expectedValue, null, excludeItemStr));
//        return this;
//    }

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

    public StepRequestValidation listNotContains(String jmesPath, Object expectedValue){
        return listContains(jmesPath,expectedValue, null);
    }

    public StepRequestValidation listNotContains(String jmesPath, Object expectedValue, String message) {
        getValidators().add(new Validator("listNotContains",jmesPath,expectedValue,message));
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
