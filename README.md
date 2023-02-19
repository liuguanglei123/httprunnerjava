# 欢迎使用 HttpRunner For Java

------

[HttpRunner](https://github.com/httprunner/httprunner)
是一款优秀的接口自动化测试框架，目前已经演变了三个大版本，其主要支持的语言是Go和Python，JAVA下目前并没有类似的工具可用。HttpRunner For Java项目主要基于HttpRunner的3.x版本，实现了JAVA版本的HttpRunner，并继承了原版的优秀设计思路。

## 核心特性
> * 支持API接口的多种请求方法，包括 GET/POST/PUT/DELETE（其他请求方式如HEAD/OPTION等正在开发）
> * 测试用例描述方式具有表现力，可采用简洁的方式描述输入参数和预期输出结果
> * 接口测试用例具有可复用性，便于创建复杂测试场景
> * 测试结果统计报告采用Allure简洁清晰，附带详尽日志记录，包括接口请求耗时、请求响应数据等
> * 具有良好的可扩展性
> * 复杂场景：基于 variables/extract/validate/hooks 机制可以方便地创建任意复杂的测试场景
> * 插件化机制：内置部分常用函数库，同时可以基于java编写自定义函数轻松实现更多能力
> * 利用testng的强大能力，可以轻松接入jenkins实现持续集成

### 简单的case样例：
#### 1.单一测试步骤
```
@Getter
public class SingleRequestStep extends HttpRunner {

    private Config config = new Config("测试用例示例")
            //可以在此指定调用域名
            .base_url("https://postman-echo.com");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunRequest("测试case1")
                //定义变量值，变量值可以从自定义方法获取，支持普通变量和函数变量
                .withVariables("{'foo1': 'bar11', sum_v': '${sum_two(1,2)}'}")
                .get("/get")
                //http请求参数，同样支持自定义方法和变量
                .withParams("{'foo1': '$foo1', 'sum_v': '$sum_v','num-param': 12345}")
                //http请求header，同样支持自定义方法和变量
                .withHeaders("{'User-Agent': 'HttpRunner/${get_httprunner_version()}'}")
                .validate()
                //一些常见的校验方法，支持json比对等复杂场景
                .assertEqual("status_code", 200)
                .assertEqual("body.args.foo1", "$foo1")
                .assertEqual("body.args.sum_v", "1002")
                .assertEqual("body.args.foo2", "$foo2")
        );
    }};
}
```


#### 2.case嵌套：
```
@Getter
public class TestCaseRefrenceStep extends HttpRunner {

    private Config config = new Config("testcase reference")
			//自定义上层变量，可以覆盖子调用中的部分变量
            .variables("{'key1':'config_value1','key2':'config_value2'}")
            .base_url("https://postman-echo.com")
            .verify(false)
            .export("['foo3']");

    private List<Step> teststeps = new ArrayList<Step>(){{
        add(new RunTestCase("嵌套的testcase")
                .withVariables("{'key1': 'testcase_ref_bar1', 'key2': 'testcase_ref_bar2'}")
				//case执行前可执行的钩子函数，函数执行异常后会终止用例执行
                .setupHook("${setup_hooks()}")
				//case执行前可执行的钩子函数，函数执行异常不会终止用例
                .setupHookNoThrowException("${NoExistFunc($foo1)}")
				//调用其他用例
                .call(SingleRequestStep2.class)
                .export("['key3']")
				//case执行后执行的钩子函数
                .teardownHook("teardown_hooks()")
				//case执行后执行的钩子函数，函数执行异常不会终止用例
                .teardownHookNoThrowException("${NoExistFunc()}")
        );
    }};
}
```

## 接口测试的核心要素
对于基础的接口测试框架，最最核心的要素可以概括为
> * 发起接口请求（Request）
> * 解析接口响应（Response）
> * 检查接口测试结果

项目一期首先支持http请求，最终选择了okHttp作为请求的客户端使用。

## 测试用例执行引擎
case样例上文已经展示，拆解case中的各个参数很简单，但是如何实现变量的传递和自定义方法的执行呢？尤其还有不同层级的变量重复情况下，变量优先级如何约束？

### 1.接口内容存储
首先设计多种成员类，比如header部分设计Header类,变量参数部分设计Variables类，这样一个用例步骤类，就可以简化为如下内容
```
class TStep{
    private Header header;
    private Param param;
    private Header header;
    private Variables var;
    
    @override
    public ParseableIntf to_value() {
        /* needParseMember包含了所有需解析的成员变量
           to_value是所有ParseableIntf接口实现类都要实现的一个方法
		   为何要对每个成员变量都要执行to_value方法？以param变量为例，其原始内容可能是
		   {'foo1': '$foo1', 'sum_v': '${sum_two()}','num-param': 12345}
		   此时就要对变量（$foo1）和自定义函数（${sum_two()}）进行解析
		*/
        this.needParseMember.forEach(e ->
            Optional.ofNullable(e)).to_value()
        );
        return this;
    }
}
```
### 2.复杂的to_value方法实现
上一步方案把所有的内容解析，都放在了to_value中，那么to_value如何实现呢？

首先可以确认，to_value的实现离不开两个重要元素
> * 上下文中的变量
> * 自定义方法的实现类

```
//variables_mapping即为上下文中的变量
//function为自定义方法的实现类
public ParseableIntfCls to_value(Variables variables_mapping, Class function) {}
```

### 3.所有用例中传入的参数都是string，如何解析？
上文的case样例中，所有传参都是string格式，比如

```
with_variables("{'foo1': 'bar11', 'foo2': 'bar21', 'sum_v': '${sum_two(1,2)}', 'foo4': '$foo5',}")
```

首先依照原版约定，自定义变量的表现形式为$variables,自定义函数的表现形式为${function(var1,var2)}
对于 header param 一类的内容，内部可以用一个简单map存储内容
```
private HashMap<String, LazyContent> content = new HashMap<>();
```
其中key为变量名，value为待解析的值，以上面with_variables参数为例

待解析的值包括两个，${sum_two(1,2)} 和 $foo5

设计两个类：LazyContent 和 LazyString extends LazyContent，后者是前者的子类

LazyContent用来存放不需要解析的非字符串类（如基本数据类型int long等，或是list map等复杂结构），LazyString用来存放需要解析的字符串类，如果LazyString中可以匹配到自定义变量和方法的正则，则进行处理，否则跳过。

为何取名LazyString？因为变量会在实际使用到时才会进行解析，是一种懒加载形式。

### 4.如何实现参数化用例

在自动化测试中，经常会遇到如下场景：
> * 测试搜索功能，只有一个搜索输入框，但有10种不同类型的搜索关键字；
> * 测试账号登录功能，需要输入用户名和密码，按照等价类划分后有20种组合情况。

可以使用testng的参数化方法，构造重复的用户数据

详情可以参考项目test目录下的ParametersTest1和ParametersTest2类，使用方式也非常简单，只需要在类上增加注解@Parameter即可，注解的内容是一个map型字符串，或是一个自定义方法，其返回值与如下字符串的map类似即可
```
@Parameters(mapStr = "{" +
        "                'param1':'5SFXXXXXXXX'," +
        "                'param2':12345," +
        "                'param3':['foo1','foo2']," +
        "                'param4':['soo1','soo2']" +
        "            }")
public class ParametersTest1 extends HttpRunner {
	//用例具体步骤忽略
}
```
解析过程会自动进行笛卡尔积计算，最终实现四种参数的用例
```
[
	{"param1":"5SFXXXXXXXX","param2":12345,"param3":"foo1","param4":"soo1"}
	{"param1":"5SFXXXXXXXX","param2":12345,"param3":"foo1","param4":"soo2"}
	{"param1":"5SFXXXXXXXX","param2":12345,"param3":"foo2","param4":"soo1"}
	{"param1":"5SFXXXXXXXX","param2":12345,"param3":"foo2","param4":"soo2"}
]
```

### 5.实现 hook 机制
在自动化测试中，通常在测试开始前需要做一些预处理操作，以及在测试结束后做一些清理性的工作。

例如，测试使用手机号注册账号的接口：

> * 测试开始前需要确保该手机号未进行过注册，常用的做法是先在数据库中删除该手机号相关的账号数据（若存在）；
> * 测试结束后，为了减少对测试环境的影响，常用的做法是在数据库中将本次测试产生的相关数据删除掉。

可以使用testng自带的@beforclass @beforetest等注解，也可以使用上面样例中的

setup_hook 和 teardown_hook方法，同样支持自定义方法