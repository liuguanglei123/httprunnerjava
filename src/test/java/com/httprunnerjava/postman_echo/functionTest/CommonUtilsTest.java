package com.httprunnerjava.postman_echo.functionTest;

import com.httprunnerjava.utils.CommonUtils;
import org.testng.annotations.Test;

import java.util.*;

public class CommonUtilsTest {
    @Test
    public void genCartesianProductTest(){
        /*
            [
                [{"param1":"5SFXXXXXXXX"}],
                [{"param2":12345}],
                [{"param3":"foo1"},{"param3":"foo2"}]
                [{"param4":"soo1"},{"param4":"soo2"}]
            ]
         */
        List<List<Map<String,Object>>> list1 = new ArrayList<>();
        Map<String,Object> map1 = new HashMap<>();
        map1.put("param1","5SFXXXXXXXX");
        list1.add(Collections.singletonList(map1));

        Map<String,Object> map2 = new HashMap<>();
        map2.put("param2",12345);
        list1.add(Collections.singletonList(map2));


        List<Map<String,Object>> list3 = new ArrayList<>();
        Map<String,Object> map3 = new HashMap<>();
        map3.put("param3","foo1");
        Map<String,Object> map31 = new HashMap<>();
        map31.put("param3","foo2");
        list3.add(map3);
        list3.add(map31);
        list1.add(list3);

        List<Map<String,Object>> list4 = new ArrayList<>();
        Map<String,Object> map4 = new HashMap<>();
        map4.put("param4","soo1");
        Map<String,Object> map41 = new HashMap<>();
        map41.put("param4","soo2");
        list4.add(map4);
        list4.add(map41);
        list1.add(list4);


        List<Map<String,Object>> result = CommonUtils.genCartesianProduct(list1);
        System.out.println(result.toString());

    }
}
