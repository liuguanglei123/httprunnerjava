package com.httprunnerjava;

import com.httprunnerjava.model.Config;
import lombok.Data;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

@Data
public class RetryAnalyzer implements IRetryAnalyzer {
    private int retryCount = 0;
    private Long waitTime = 10000L;


    @Override
    public boolean retry(ITestResult result) {
        // 失败重试时，默认等待10秒，让服务缓一缓
        try {
            Thread.sleep(Config.waitTime);
        } catch (InterruptedException e) {
        }
        if (retryCount < Config.maxRetryCount) {
            retryCount++;
            return true;
        }
        return false;
    }
}