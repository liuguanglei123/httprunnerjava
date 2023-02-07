package com.httprunnerjava.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class ClassUtils {
    /**
     * 获取某包下（包括该包的所有子包）的指定类
     *
     * @param packageName 包名
     * @return 类的完整名称
     */

    public static Class<?> getDefaultDebugtalkClass(String packageName){
        Class<?> ca = null;

        while(true){
            try{
                ca = Class.forName(packageName + ".Debugtalk");
                log.debug("在package: " + packageName + "下找到Debugtalk类，执行过程中的自定义方法将以该类内容为准");
            }
            catch (Exception e){
                int lastIndex = packageName.lastIndexOf(".");
                if (lastIndex == -1) {
                    break;
                }
                packageName = packageName.substring(0,lastIndex);
                continue;
            }
            return ca;
        }

        try{
            ca = Class.forName("com.httprunnerjava.DefaultDebugtalk");
        }catch (ClassNotFoundException e2){
            log.error("com.httprunnerjava.Debugtalk类未找到，请检查hrun.jar当前版本文件是否正确！");
        }

        return ca;
    }

}
