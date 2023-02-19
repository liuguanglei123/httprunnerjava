# 欢迎使用 HttpRunner For Java

------

[HttpRunner](https://github.com/httprunner/httprunner)
是一款优秀的接口自动化测试框架，目前已经演变了三个大版本，其主要支持的语言是Go和Python，JAVA下目前并没有类似的工具可用。HttpRunner For Java项目主要基于HttpRunner的3.x版本，实现了JAVA版本的HttpRunner，并继承了原版的优秀设计。

## 核心特性
> * 支持API接口的多种请求方法，包括 GET/POST/HEAD/PUT/DELETE 等（目前实现了前两种，其他正在开发中）
> * 测试用例描述方式具有表现力，可采用简洁的方式描述输入参数和预期输出结果
> * 接口测试用例具有可复用性，便于创建复杂测试场景
> * 测试结果统计报告采用Allure简洁清晰，附带详尽日志记录，包括接口请求耗时、请求响应数据等
> * 具有可扩展性
``
### 简单的case样例：
#### 1.单一测试步骤
```
        new RunRequest("get with params")
        //定义变量值，变量值可以从自定义方法获取
                .with_variables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}'}")
                .get("/get")
                //http请求参数，支持自定义方法和变量
                .with_params("{'foo1': '$foo1', 'foo2': '$foo2', 'sum_v': '$sum_v'}")
                //请求header，支持自定义方法和变量
                .with_headers("{'User-Agent': 'HttpRunner/${get_httprunner_version()}'}")
                .extract()
                //请求响应体结果导出
                .with_jmespath("body.args.foo2", "foo3")
                .validate()
                //结果校验
                .assert_equal("status_code", 200)
                .assert_equal("body.args.foo1", "bar11")
                .assert_equal("body.args.sum_v", "1002")
                .assert_equal("body.args.foo2", "bar21")
```


#### 2.case嵌套：
```
new Step(
        new RunTestCase("request with functions")
            //自定义变量
            .with_variables("{'foo1': 'testcase_ref_bar1', 'expect_foo1': 'testcase_ref_bar1'}")
            //钩子函数
            .setup_hook("${sleep(0.1)}")
            //嵌套的测试方法的类，支持全量步骤调用，也支持单一步骤调用
            .call(request_with_functions_test.class)
            .teardown_hook("${sleep(0.2)}")
            //导出变量，可以输出或给后续步骤使用
            .export("['foo3']")
)

```

## 接口测试的核心要素
对于基础的接口测试框架，最最核心的要素可以概括为
> * 发起接口请求（Request）
> * 解析接口响应（Response）
> * 检查接口测试结果

项目一期首先支持http请求，java下的http请求框架非常多，最终选择了okHttp作为请求的客户端使用。

## 测试用例执行引擎
case样例上文已经展示，拆解case中的各个参数很简单，但是如何实现变量的传递和自定义方法的执行呢？

### 1.接口内容存储
首先设计多种成员类，比如header部分设计Header类,变量参数部分设计Variables类，可解析的类，继承ParseableIntf接口，这样可解析的部分就可以简化为：
```
class TStep{
    private Header header;
    private Param param;
    private Header header;
    private Variables var;
    
    @override
    public ParseableIntf to_value() {
        //needParseMember包含了所有需解析的成员变量
        //to_value是所有ParseableIntf接口实现类都要实现的一个方法
        this.needParseMember.forEach(e ->
            Optional.ofNullable(e)).to_value()
        );
        return this;
    }
}
```

### 2.复杂的to_value方法实现
上一步方案把所有的内容解析，都放在了to_value中，to_value如何实现呢？

首先可以确认，to_value的实现离不开a)上下文中的变量 b)自定义方法的实现类，所以方法的传参一定是
```
public ParseableIntfCls to_value(Variables variables_mapping, Class function) {}
```

### 3.所有用例中传入的参数都是strting，如何解析？
上文的case样例中，所有传参都是string格式，比如
```with_variables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}', 'foo4': '$foo5',}")```
首先依照原版约定，自定义变量的表现形式为$variables,自定义函数的表现形式为${function(var1,var2)}
对于 header param 一类的内容，内部可以用一个简单map存储内容
```
private HashMap<String, LazyContent> content = new HashMap<>();
```
其中key为变量名，value为待解析的值，以上面with_variables参数为例

待解析的值包括两个，${sum_two(1,2)} 和 $foo5

设计两个类：LazyContent 和 LazyString extends LazyContent，后者是前者的子类

LazyContent用来存放不许解析的非字符串类，LazyString用来存放需要解析的字符串类，如果LazyString中可以匹配到自定义变量和方法的正则，则进行处理，否则跳过。

为何取名LazyString？因为变量会在实际使用到时才会进行解析，是一种懒解析形式。

### 4.如何实现参数化用例
在自动化测试中，经常会遇到如下场景：
> * 测试搜索功能，只有一个搜索输入框，但有10种不同类型的搜索关键字；
> * 测试账号登录功能，需要输入用户名和密码，按照等价类划分后有20种组合情况。

可以使用testng的参数化方法，构造重复的用户数据
详情可以参考项目test目录下的request_with_parameters_test类
```
public static List<Map<String,Object>> getParams() {
        return Parse.parse_parameters(new HashMap<String, Object>() {{
                                          put("user_agent", Arrays.asList("iOS/10.1", "iOS/10.2"));
                                          put("username-password", "${parameterize(request_methods/account.csv)}");
                                          put("app_version", "${get_app_version()}");
                                      }}
        );
    }

    @Override
    @DataProvider(name="HrunDataProvider")
    public Iterator<Object[]> createData(){
        List<Object[]> users = new ArrayList<>();
        for (TStep u : this.get__teststeps()) {
            for(Map<String,Object> each : getParams()){
                users.add(new Object[]{u,each});
            }
        }

        return users.iterator();
    }
```

### 5.实现 hook 机制
在自动化测试中，通常在测试开始前需要做一些预处理操作，以及在测试结束后做一些清理性的工作。

例如，测试使用手机号注册账号的接口：

> * 测试开始前需要确保该手机号未进行过注册，常用的做法是先在数据库中删除该手机号相关的账号数据（若存在）；
> * 测试结束后，为了减少对测试环境的影响，常用的做法是在数据库中将本次测试产生的相关数据删除掉。

可以使用testng自带的@beforclass @beforetest等注解，也可以使用上面样例中的

setup_hook 和 teardown_hook方法，同样支持自定义方法
