package com.httprunnerjava;

import com.alibaba.fastjson.JSONException;
import com.httprunnerjava.exception.HrunBizException;
import com.httprunnerjava.exception.HrunExceptionFactory;
import com.httprunnerjava.model.Enum.MethodEnum;
import com.httprunnerjava.model.component.atomsComponent.request.Headers;
import com.httprunnerjava.model.component.atomsComponent.request.Params;
import com.httprunnerjava.model.component.intf.reqOrResp;
import com.httprunnerjava.model.component.moleculesComponent.TRequest;
import com.httprunnerjava.model.lazyLoading.LazyContent;
import com.httprunnerjava.model.lazyLoading.LazyString;
import com.httprunnerjava.model.runningData.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class HttpSession {

    private OkHttpClient okHttpClient;

    private SessionData data;

    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

    public CookieJar getCookieJar(){
        return new CookieJar(){
            @Override
            public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
                cookieStore.put(httpUrl.host(), list);
            }

            @Override
            public @NotNull List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
                List<Cookie> cookies = cookieStore.get(httpUrl.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };
    }

    //TODO:下面的拦截器方法可以获取ip
    private Interceptor getInterceptor(){
        return chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);

            log.info(chain.connection().socket().getRemoteSocketAddress().toString());
            return response ;
        };
    }

    private X509TrustManager getTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    public HttpSession(HttpRunner httpRunner) {
        try{
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { getTrustManager() }, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();

            if(httpRunner.getConfig().getIsProxy().equals(true))
                okHttpClientBuilder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888)));

            Integer timeOut = httpRunner.getConfig().getTimeOut();
            okHttpClient = okHttpClientBuilder
                    //设置读取超时时间
                    .readTimeout(timeOut, TimeUnit.SECONDS)
                    //设置写的超时时间
                    .writeTimeout(timeOut, TimeUnit.SECONDS)
                    .connectTimeout(timeOut, TimeUnit.SECONDS)
                    .sslSocketFactory(sslSocketFactory, getTrustManager())
                    .addNetworkInterceptor(getInterceptor())
                    .cookieJar(getCookieJar())
                    .build();
        }catch (Exception e){
            log.error("创建HttpSession对象失败，失败原因是 %s",e.getMessage());
            log.debug(e.getStackTrace().toString());
            HrunExceptionFactory.create("E20005");
        }

    }

    public Response request(MethodEnum method, String url, TRequest request) {
        this.data = new SessionData();

        long startTimestamp = System.currentTimeMillis();
        Response response = sendRequestSafeMode(method, url, request);
        long responseTimeMs = System.currentTimeMillis() - startTimestamp;

        //TODO：低优先级 获取ip地址
        // try:z
        //        client_ip, client_port = response.raw.connection.sock.getsockname()

        long contentSize = (response.body() != null &&response.body().contentLength() == -1) ?
                0 : response.body().contentLength();

        // record the consumed time
        this.getData().getStat().setResponseTimeMs(responseTimeMs);
        this.getData().getStat().setContentSize(contentSize);

        // record request and response histories, include 30X redirection
        //TODO: response.history记录的是请求重定向的内容，暂时不支持重定向
        // response_list = response.history + [response]

        //TODO: 低优先级 这里需要对接口请求的status_code做一些判断处理，异常请求需要抛异常
        // try:
        //     response.raise_for_status()

        this.getData().setReqResps(Collections.singletonList(getReqRespRecord(response)));

        log.info(String.format("status_code: %s, response_time(ms): %s ms, response_length: %s bytes",
                response.code(),
                responseTimeMs,
                contentSize));

        return response;
    }

    public Response sendRequestSafeMode(MethodEnum method, String url, TRequest tRequest) {
        Response response = null;
        switch(method){
            case GET:
                try {
                    Request.Builder requestBuilder = new Request.Builder();
                    addHeaders(tRequest.getHeaders(),requestBuilder);

                    Request request = requestBuilder.url(parseUrl(url,tRequest.getParams())).get().build();
                    response = okHttpClient.newCall(request).execute();
                }catch (IOException e){
                    log.error("请求接口报错，请根据日志检查请求是否准确，报错原始信息如下");
                    log.error(HrunBizException.toStackTrace(e));
                    HrunExceptionFactory.create("E20003");
                }
                break;
            case POST:
                try {
                    Request.Builder requestBuilder = new Request.Builder();
                    addHeaders(tRequest.getHeaders(),requestBuilder);
                    RequestBody requestBody = null;
                    String contentType = Optional.ofNullable(tRequest.getHeaders().getContent().get("Content-Type")).
                            orElse(new LazyString("")).getEvalValue().toString();
                    if(contentType.contains("application/json")){
                        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                        if(tRequest.getReqJson() != null){
                            requestBody = RequestBody.create(tRequest.getReqJson().getEvalString(),mediaType);
                        }else if (tRequest.getData() != null){
                            requestBody = RequestBody.create(tRequest.getData().getEvalString(),mediaType);
                        } else {
                            requestBody = RequestBody.create("",mediaType);
                        }
                    }else if(contentType.contains("application/form-data")){

                    }else if(contentType.contains("application/x-www-form-urlencoded")){
                        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
                        requestBody = RequestBody.create(tRequest.getData().getEvalString(),mediaType);
                    }else{
                        MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
                        requestBody = RequestBody.create(tRequest.getData().getEvalString(),mediaType);
                    }

                    Request request = requestBuilder.url(parseUrl(url,tRequest.getParams())).post(requestBody).build();
                    response = okHttpClient.newCall(request).execute();
                }catch (Exception e){
                    log.error("请求接口报错，请根据日志检查请求是否准确，报错原始信息如下");
                    log.error(HrunBizException.toStackTrace(e));
                    HrunExceptionFactory.create("E20003");
                }
                break;
            case PUT:
                try {
                    Request.Builder requestBuilder = new Request.Builder();
                    addHeaders(tRequest.getHeaders(),requestBuilder);
                    RequestBody requestBody = null;
                    String contentType = Optional.ofNullable(tRequest.getHeaders().getContent().get("Content-Type")).
                            orElse(new LazyString("")).getEvalValue().toString();
                    if(contentType.contains("application/json")){
                        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                        if(tRequest.getReqJson() != null){
                            requestBody = RequestBody.create(tRequest.getReqJson().getEvalString(),mediaType);
                        }else{
                            requestBody = RequestBody.create("",mediaType);
                        }
                    }else if(contentType.contains("application/form-data")){

                    }else if(contentType.contains("application/x-www-form-urlencoded")){
                        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
                        requestBody = RequestBody.create(tRequest.getData().getEvalString(),mediaType);
                    }else{
                        MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
                        requestBody = RequestBody.create(tRequest.getData().getEvalString(),mediaType);
                    }

                    Request request = requestBuilder.url(parseUrl(url,tRequest.getParams())).put(requestBody).build();
                    response = okHttpClient.newCall(request).execute();
                }catch (Exception e){
                    log.error("请求接口报错，请根据日志检查请求是否准确，报错原始信息如下");
                    log.error(HrunBizException.toStackTrace(e));
                    HrunExceptionFactory.create("E20003");
                }
                break;
            case DELETE:
                try {
                    Request.Builder requestBuilder = new Request.Builder();
                    addHeaders(tRequest.getHeaders(),requestBuilder);
                    RequestBody requestBody = null;
                    String contentType = Optional.ofNullable(tRequest.getHeaders().getContent().get("Content-Type")).
                            orElse(new LazyString("")).getEvalValue().toString();
                    if(contentType.contains("application/json")){
                        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                        if(tRequest.getReqJson() != null){
                            requestBody = RequestBody.create(tRequest.getReqJson().getEvalString(),mediaType);
                        }else{
                            requestBody = RequestBody.create("",mediaType);
                        }
                    }else if(contentType.contains("application/form-data")){

                    }else if(contentType.contains("application/x-www-form-urlencoded")){
                        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
                        requestBody = RequestBody.create(tRequest.getData().getEvalString(),mediaType);
                    }else{
                        MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
                        requestBody = RequestBody.create(tRequest.getData().getEvalString(),mediaType);
                    }

                    Request request = requestBuilder.url(parseUrl(url,tRequest.getParams())).delete(requestBody).build();
                    response = okHttpClient.newCall(request).execute();
                }catch (Exception e){
                    log.error("请求接口报错，请根据日志检查请求是否准确，报错原始信息如下");
                    log.error(HrunBizException.toStackTrace(e));
                    HrunExceptionFactory.create("E20003");
                }
                break;
        }

        return response;
    }

    public void addHeaders(Headers headers, Request.Builder requestBuilder){
        if(headers == null || headers.isEmpty()) {
            return;
        }

        for(Map.Entry<String, LazyContent> entry : headers.getContent().entrySet()){
            requestBuilder.addHeader(entry.getKey(),String.valueOf(entry.getValue().getEvalValue()));
        }
    }

    public HttpUrl parseUrl(String url, Params params){
        if(params == null || params.getContent().size() == 0) {
            return HttpUrl.parse(url);
        }

        StringBuilder urlBuilder = new StringBuilder(url);
        if(params.getContent().size() != 0) {
            urlBuilder.append("?");
        }

        params.getContent().entrySet().forEach( each ->
                urlBuilder.append(each.getKey())
                          .append("=")
                          .append(each.getValue().getEvalValue())
                          .append("&")
        );

        url = urlBuilder.toString();
        if(url.endsWith("&")) {
            url = url.substring(0, url.length() - 1);
        }

        return HttpUrl.parse(url);
    }

    public ReqRespData getReqRespRecord(Response respObj){
        Headers requestHeaders = new Headers(respObj.request().headers());
        String requestCookies = Optional.ofNullable(respObj.request().header("Cookie")).orElse("");

        String requestBodyStr = "";
        RequestBody requestBody = respObj.request().body();
        try{
            String str = "";
            if(requestBody != null && requestBody.contentLength() != 0L){
                final Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                str = buffer.readUtf8();
            }

            requestBodyStr =
                    (requestBody != null && requestBody.contentLength() != 0L) ? str : "";

            LazyContent<?> requestContentType = requestHeaders.getContent().get("content-type");
            //TODO:低优先级
            // if requestContentType and "multipart/form-data" in requestContentType:
            //     requestBody = "upload file stream (OMITTED)"
        }catch (IOException e){
            HrunExceptionFactory.create("E20006");
        }

        RequestData requestData = new RequestData(respObj.request().method(),
                respObj.request().url().toString(),
                requestHeaders,
                requestCookies,
                requestBodyStr
        );
        logPrint(requestData,"request");

        Headers respHeaders = new Headers(respObj.headers());
        LazyContent<?> contentType = respHeaders.getContent().get("content-type");
        //TODO:
        // if "image" in contentType:
        String respText = "";
        try{
            respText = respObj.body().string();
            ResponseObject.setCurrentRespBody(respText);
        }catch (IOException e){
            // TODO:异常的处理
            HrunExceptionFactory.create("E20007");
        }catch(JSONException e){

        }

        ResponseData responseData = new ResponseData(respObj.code(),
                respHeaders,
                "",
                respObj.body().contentType() == null ? "NULL" : respObj.body().contentType().toString(),
                respObj.header("content-type"),
                respText
        );
        logPrint(responseData,"response");

        return new ReqRespData(requestData, responseData);
    }

    public void logPrint(reqOrResp reqOrResp, String rType){
        String msg = String.format("\n================== %s details ==================\n",rType);

        msg += reqOrResp.toString();
        log.debug(msg);
    }

}
