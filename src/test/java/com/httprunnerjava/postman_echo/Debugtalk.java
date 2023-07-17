package com.httprunnerjava.postman_echo;

import com.alibaba.fastjson.JSONArray;
import com.httprunnerjava.postman_echo.gitignore.po.TobBusinessAccount;
import okhttp3.*;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;


import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

public class Debugtalk {

    public static final MediaType JSON_BODY_MEDIA
            = MediaType.get("application/json; charset=utf-8");

    private static String __version__ = "2.0.0";

    public static String get_httprunner_version(){
        return __version__;
    }

    public static List<String> get_app_version(){
        return Arrays.asList("3.1","3.0");
    }

    public static Integer sum_two(String m, String n) {
        return Integer.valueOf(m) + Integer.valueOf(n) + 999;
    }

    public static Double sum_two_double(String m, String n) {
        return Double.valueOf(m) + Double.valueOf(n);
    }

    public static String funcWithoutParam(){
        return "android_chuizi";
    }

    public static String funcWithParam(String var1,String var2){
        return var1+var2;
    }

    public void setup_hooks(){
        System.out.println("setup_hooks execute");
    }

    public void setup_testsuite(){
        System.out.println("setup_testsuite execute");
    }

    public void setup_testcase(){
        System.out.println("setup_testcase execute");
    }

    public void setup_api(){
        System.out.println("setup_api execute");
    }

    public void teardown_hooks(){
        System.out.println("teardown_hooks execute");
    }

    public void teardown_testsuite(){
        System.out.println("teardown_testsuite execute");
    }

    public void func2(){
        System.out.println("func2 execute");
    }

    public void sleep(String n_secs){
        System.out.println("start sleep!");
    }

    public String getAccountId(String accountId){
        return accountId;
    }

    public String getUserId(String uId){
        return uId;
    }

    public String toStr(Object obj){
        return String.valueOf(obj);
    }

    public Map<String,Object> parameterUsers(String startNum, String num){
        Integer startNums = Integer.valueOf(startNum);
        Integer nums = Integer.valueOf(num);
        Integer i=0;

        Map<String,Object> result = new HashMap<>();
        List<Integer> allUsers = new ArrayList<>();
        while(i<=nums){
            allUsers.add(startNums);
            startNums++;
            i++;
        }

        result.put("userId",allUsers);

        return result;
    }

    public Map<String,Object> getAllOrgAccountIdsMap() {
        List<Long> allAccountIds = getAllOrgAccountIds("http://saas-business-account-prodtest.k8s-txyun-prod.qunhequnhe.com");
        Map<String,Object> result = new HashMap<>();
        result.put("orgId", allAccountIds);

        return result;
    }

    public List<Long> getAllOrgAccountIds(String saasBusinessAccountUrl){
        List<Long> result = new ArrayList<>();
        Long startAccountId = 0L;

        int i = 0;
        while(i<100){
            try {
                String resultStr = "";
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(String.format("{\"accountId\":%s,\"fields\":[\"accountId\"],\"num\":2000}",startAccountId), JSON_BODY_MEDIA);

                Request request = new Request.Builder()
                        .url(saasBusinessAccountUrl + "/saas/business-account/api/tob-business-account-service/get-all-inservice-root-account-info")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resultStr = response.body().string();
                List<TobBusinessAccount> accountInfoList = JSONArray.parseArray(resultStr, TobBusinessAccount.class);
                if(CollectionUtils.isEmpty(accountInfoList)){
                    break;
                }
                List<Long> accountIdList= accountInfoList.stream().map(TobBusinessAccount::getAccountId).collect(Collectors.toList());
                startAccountId = Collections.max(accountIdList);
                result.addAll(accountIdList);
            }catch (Exception e){
                System.out.println("error");
            }

            i++;
        }

        return result;
    }
}
