/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the APACHE LICENSE, VERSION 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.httprunnerjava;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.httprunnerjava.annotation.Parameters;
import com.httprunnerjava.exception.HrunBizException;
import com.httprunnerjava.exception.HrunExceptionFactory;
import com.httprunnerjava.exception.ValidationFailureException;
import com.httprunnerjava.model.*;
import com.httprunnerjava.model.Enum.HookType;
import com.httprunnerjava.model.Enum.MethodEnum;
import com.httprunnerjava.model.component.atomsComponent.response.*;
import com.httprunnerjava.model.component.atomsComponent.request.*;
import com.httprunnerjava.model.component.moleculesComponent.TRequest;
import com.httprunnerjava.model.lazyLoading.LazyString;
import com.httprunnerjava.model.runningData.ResponseObject;
import com.httprunnerjava.utils.CommonUtils;
import io.qameta.allure.Allure;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.testng.annotations.*;

import java.util.*;
import java.util.Optional;

@Data
@Slf4j
public class HttpRunner {

    // 用例集（testcase）层级的参数化配置，其中可以包含 baseUrl，用例集层级的参数变量，导出字段等
    private Config config;

    // 用例集的步骤，步骤可以是单个step，也可以嵌套其他case
    private List<Step> teststeps;

    // 标注测试用例执行结果
    private Boolean success = false;

    // 指定的caseId，没有的话会自动生成一个uuid
    private String caseId;

    // 用例导出字段
    private Export export;

    //case中的每一步step，都会返回对应的数据，包括success，step的name，状态值等，统一存在该字段中
    private List<StepData> stepDatas = new ArrayList<>();

    // 会话上下文信息，可以用来保存cookie等，更高级的用法正在探索中
    private HttpSession session;

    //上下文信息中产生的变量，比如第一个接口中导出的A字段作为某个变量，可以在第二个接口请求中传入
    private Variables sessionVariables = new Variables();

    private static ProjectMeta projectMeta;

    // time
    private long startAt;
    private long endAt;
    private long duration;

    private Boolean useAllure = true;

    // 是否开启proxy模式，开启后会自动设置127.0.0.0:8888代理，所有的请求都会经过代理转发
    private Boolean isProxy = false;

    // 这个字段是原版没有的，增加的原因是原版的用例执行，完全是在httprunner内部，而java的实现版本，中间跨越了testng框架
    // 为了将多个step间的变量串联起来，因此增加了这个字段
    private Variables extractedVariables;

    //TODO：这里只是很简单的用了一个随机值表示hashcode，需要探索下更好的实现方式，当前的目的仅仅是为了让同一个HttpRunner对象的hashcode值一致
    // 重写hashcode的目的，是因为testng中，会把当前的HttpRunner对象放进一个HashMap中，map的key是HttpRunner对象，Value是Collection<ITestNGMethod>
    // 根据hashmap的定义，map中的key，实际存储的是HttpRunner对象的hashcode，在case执行过程中，由于HttpRunner类对象变化导致hashcode变化，用例执行完时，根据原有的hashcode找不到value了
    // testng认为此时测试用例的执行方法为空，抛出异常 java.lang.AssertionError: l should not be null
    // 姑且认为，在用户执行过程中，hashcode的值是不能变的
    private Integer hashCode = new Random().nextInt(999999);

    /**
     * 初始化tConfig和tTestSteps对象
     */
    public void initCheck() {
        setConfig(
                Optional.ofNullable(getConfig())
                        .orElseGet(() -> new Config("default config"))
        );

        if( null == getTeststeps() || 0 == this.getTeststeps().size()){
            HrunExceptionFactory.create("E00001");
        }
    }

