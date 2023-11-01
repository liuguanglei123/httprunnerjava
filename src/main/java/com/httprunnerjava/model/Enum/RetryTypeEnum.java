package com.httprunnerjava.model.Enum;

public enum RetryTypeEnum {
    SINGLESTEP("SINGLESTEP","单步重试"),

    ALLSTEP("ALLSTEP","全部重试"),

    NORETRY("NORETRY","不重试");

    private final String retryType;

    private final String retryDescription;

    RetryTypeEnum(String retryType,String retryDescription) {
        this.retryType = retryType;
        this.retryDescription = retryDescription;
    }

    public String getRetryType(){
        return this.retryType;
    }

    public String getRetryDescription(){
        return this.retryDescription;
    }

    public static RetryTypeEnum getRetryTypeEnum(String retryType){
        for (RetryTypeEnum c : RetryTypeEnum.values()) {
            if (c.getRetryType().equals(retryType)) {
                return c;
            }
        }
        return null;
    }
}
