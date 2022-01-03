package com.er1cccc.acaf.example.ssrf;

import com.er1cccc.acaf.config.ControllableParam;
import com.er1cccc.acaf.config.PassthroughRegistry;
import com.er1cccc.acaf.config.Sink;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.lang.reflect.Method;

public class SsrfSink4 implements Sink {
    private ControllableParam params = new ControllableParam();

    public SsrfSink4(){
        params.put("url","http://localhost");
    }

    @Override
    public Object sinkMethod() throws Exception {
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder().url((String) params.getParameter("url")).build();
        Response response = httpClient.newCall(request).execute();
        return null;
    }

    @Override
    public void addPassthrough(PassthroughRegistry passthroughRegistry) {
        try{
            Class<?> builder = new Request.Builder().getClass();
            Method urlMethod = builder.getMethod("url",String.class);
            Method buildMethod = builder.getMethod("build");
            Class<OkHttpClient> okHttpClientClass = OkHttpClient.class;
            Method newCall = okHttpClientClass.getMethod("newCall", Request.class);
            Class<?> call = newCall.getReturnType();
            Method execute = call.getMethod("execute");

            passthroughRegistry.addPassthrough(urlMethod,1);
            passthroughRegistry.addPassthrough(buildMethod,0);
            passthroughRegistry.addPassthrough(newCall,1);
            passthroughRegistry.addPassthrough(execute,0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
//        new SsrfSink4().sinkMethod();
        Class<?> builder = new Request.Builder().getClass();
        Method urlMethod = builder.getMethod("url",String.class);
        Method buildMethod = builder.getMethod("build");
        Class<OkHttpClient> okHttpClientClass = OkHttpClient.class;
        Method newCall = okHttpClientClass.getMethod("newCall", Request.class);
        Class<?> call = newCall.getReturnType();
        Method execute = call.getMethod("execute");

    }

}
