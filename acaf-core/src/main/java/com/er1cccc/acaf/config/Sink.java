package com.er1cccc.acaf.config;

public interface Sink {
    Object sinkMethod() throws Exception;

    default void addPassthrough(PassthroughRegistry passthroughRegistry) {}
}
