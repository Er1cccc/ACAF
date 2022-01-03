package com.er1cccc.acaf.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SinkRegistry {
    private List<Sink> sinkList=new ArrayList<>();

    public SinkRegistry addSink(Sink sink) {
        this.sinkList.add(sink);
        return this;
    }

}
