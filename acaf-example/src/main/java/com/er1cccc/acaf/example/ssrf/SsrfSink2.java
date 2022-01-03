package com.er1cccc.acaf.example.ssrf;

import com.er1cccc.acaf.config.ControllableParam;
import com.er1cccc.acaf.config.PassthroughRegistry;
import com.er1cccc.acaf.config.Sink;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.HttpURLConnection;
import java.net.URL;

public class SsrfSink2 implements Sink {

    private ControllableParam params = new ControllableParam();

    public SsrfSink2(){
        params.put("url","http://localhost");
    }

    @Override
    public Object sinkMethod() throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet getRequest = new HttpGet((String) params.getParameter("url"));
        httpClient.execute(getRequest);
        return null;
    }

    public static void main(String[] args) throws Exception{
//        new SsrfSink2().sinkMethod();
    }

}