    /**
     * 用例执行前执行该方法，加载用例集的变量（类似全局变量一样）和 projectMeta 对象数据
     * projectMeta用于存储一些环境变量（env，暂时还没有用到）和 自定义方法类等信息
     */
    @BeforeClass
    public void beforeTestStart() {
        initCheck();
        projectMeta = Optional.ofNullable(projectMeta).orElseGet( () ->
                Loader.loadProjectMeta(
                    Optional.ofNullable(getConfig().getPath())
                        .orElseGet(() -> {
                            //TODO: debugtalk类文件位置当前无法指定，以后支持yml文件后可能会考虑支持
                            log.debug("config中未指定debugtalk文件位置，默认获取测试执行类所在目录及逐级上层目录，其次取HttpRunnerForJava包下的默认Debugtal文件");
                            return new LazyString(this.getClass().getPackage().getName());
                        }))
        );

        if (Strings.isNullOrEmpty(caseId)) {
            caseId = UUID.randomUUID().toString();
        }

        Variables configVariables = getConfig().getVariables();
        // TODO: 需要测试
        configVariables.update(sessionVariables);

        configVariables.update(projectMeta.getEnvVar());

        getConfig().setName(getConfig().getName().parse(
                configVariables, projectMeta.getFunctions()
        ));

        //TODO:下面这两行应该可以暂时不要，后面再调整一下
        //        Allure.getLifecycle().updateTestCase(result -> result.setName(__config.getName().getEvalString()));
        //        Allure.description(String.format("TestCase ID: %s", __case_id));

        log.info(
                "开始执行用例: {}, TestCaseID为: {}",
                getConfig().getName().getEvalString(), getCaseId()
        );

        beforeRunTestcase(new TestCase(getConfig(), getTeststeps()));
    }

    public void beforeRunTestcase(TestCase testcase) {
        parseConfig(getConfig());
        startAt = System.currentTimeMillis();
        stepDatas = new ArrayList<>();

        session = (
                session == null ? new HttpSession(this) : session
        );

        extractedVariables = new Variables();
    }

    /**
     * 初始化tConfig和tTestSteps对象
     * @param step 实际执行过程中的步骤
     * @param params 适用于手动执行某个测试集合（或某个步骤）的场景，用于往里传参，对于用例的扑通执行场景暂无意义
     */
    @Test(dataProvider = "HrunDataProvider")
    public void testStart(Step step, Map<String, Object> params) {
        Variables configVariables = getConfigVar();
        if (params != null && !params.isEmpty()) {
            configVariables.update(params);
        }

        // override variables
        // step variables > extracted variables from previous steps
        step.setVariables(Variables.mergeVariables(step.getVariables(), extractedVariables));
        // step variables > testcase config variables
        step.setVariables(Variables.mergeVariables(step.getVariables(), getConfigVar()));
        // step variables > testcase config variables
//        step.setVariables(Variables.mergeVariables(sessionVariables, step.getVariables()));

        step.setVariables(step.getVariables().parse(projectMeta.getFunctions()));

        Map<String,Object> extractMapping = null;
        try {
            if(getUseAllure()) {
                Allure.step(String.format("step: %s", step.getName()));
            }
            extractMapping = this.runStep(step);
        } catch (Exception e){
            log.error("用例执行过程中出现错误，用例尚未执行完成，请检查！");
            throw e;
        }

        extractedVariables.update(extractMapping);
    }

