package com.er1cccc.acaf.example.ssrf;


import com.er1cccc.acaf.config.*;

public class SSRFConfigurer implements ACAFConfigurer {
    @Override
    public void addSource(SourceRegistry sourceRegistry) {

    }

    @Override
    public void addSanitize(SanitizeRegistry sanitizeRegistry) {
    }

    @Override
    public void addSink(SinkRegistry sinkRegistry) {
        sinkRegistry.addSink(new SsrfSink1());
        sinkRegistry.addSink(new SsrfSink2());
        sinkRegistry.addSink(new SsrfSink3());
        sinkRegistry.addSink(new SsrfSink4());
    }
}
