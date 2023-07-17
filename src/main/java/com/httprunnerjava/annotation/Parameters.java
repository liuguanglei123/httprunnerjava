package com.httprunnerjava.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameters {

    /**从文件中加载map待实现，但是这里限定只能加载csv类型的文件，且文件第一行需要是标题
     * csvFile指的是待加载文件的路径，从根节点开始
     */
    String csvFile() default "";

    /* mapStr可以是如下类型的字符串
        {
            "param1":"5SFXXXXXXXX",
            "param2":12345,
            "param3":["foo1","foo2"],
            "param4":["soo1","soo2"]
        }，
     也可以是一个函数${function{param1,param2)}，但是函数的返回值一定是上述格式，否则会产生意料之外的结果
     */
    String mapStr() default "";

}