    /**
     * testng的参数构造器，已经支持 parameterized方法
     * @return
     */
    @DataProvider(name = "HrunDataProvider")
    public Iterator<Object[]> createData() {
        Parameters parameters = this.getClass().getAnnotation(Parameters.class);
        if(parameters == null){
            List<Object[]> steps = new ArrayList<>();
            for (Step step : this.getTeststeps()) {
                steps.add(new Object[]{step, null});
            }
            return steps.iterator();
        }

//        String file = parameters.file();
//        if(Strings.isNullOrEmpty(parameters.file())){
//            return null;
//        }

        String mapStr = parameters.mapStr();
        Map<String,Object> mapTemp = new HashMap<>();
        /**思路：
         * 1.需要先将mapstring解析成Map<String,Object>，使用fastjson处理吧
         *     循环处理每个Object对象，如果不是string，可以直接使用jsonarray进行解析
         *     如果不是list，那就匹配${function()},然后执行方法，获取结果
         *     如果匹配不到方法，那么直接作为object存储起来（这里可能是string，int long等多种类型）
         */
        if(!Strings.isNullOrEmpty(mapStr)){
            //parameter参数为一个完整的方法${function()}
            if(Parse.isValidFunc(mapStr)){
                log.debug("call parameters function: {}", mapStr);
                try{
                    LazyString function = new LazyString(mapStr);
                    function.parse(new Variables(projectMeta.getEnvVar()), projectMeta.getFunctions());
                    Object functResult = function.getParsedValue();

                    if(!(functResult instanceof Map)){
                        HrunExceptionFactory.create("E00008");
                    }

                    mapTemp = (Map<String,Object>)functResult;
                }catch(Exception e){
                    log.error("参数化函数执行异常，执行的函数是：" + mapStr);
                    throw e;
                }
            } else {
                try{
                    mapTemp = JSONObject.parseObject(mapStr,Map.class);
                }catch (Exception e){
                    log.error("mapStr解析为map错误，请检查");
                    throw e;
                }
                for(Map.Entry<String,Object> entry : mapTemp.entrySet()){
                    if(entry.getValue() instanceof List){
                        continue;
                    }else if(entry.getValue() instanceof String && Parse.isValidFunc(entry.getValue().toString())){
                        try{
                            LazyString function = new LazyString(entry.getValue().toString());
                            function.parse(new Variables(projectMeta.getEnvVar()), projectMeta.getFunctions());
                            Object functResult = function.getParsedValue();

                            if(!(functResult instanceof List)){
                                HrunExceptionFactory.create("E00009");
                            }

                            mapTemp = (Map<String,Object>)functResult;
                        }catch(Exception e){
                            log.error("参数化函数执行异常，执行的函数是：" + entry.getValue());
                            throw e;
                        }
                    }
                }
            }
            /*
            执行到这里，map的形式为：
            {
                "param1":"5SFXXXXXXXX",
                "param2":12345,
                "param3":["foo1","foo2"]
                "param4":["soo1","soo2"]
            },最终返回的结果，要进行一些笛卡尔积的计算
            [
                {"param1":"5SFXXXXXXXX","param2":12345,"param3":"foo1","param4":"soo1"}
                {"param1":"5SFXXXXXXXX","param2":12345,"param3":"foo1","param4":"soo2"}
                {"param1":"5SFXXXXXXXX","param2":12345,"param3":"foo2","param4":"soo1"}
                {"param1":"5SFXXXXXXXX","param2":12345,"param3":"foo2","param4":"soo2"}
            ]
             */
            /*
            首先将原始map转成如下形式
            [
                [{"param1":"5SFXXXXXXXX"}],
                [{"param2":12345}],
                [{"param3":"foo1"},{"param3":"foo2"}]
                [{"param4":"soo1"},{"param4":"soo2"}]
            ]
             */
            List<List<Map<String,Object>>> listTemp = new ArrayList<>();
            for(Map.Entry<String,Object> entry : mapTemp.entrySet()){
                if(entry.getValue() instanceof List){
                    List<Map<String,Object>> listTempIn = new ArrayList<>();
                    for(Object each : (List)entry.getValue()){
                        Map<String,Object> mapTempIn = new HashMap<>();
                        mapTempIn.put(entry.getKey(),each);
                        listTempIn.add(mapTempIn);
                    }
                    listTemp.add(listTempIn);
                }else{
                    Map<String,Object> mapTempIn = new HashMap<>();
                    mapTempIn.put(entry.getKey(),entry.getValue());
                    listTemp.add(Collections.singletonList(mapTempIn));
                }
            }

            List<Map<String,Object>> mapResult = CommonUtils.genCartesianProduct(listTemp);
            List<Object[]> steps = new ArrayList<>();
            for(Map<String,Object> each : mapResult){
                for (Step step : this.getTeststeps()) {
                    steps.add(new Object[]{step, each});
                }
            }

            return steps.iterator();
        }

        return null;
    }

