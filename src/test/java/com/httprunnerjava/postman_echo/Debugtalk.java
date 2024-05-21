package com.httprunnerjava.postman_echo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.httprunnerjava.Loader;
//import com.httprunnerjava.postman_echo.gitignore.po.TobBusinessAccount;
import com.httprunnerjava.utils.CSVFileUtil;
import okhttp3.*;

import java.io.*;
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

//    public Map<String,Object> getAllOrgAccountIdsMap() {
//        List<Long> allAccountIds = getAllOrgAccountIds("http://xxxx.k8s-txyun-prod.xxxx.com");
//        Map<String,Object> result = new HashMap<>();
//        result.put("orgId", allAccountIds);
//
//        return result;
//    }

//    public List<Long> getAllOrgAccountIds(String saasBusinessAccountUrl){
//        List<Long> result = new ArrayList<>();
//        Long startAccountId = 0L;
//
//        int i = 0;
//        while(i<100){
//            try {
//                String resultStr = "";
//                OkHttpClient client = new OkHttpClient();
//                RequestBody body = RequestBody.create(String.format("{\"accountId\":%s,\"fields\":[\"accountId\"],\"num\":2000}",startAccountId), JSON_BODY_MEDIA);
//
//                Request request = new Request.Builder()
//                        .url(saasBusinessAccountUrl + "/xxxx/xxxx-account/api/tob-xxxx-account-service/xxxx")
//                        .post(body)
//                        .build();
//
//                Response response = client.newCall(request).execute();
//                resultStr = response.body().string();
//                List<TobBusinessAccount> accountInfoList = JSONArray.parseArray(resultStr, TobBusinessAccount.class);
//                if(CollectionUtils.isEmpty(accountInfoList)){
//                    break;
//                }
//                List<Long> accountIdList= accountInfoList.stream().map(TobBusinessAccount::getAccountId).collect(Collectors.toList());
//                startAccountId = Collections.max(accountIdList);
//                result.addAll(accountIdList);
//            }catch (Exception e){
//                System.out.println("error");
//            }
//
//            i++;
//        }
//
//        return result;
//    }

//    public Map<String,Object> getAllOrgUserIdsMap() {
//        List<Long> allUserIds = getAllOrgUserIds("http://xxxx.k8s-txyun-prod.xxxx.com");
//        Map<String,Object> result = new HashMap<>();
//        result.put("userId", allUserIds);
//
//        return result;
//    }

//    public List<Long> getAllOrgUserIds(String saasBusinessAccountUrl){
//        List<Long> result = new ArrayList<>();
//        Long startAccountId = 0L;
//
//        int i = 0;
//        while(i<100){
//            try {
//                String resultStr = "";
//                OkHttpClient client = new OkHttpClient();
//                RequestBody body = RequestBody.create(String.format("{\"accountId\":%s,\"fields\":[\"accountId\",\"userId\"],\"num\":1000}",startAccountId), JSON_BODY_MEDIA);
//
//                Request request = new Request.Builder()
//                        .url(saasBusinessAccountUrl + "/xxxx/business-account/api/tob-xxxx-account-service/xxxx")
//                        .post(body)
//                        .build();
//
//                Response response = client.newCall(request).execute();
//                resultStr = response.body().string();
//                List<TobBusinessAccount> accountInfoList = JSONArray.parseArray(resultStr, TobBusinessAccount.class);
//                if(CollectionUtils.isEmpty(accountInfoList)){
//                    break;
//                }
//                List<Long> userIdList= accountInfoList.stream().map(TobBusinessAccount::getUserId).collect(Collectors.toList());
//                List<Long> accountIdList= accountInfoList.stream().map(TobBusinessAccount::getAccountId).collect(Collectors.toList());
//                startAccountId = Collections.max(accountIdList);
//                result.addAll(userIdList);
//            }catch (Exception e){
//                System.out.println("error");
//            }
//
//            i++;
//        }
//
//        return result;
//    }

    public static String getRandomStrings(String csvFile, String count) {
        String newCsvFilePath =  csvFile.replace("/","\\");
        //只支持加载resource目录下的文件
        InputStream inputStream = Loader.class.getClassLoader().getResourceAsStream(newCsvFilePath);

        List<String> allLines = CSVFileUtil.getLines(inputStream, "UTF-8");

        List<String> randomStrings = new ArrayList<>();
        int size = allLines.size();
        Random random = new Random();

        Integer countNumber= Integer.valueOf(count);

        for (int i = 0; i < countNumber; i++) {
            int randomIndex = random.nextInt(size);
            String randomString = allLines.get(randomIndex);
            randomStrings.add(randomString);
        }

        return JSON.toJSONString(randomStrings);
    }


    /**
     * 把一个文本文件转为csv文件，文件的来源为cap-plan录制的流量，格式比较固定
     * @param inputFilePath
     * @param outputFilePath
     */
    public static void convertTextToCsv(String inputFilePath, String inputFilePath2, String outputFilePath) {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(inputFilePath));
             BufferedReader reader2 = new BufferedReader(new FileReader(inputFilePath2));
             FileWriter writer = new FileWriter(outputFilePath)) {

            // 添加首行内容
            writer.append("method,url,accept,acceptEncoding,acceptLanguage,connection,cookie,useAgent,params,body,contentType,qunheid,xToolName,xQhUserId,xQhAccountId,unknow\n");

            String line;
            while ((line = reader1.readLine()) != null) {
                String[] fields = line.split("\\|\\|\\|",-1);
                for (int i = 0; i < fields.length; i++) {
                    writer.append(escapeSpecialCharacters(fields[i]));
                    if (i != fields.length - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }

            while ((line = reader2.readLine()) != null) {
                String[] fields = line.split("\\|\\|\\|",-1);
                for (int i = 0; i < fields.length; i++) {
                    writer.append(escapeSpecialCharacters(fields[i]));
                    if (i != fields.length - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }

            System.out.println("转换完成！");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String escapeSpecialCharacters(String field) {
        if (field.contains(",")) {
            field = field.replace("\"","\"\"");
            field = "\"" + field + "\"";
        }
        return field;
    }

}
