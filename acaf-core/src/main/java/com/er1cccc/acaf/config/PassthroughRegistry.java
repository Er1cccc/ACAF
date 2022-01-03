package com.er1cccc.acaf.config;

import com.er1cccc.acaf.core.audit.discovery.PassthroughDiscovery;
import com.er1cccc.acaf.core.entity.ClassReference;
import com.er1cccc.acaf.core.entity.MethodReference;
import org.objectweb.asm.*;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PassthroughRegistry {
    private Map<MethodReference.Handle, Set<Integer>> passthroughDataflow;

    public PassthroughRegistry(Map<MethodReference.Handle, Set<Integer>> passthroughDataflow) {
        this.passthroughDataflow=passthroughDataflow;
    }


    public void addPassthrough(Method method,Integer ...args) throws IOException {
        ClassReference.Handle classHandle = new ClassReference.Handle(method.getDeclaringClass().getName().replace(".","/"));
        MethodReference.Handle methodHandle = new MethodReference.Handle(classHandle, method.getName(), Type.getMethodDescriptor(method));
        HashSet<Integer> argSet = new HashSet<>();
        for(int arg:args){
            argSet.add(arg);
        }
        this.passthroughDataflow.put(methodHandle,argSet);
    }

    public void addPassthrough(Constructor constructor, Integer ...args) throws IOException {
        ClassReference.Handle classHandle = new ClassReference.Handle(constructor.getDeclaringClass().getName().replace(".","/"));
        MethodReference.Handle methodHandle = new MethodReference.Handle(classHandle, "<init>", Type.getConstructorDescriptor(constructor));
        HashSet<Integer> argSet = new HashSet<>();
        for(int arg:args){
            argSet.add(arg);
        }
        this.passthroughDataflow.put(methodHandle,argSet);
    }


    public static void main(String[] args) throws Exception{
//        PassthroughRegistry passthroughRegistry = new PassthroughRegistry(null);
//        passthroughRegistry.addPassthrough(PassthroughRegistry.class.getMethod("main",String[].class),null);
    }

}
