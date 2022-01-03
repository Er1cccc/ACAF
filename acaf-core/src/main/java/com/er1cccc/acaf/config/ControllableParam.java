package com.er1cccc.acaf.config;

import java.util.HashMap;
import java.util.Map;

public class ControllableParam {
    private Map<String,Object> params=new HashMap<>();

    public <V> V put(String key, V value){
         return (V) params.put(key, value);
    }

    public Object getParameter(String name){
        return params.get(name);
    }

}
