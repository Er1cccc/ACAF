package com.er1cccc.acaf.example.ssrf;

import com.er1cccc.acaf.config.ControllableParam;
import com.er1cccc.acaf.config.Sink;

import java.net.HttpURLConnection;
import java.net.URL;

public class SsrfSink1 implements Sink {
    private ControllableParam params = new ControllableParam();

    public SsrfSink1(){
        params.put("url","http://localhost");
    }

    @Override
    public Object sinkMethod() throws Exception {
        URL url = new URL((String) params.getParameter("url"));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.getInputStream();
        return null;
    }


}
