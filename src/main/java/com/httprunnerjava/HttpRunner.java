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

import com.google.common.base.Strings;
import com.httprunnerjava.exception.HrunBizException;
import com.httprunnerjava.exception.HrunExceptionFactory;
import com.httprunnerjava.exception.ParamsError;
import com.httprunnerjava.exception.ValidationFailureException;
import com.httprunnerjava.model.*;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

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
                            log.info("config中未指定debugtalk文件位置，默认获取测试执行类所在目录及逐级上层目录，其次取HttpRunnerForJava包下的默认Debugtal文件");
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
                "Start to run testcase: {}, TestCase ID: {}",
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

        extractedVariables = new Variables().update(getConfig().getVariables());
    }


    /**
     * 初始化tConfig和tTestSteps对象
     * @param step 实际执行过程中的步骤
     * @param params TODO: 暂时无用
     */
    @Test(dataProvider = "HrunDataProvider")
    public void testStart(Step step, Map<String, Object> params) {
        Variables configVariables = getConfig().getVariables();
        if (params != null && !params.isEmpty()) {
            configVariables.update(params);
        }

        // override variables
        // step variables > extracted variables from previous steps
        step.setVariables(Variables.mergeVariables(getConfig().getVariables(), step.getVariables()));
        // step variables > testcase config variables
        step.setVariables(Variables.mergeVariables(sessionVariables, step.getVariables()));

        step.setVariables(Variables.mergeVariables(extractedVariables, step.getVariables()));

        step.setVariables(step.getVariables().parse(projectMeta.getFunctions()));

        Map<String,Object> extractMapping = null;
        try {
            if(getUseAllure()) {
                Allure.step(String.format("step: %s", step.getName()));
            }
            extractMapping = this.runStep(step);
        } catch (Exception e){
            log.error("用例执行过程中出现错误，请检查！");
            throw e;
        }

        extractedVariables.update(extractMapping);
    }

    @DataProvider(name = "HrunDataProvider")
    public Iterator<Object[]> createData() {
        List<Object[]> steps = new ArrayList<>();
        for (Step step : this.getTeststeps()) {
            steps.add(new Step[]{step, null});
        }
        return steps.iterator();
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
            HrunExceptionFactory.create("E0002");
        }

        stepDatas.add(stepData);
        log.info("run step end: {} <<<<<<\n", step.getName());
        return stepData.getExportVars();
    }

    public StepData runStepRequest(Step step) {
        ResponseObject.setCurrentRespBody(null);
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

        parsedRequestDict.getHeaders().setdefault("HRUN-Request-ID",
                String.format("HRUN-%s-%s", caseId, System.currentTimeMillis()));
        step.getVariables().update("request", parsedRequestDict);

        // prepare arguments
        MethodEnum method = parsedRequestDict.getMethod();
        String urlPath = parsedRequestDict.getUrl().getEvalString();

        String url = CommonUtils.buildUrl(getConfig().getBaseUrl().getRawValue(), urlPath);
        parsedRequestDict.setVerify(getConfig().getVerify());

        ResponseObject respObj = null;

        Response resp = session.request(method, url, parsedRequestDict);
        respObj = new ResponseObject(resp);

        step.getVariables().update("response", respObj);

        Optional.of(step.getTeardownHooks()).ifPresent(tearDownHooks->
                callHooks(tearDownHooks, step.getVariables(), "setup request")
        );

        HashMap<String, String> extractors = step.getExtract();
        Variables extractMapping = respObj.extract(extractors, step.getVariables(), projectMeta.getFunctions());
        stepData.setExportVars(extractMapping.toMap());

        Variables variablesMapping = step.getVariables();
        variablesMapping.update((extractMapping));

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
            }else
                log.error("比对过程中发生异常，当作比对不一致处理！");
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

    public void parseConfig(Config config) {
        getConfig().updateVariables(sessionVariables);
        getConfig().setVariables(
                getConfig().getVariables().parse(projectMeta.getFunctions())
        );
        getConfig().setName(
                getConfig().getName().parse(getConfig().getVariables(), projectMeta.getFunctions())
        );
        getConfig().setBaseUrl(
                getConfig().getBaseUrl().parse(getConfig().getVariables(), projectMeta.getFunctions())
        );
    }

    public void callHooks(Hooks hooks, Variables stepVariables, String hookMsg) {
        log.info("call hook actions: {}", hookMsg);
        for (Hooks.HookString hook : hooks.getContent()) {
            if (hook.getType() == 1) {
                // format 1: "${func()}"
                log.debug("call hook function: {}", hook.getFuncHook());
                try{
                    hook.getFuncHook().parse(stepVariables, projectMeta.getFunctions());
                }catch(Exception e){
                    if(!hook.getNoThrowException()) {
                        log.error("钩子函数执行异常，执行的钩子函数是：" + hook.toString());
                        throw e;
                    }
                    log.warn("钩子函数执行异常，但不会终端用例执行，出现错误的钩子函数是：" + hook.toString());
                }
            } else if(hook.getType() == 2 && hook.getMapHook().size() == 1) {
                // format 2: {"var": "${func()}"}
                Map.Entry<LazyString,LazyString> entry = hook.getMapHook().entrySet().iterator().next();
                try{
                    entry.getValue().parse(
                            stepVariables, projectMeta.getFunctions()
                    );
                }catch (Exception e){
                    if(!hook.getNoThrowException()) {
                        log.error("钩子函数执行异常，执行的钩子函数是：" + hook.toString());
                        throw e;
                    }
                    log.warn("钩子函数执行异常，但不会终端用例执行，出现错误的钩子函数是：" + hook.toString());
                }
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
                log.error("嵌套的testcase类中未包含config和teststep成员变量，或两者为空");
            }
        } catch (Exception e) {
            log.error("testcase嵌套内容执行失败，原始报错信息如下： \n " + HrunBizException.toStackTrace(e));
            throw new ParamsError(
                    "Invalid teststep referenced testcase" + step.toString()
            );
        }

        Optional.of(step.getTeardownHooks()).ifPresent(tearDownHooks->
                callHooks(tearDownHooks, stepVariables, "teardown testcase")
        );

        stepData.setTestCasedata(caseResult.getStepDatas());
        stepData.setExportVars(caseResult.getExportVariables().toMap());
        stepData.setSuccess(caseResult.getSuccess());
        this.success = caseResult.success;

        if (stepData.getExportVars() != null && stepData.getExportVars().size() != 0)
            log.info("export variables: {}", stepData.getExportVars());

        return stepData;
    }
    
    public Variables getExportVariables() {
        Export export_var_names;
        if (export == null || export.getContent().size() == 0)
            export_var_names = getConfig().getExport();
        else
            export_var_names = export;

        Variables export_vars_mapping = new Variables();
        for (String var_name : export_var_names.getContent()) {
            if (!sessionVariables.getContent().containsKey(var_name))
                HrunExceptionFactory.create("E0069");

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
        if(isProxy)
            log.warn("已开启代理模式，所有请求将请求到 127.0.0.1:8888，请确认代理服务器状态。");

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

}