    @AfterClass
    public void afterRunTestcase() {
        sessionVariables.update(extractedVariables);
        duration = System.currentTimeMillis() - startAt;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * 执行step，step类型可能是某个request，也可能是嵌套的其他testcase
     * @param step 待执行的用例
     * @return extract中的所有变量
     */
    public Map<String,Object> runStep(Step step) {
        log.info("run step begin: {} >>>>>>", step.getName());
        StepData stepData = null;

        if (step.getRequest() != null) {
            stepData = runStepRequest(step);
        } else if (step.getTestcase() != null) {
            stepData = runStepTestcase(step);
        } else {
            log.debug(step.toString());
            HrunExceptionFactory.create("E00004");
        }

        stepDatas.add(stepData);
        log.info("run step end: {} <<<<<<\n", step.getName());
        return stepData.getExportVars();
    }

    /**
     * 执行测试用例中的request部分
     * @param step 待执行的用例内容
     * @return StepData对象，包含一些执行数据和用例状态
     */
    public StepData runStepRequest(Step step) {
        StepData stepData = new StepData(step.getName());

        //TODO: 低优先级 deal upload request
        // prepare_upload_step(step,this.__project_meta.getFuntions());
        // request_dict.remove("upload");

        Optional.of(step.getSetupHooks()).ifPresent(setupHooks->
                callHooks(setupHooks, step.getVariables(), "setup request")
        );

        TRequest parsedRequestDict = step.getRequest().parse(
                step.getVariables(), projectMeta.getFunctions()
        );

        parsedRequestDict.getHeaders().set("hrunRequestID",
                String.format("HRUN-%s-%s", caseId, System.currentTimeMillis()));
        step.getVariables().update("request", parsedRequestDict);

        // prepare arguments
        MethodEnum method = parsedRequestDict.getMethod();
        String urlPath = parsedRequestDict.getUrl().getEvalString();

        String url = CommonUtils.buildUrl(getConfig().getBaseUrl().getRawValue(), urlPath);
        parsedRequestDict.setVerify(getConfig().getVerify());

        ResponseObject respObj;

        Response resp = session.request(method, url, parsedRequestDict);
        respObj = new ResponseObject(resp);

        step.getVariables().update("response", respObj);

        Optional.of(step.getTeardownHooks()).ifPresent(tearDownHooks->
                callHooks(tearDownHooks, step.getVariables(), "teardown request")
        );

        HashMap<String, String> extractors = step.getExtract();
        Variables extractMapping = respObj.extract(extractors, step.getVariables(), projectMeta.getFunctions());
        stepData.setExportVars(extractMapping.toMap());

        Variables variablesMapping = step.getVariables();
        variablesMapping.update(extractMapping);

        List<Validator> validators = step.getValidators();
        boolean sessionSuccess = false;
        try {
            respObj.validate(validators, variablesMapping, projectMeta.getFunctions());
            sessionSuccess = true;
        } catch (Throwable e) {
            if( e instanceof ValidationFailureException){
                log.error("结果比对存在不一致!");
                logReqRespDetails(url, method, parsedRequestDict, respObj);
                duration = System.currentTimeMillis() - startAt;
            }else {
                log.error("比对过程中发生异常，当作比对不一致处理！");
            }
            throw e;
        } finally {
            success = sessionSuccess;
            stepData.setSuccess(sessionSuccess);
            //TODO:            if hasattr(self.__session, "data"):
            //                # httprunner.client.HttpSession, not locust.clients.HttpSession
            //                # save request & response meta data
            //                self.__session.data.success = sessionSuccess
            //                self.__session.data.validators = resp_obj.validation_results
            //                # save step data
            //                stepData.data = self.__session.data
        }

        return stepData;
    }

    public void logReqRespDetails(String url,MethodEnum method,TRequest request,ResponseObject response){

        //log request
        String errMsg = "\n**************** 完整的请求和相应信息 ****************\n" +
                "**************** DETAILED REQUEST & RESPONSE ****************\n" +
                "\n====== request details ======\n" +
                "url: " + url + "\n" +
                "method: " + method.getMethod() + "\n" +
                request.logDetail() +
                "\n====== response details ======\n" +
                response.logDetail();

        log.error(errMsg);
    }

    /**
     * 解析config内容，比如将$var形式的变量转变成真正的值，还有将session中的变量覆盖到config变量中
     * @param config
     */
    public void parseConfig(Config config) {
        getConfig().updateVariables(sessionVariables);
        getConfigVar().parse(projectMeta.getFunctions());
        getConfigName().parse(getConfigVar(), projectMeta.getFunctions());
        getConfigBaseUrl().parse(getConfigVar(), projectMeta.getFunctions());
    }

    /**
     * 调用钩子函数
     * @param hooks 要执行的钩子函数对象
     * @param stepVariables 用例步骤变量，执行变量的时候可能用得到
     * @param hookMsg 钩子函数消息，一般传入“setup”或者“tearDown“表示钩子函数类型
     */
    public void callHooks(Hooks hooks, Variables stepVariables, String hookMsg) {
        log.info("call hook actions: {}", hookMsg);
        for (Hooks.HookString hook : hooks.getContent()) {
             if (hook.getType().equals(HookType.StringHook)) {
                // 待执行的钩子函数格式 1: "${func(...$param)}"
                log.debug("call hook function: {}", hook.getFuncHook());
                try{
                    hook.getFuncHook().parse(stepVariables, projectMeta.getFunctions());
                }catch(Exception e){
                    if(!hook.getNoThrowException()) {
                        log.error("钩子函数执行异常，执行的钩子函数是：" + hook.toString());
                        throw e;
                    }
                    log.warn("钩子函数执行异常，但不会终止用例执行，出现错误的钩子函数是：" + hook.toString());
                }
            } else if(hook.getType().equals(HookType.MapHook) && hook.getMapHook().size() == 1) {
                // 待执行的钩子函数格式 2: {"var": "${func(...$param)}"}
                Map.Entry<LazyString,LazyString> entry = hook.getMapHook().entrySet().iterator().next();
                try{
                    entry.getValue().parse(
                            stepVariables, projectMeta.getFunctions()
                    );

                    log.debug(
                            "call hook function: {}, got value: {}",
                            entry.getValue().getRawValue(),
                            entry.getValue().getEvalString()
                    );
                    log.debug(
                            "assign variable: {} = {}",
                            entry.getKey().getRawValue(),
                            entry.getValue().getEvalString()
                    );
                    stepVariables.put(
                            entry.getKey().getRawValue(),entry.getValue().getEvalValue()
                    );

                }catch (Exception e){
                    if(!hook.getNoThrowException()) {
                        log.error("钩子函数执行异常，执行的钩子函数是：" + hook.toString());
                        throw e;
                    }
                    log.warn("钩子函数执行异常，但不会终止用例执行，出现错误的钩子函数是：" + hook.toString());
                }

            }else{
                log.error("Invalid hook format: {}", hook);
            }
        }
    }

    /**
     * run teststep: referenced testcase
     */
    public StepData runStepTestcase(Step step) {
        StepData stepData = new StepData(step.getName());
        Variables stepVariables = step.getVariables();
        Export stepExport = step.getExport();

        // setup hooks
        Optional.of(step.getSetupHooks()).ifPresent(setupHooks->
                callHooks(setupHooks, stepVariables, "setup testcase")
        );

        HttpRunner caseResult = null;
        try {
            if (step.getTestcase().getDeclaredField("config") != null
                    && step.getTestcase().getDeclaredField("teststeps") != null) {
                Class<? extends HttpRunner> testcaseCls = step.getTestcase();

                caseResult = testcaseCls
                        .newInstance()
                        .withSession(session)
                        .withCaseId(caseId)
                        .withLocalDebug(config.getIsProxy())
                        .withVariables(stepVariables)
                        .withExport(stepExport)
                        .run();
            } else {
                log.error("嵌套的testcase类中未包含config或teststep成员变量，无法执行用例");
            }
        } catch (Exception e) {
            log.error("testcase嵌套内容执行失败，原始报错信息如下： \n " + e.getMessage());
            log.debug("testcase嵌套内容执行失败，原始报错信息如下： \n " + HrunBizException.toStackTrace(e));
            HrunExceptionFactory.create("E20010");
        }

        Optional.of(step.getTeardownHooks()).ifPresent(tearDownHooks->
                callHooks(tearDownHooks, stepVariables, "teardown testcase")
        );

        stepData.setTestCasedata(caseResult.getStepDatas());
        stepData.setExportVars(caseResult.getExportVariables().toMap());
        stepData.setSuccess(caseResult.getSuccess());
        this.success = caseResult.success;

        if (stepData.getExportVars() != null && stepData.getExportVars().size() != 0) {
            log.info("export variables: {}", stepData.getExportVars());
        }

        return stepData;
    }
    
    public Variables getExportVariables() {
        Export export_var_names;
        if (export == null || export.getContent().size() == 0) {
            export_var_names = getConfig().getExport();
        } else {
            export_var_names = export;
        }

        Variables export_vars_mapping = new Variables();
        for (String var_name : export_var_names.getContent()) {
            if (!sessionVariables.getContent().containsKey(var_name)) {
                log.error("需要导出的变量 " + var_name +" 不存在");
                HrunExceptionFactory.create("E20011");
            }

            export_vars_mapping.getContent().put(var_name, sessionVariables.get(var_name));
        }

        return export_vars_mapping;
    }

    public HttpRunner run() {
        TestCase testcase_obj = new TestCase(getConfig(), getTeststeps());
        return runInlineTestcase(testcase_obj);
    }

    public HttpRunner runInlineTestcase(TestCase testcase) {
        beforeTestStart();

        for (Step step : getTeststeps()) {
            testStart(step, null);
        }

        sessionVariables.update(extractedVariables);
        duration = System.currentTimeMillis() - startAt;

        return this;
    }

    public HttpRunner withSession(HttpSession session) {
        this.session = session;
        return this;
    }

    public HttpRunner withCaseId(String case_id) {
        this.caseId = caseId;
        return this;
    }

    public HttpRunner withVariables(Variables variables) {
        sessionVariables = variables;
        return this;
    }

    public HttpRunner withExport(Export export) {
        this.export = export;
        return this;
    }

    public HttpRunner withLocalDebug(Boolean isProxy){
        if(isProxy) {
            log.warn("已开启代理模式，所有请求将请求到 127.0.0.1:8888，请确认代理服务器状态。");
        }

        this.isProxy = isProxy;
        return this;
    }

    // 手动执行teststep的入口
    // 手动执行所有的步骤
    public void manualExecuteAllTestStep(Map<String, Object> params){
        try{
            setUseAllure(false);

            beforeTestStart();

            for (Step step : getTeststeps()) {
                testStart(step, params);
            }
        }catch (Exception e) {
            log.error("手动执行case失败！");
            throw e;
        }
    }

    public StepData manualExecuteSingleTeststep(Integer index, Map<String, Object> params){
        try{
            setUseAllure(false);
            beforeTestStart();
            testStart(getTeststeps().get(index), params);
            return stepDatas.get(0);
        }catch (Exception e) {
            log.error("手动执行单步case失败！");
            throw e;
        }
    }

    public static ProjectMeta getProjectMeta(){
        return projectMeta;
    }

    public Variables getConfigVar(){
        return getConfig().getVariables();
    }

    public LazyString getConfigName(){
        return getConfig().getName();
    }

    public LazyString getConfigBaseUrl(){
        return getConfig().getBaseUrl();
    }

}
