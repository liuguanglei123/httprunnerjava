package com.httprunnerjava.postman_echo.functionalVerification;

import com.httprunnerjava.HttpRunner;
import com.httprunnerjava.postman_echo.ManualTest.ManualAllTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class ManualExecuteTest {

    @Test
    public void manualallSteps(){
        Map<String, Object> params = new HashMap<>();
        params.put("userId","5FXXXXX");
        ManualAllTest manualAllTest = new ManualAllTest();
        manualAllTest.manualExecuteAllTestStep(params);
    }
}
