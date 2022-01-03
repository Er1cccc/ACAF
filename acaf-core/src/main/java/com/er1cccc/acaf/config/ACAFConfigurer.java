package com.er1cccc.acaf.config;

public interface ACAFConfigurer {
    void addSource(SourceRegistry sourceRegistry);
    void addSanitize(SanitizeRegistry sanitizeRegistry);
    void addSink(SinkRegistry sinkRegistry);
}
