package com.httprunnerjava.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

public class ExceptionDefinitions {
    // 单例
    private volatile static ExceptionDefinitions exceptionDefinitions;

    private final Logger logger = LoggerFactory.getLogger(ExceptionDefinitions.class);

    private Properties exceptionDefinitionProps;

    /**
     * Creates a new instance of ExceptionDefinitions. Description
     */
    private ExceptionDefinitions() {
    }


    private Properties getDefinitions() throws IOException {
        if (this.exceptionDefinitionProps == null) {
            this.exceptionDefinitionProps = new Properties();
            InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(this.getClass().getResourceAsStream("/ErrorCode.properties")), StandardCharsets.UTF_8);
            this.exceptionDefinitionProps.load(reader);
        }
        return this.exceptionDefinitionProps;
    }


    public String getExceptionMessage(String errorCode) {

        String message = "";
        try {
            message = (String) getDefinitions().get(errorCode);
        } catch (IOException e) {
            this.logger.error(String.format("Error message for [code=%s] is not defined", errorCode));
            logger.error(String.valueOf(e.getStackTrace()));
        }
        if (message.equals("")) {
            message = String.format("系统错误[ErrorType = ERROR_MESSAGE_DEFINITION, ErrorCode=%s]", errorCode);
        }
        return message;
    }

    public static ExceptionDefinitions getExceptionDefinitions() {
        if (exceptionDefinitions == null) {
            synchronized (ExceptionDefinitions.class) {
                if (exceptionDefinitions == null) {
                    exceptionDefinitions = new ExceptionDefinitions();
                }
            }
        }
        return exceptionDefinitions;
    }
}
