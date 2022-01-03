package com.er1cccc.acaf.data;

public interface DataFactory<T> {
    T parse(String[] fields);
    String[] serialize(T obj);
}
