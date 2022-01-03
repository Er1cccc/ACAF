package com.er1cccc.acaf.core.audit.discovery;

import com.er1cccc.acaf.config.*;
import com.er1cccc.acaf.core.audit.auditcore.CallGraph;
import com.er1cccc.acaf.core.audit.auditcore.InheritanceMap;
import com.er1cccc.acaf.core.audit.auditcore.InheritanceUtil;
import com.er1cccc.acaf.core.audit.auditcore.VulnClassVisitor;
import com.er1cccc.acaf.core.entity.*;
import com.er1cccc.acaf.core.resolver.Resolver;
import com.er1cccc.acaf.core.resolver.impl.CompositResolver;
import com.er1cccc.acaf.core.template.VulnTemplateSinkVisitor;
import com.er1cccc.acaf.data.DataLoader;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class VulnDiscovery {
    private static final Logger logger = Logger.getLogger(VulnDiscovery.class);

    private Map<String, ClassFile> classFileMap;
    private InheritanceMap localInheritanceMap;
    private Map<MethodReference.Handle, Set<Integer>> localDataFlow;
    private Map<MethodReference.Handle, MethodReference> methodMap;
    private Map<ClassReference.Handle, ClassReference> classMap ;
    private List<CallGraph> discoveredCalls ;
    private SourceRegistry sourceRegistry;
    private SanitizeRegistry sanitizeRegistry;
    private SinkRegistry sinkRegistry;
    private Map<MethodReference.Handle, Set<CallGraph>> callGraphMap;
    private Path templateSaveDataDirPath;
    private Map<Sink,CompositResolver> resolverMap=new HashMap<>();

    public VulnDiscovery(List<ClassFile> targetClassFileList,
                         SourceRegistry sourceRegistry,
                         SanitizeRegistry sanitizeRegistry,
                         SinkRegistry sinkRegistry,
                         Path targetSaveDataDirPath,
                         Path templateSaveDataDirPath) throws IOException{
        this.sourceRegistry=sourceRegistry;
        this.sanitizeRegistry=sanitizeRegistry;
        this.sinkRegistry=sinkRegistry;
        this.templateSaveDataDirPath=templateSaveDataDirPath;
        localInheritanceMap = InheritanceMap.load(targetSaveDataDirPath);
        localDataFlow = PassthroughDiscovery.load(targetSaveDataDirPath);
        methodMap= DataLoader.loadMethods(targetSaveDataDirPath);
        classMap= DataLoader.loadClasses(targetSaveDataDirPath);
        discoveredCalls= CallGraphDiscovery.load(targetSaveDataDirPath);
        classFileMap=new HashMap<>();
        for (ClassFile classFile : targetClassFileList) {
            classFileMap.put(classFile.getClassName(),classFile);
        }
    }

    private CompositResolver getResolver(Sink sink) throws IOException {
        CompositResolver compositResolver = resolverMap.get(sink);
        if(compositResolver==null){
            ClassReader classReader = new ClassReader(sink.getClass().getName());
            compositResolver = new CompositResolver(sink);
            classReader.accept(new VulnTemplateSinkVisitor(compositResolver,templateSaveDataDirPath, sink),ClassReader.EXPAND_FRAMES);
            PassthroughRegistry passthroughRegistry = new PassthroughRegistry(this.localDataFlow);
            //TODO 这样的话上一个sink添加的Passthrough会传递给下一个sink，有没有影响？
            sink.addPassthrough(passthroughRegistry);
            resolverMap.put(sink,compositResolver);
        }else{
            //重复利用之前要先重置resolver的ind
            compositResolver.reset();
        }
        return compositResolver;
    }


    public void discover(List<SpringController> controllers) throws IOException {
        logger.info("finding vuln...");
        callGraphMap = prepareCallGraphMap(methodMap);
        doDiscover(controllers);
    }

    /**
     * 准备方法调用图。把抽象方法全部具体化
     * @param methodMap
     * @return
     * @throws IOException
     */
    private Map<MethodReference.Handle, Set<CallGraph>> prepareCallGraphMap(Map<MethodReference.Handle, MethodReference> methodMap){
        Map<MethodReference.Handle, Set<CallGraph>> graphCallMap=new HashMap<>();

        Map<MethodReference.Handle, Set<MethodReference.Handle>> methodImplMap =
                InheritanceUtil.getAllMethodImplementations(localInheritanceMap, methodMap);
        List<CallGraph> tempList = new ArrayList<>(discoveredCalls);
        for (int i = 0; i < discoveredCalls.size(); i++) {
            CallGraph edge = tempList.get(i);
            ClassReference.Handle handle = edge.getTargetMethod().getClassReference();
            ClassReference classReference = classMap.get(handle);
            if (classReference != null && classReference.isInterface()) {
                Set<MethodReference.Handle> implSet = methodImplMap.get(edge.getTargetMethod());
                if (implSet == null || implSet.size() == 0) {
                    continue;
                }
                for (MethodReference.Handle methodHandle : implSet) {
                    String callerDesc = methodMap.get(methodHandle).getDesc();
                    if (edge.getTargetMethod().getDesc().equals(callerDesc)) {
                        tempList.add(new CallGraph(
                                edge.getTargetMethod(),
                                methodHandle,
                                edge.getTargetArgIndex(),
                                null,
                                edge.getTargetArgIndex()
                        ));
                    }
                }
            }
        }

        discoveredCalls.clear();
        discoveredCalls.addAll(tempList);
        for (CallGraph graphCall : discoveredCalls) {
            MethodReference.Handle caller = graphCall.getCallerMethod();
            if (!graphCallMap.containsKey(caller)) {
                Set<CallGraph> graphCalls = new HashSet<>();
                graphCalls.add(graphCall);
                graphCallMap.put(caller, graphCalls);
            } else {
                graphCallMap.get(caller).add(graphCall);
            }
        }
        return graphCallMap;
    }

    private void doDiscover(List<SpringController> controllers) throws IOException {
        for (SpringController controller : controllers) {
            for (SpringMapping mapping : controller.getMappings()) {
                MethodReference methodReference = mapping.getMethodReference();
                if (methodReference == null) {
                    continue;
                }
                Type[] argTypes = Type.getArgumentTypes(methodReference.getDesc());
                Type[] extendedArgTypes = new Type[argTypes.length + 1];
                System.arraycopy(argTypes, 0, extendedArgTypes, 1, argTypes.length);
                argTypes = extendedArgTypes;
                boolean[] vulnerableIndex = new boolean[argTypes.length];
                for (int i = 1; i < argTypes.length; i++) {
                    if (!isPrimitive(argTypes[i])) {
                        vulnerableIndex[i] = true;
                    }
                }
                Set<CallGraph> calls = callGraphMap.get(methodReference.getHandle());
                if (calls == null || calls.size() == 0) {
                    continue;
                }
                // mapping method中的call
                for (CallGraph callGraph : calls) {
                    int callerIndex = callGraph.getCallerArgIndex();
                    if (callerIndex == -1) {
                        continue;
                    }
                    if (vulnerableIndex[callerIndex]) {
                        // 防止循环
                        for(Sink sink: sinkRegistry.getSinkList()){
                            CompositResolver resolver = getResolver(sink);

                            List<MethodReference.Handle> visited = new ArrayList<>();
                            LinkedList<MethodReference.Handle> stack = new LinkedList<>();
                            stack.push(callGraph.getCallerMethod());
                            doTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited, stack, resolver);
                        }
                    }
                }
            }
        }
    }

    private boolean isPrimitive(Type argType) {
        int sort = argType.getSort();
        return sort==Type.BYTE||
                sort==Type.INT||
                sort==Type.SHORT||
                sort==Type.LONG||
                sort==Type.FLOAT||
                sort==Type.DOUBLE||
                sort==Type.BOOLEAN||
                sort==Type.CHAR;
    }

    private void doTask(MethodReference.Handle targetMethod, int targetIndex,
                               List<MethodReference.Handle> visited,
                                Deque<MethodReference.Handle> stack,
                                CompositResolver vulnResolver) {
        if (visited.contains(targetMethod)) {
            return;
        } else {
            visited.add(targetMethod);
        }
        stack.push(targetMethod);
        ClassFile file = classFileMap.get(targetMethod.getClassReference().getName());
        try {
            InputStream ins = file.getInputStream();
            ClassReader cr = new ClassReader(ins);
            ins.close();
            VulnClassVisitor cv = new VulnClassVisitor(
                    targetMethod, targetIndex, localInheritanceMap, localDataFlow, vulnResolver);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            if(vulnResolver.resolve(null)){
                logger.info("detect vuln: " + vulnResolver.getSink().getClass().getName());
                printStackTrace(stack);
                return;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Set<CallGraph> calls = callGraphMap.get(targetMethod);
        if (calls == null || calls.size() == 0) {
            return;
        }
        for (CallGraph callGraph : calls) {
            if (callGraph.getCallerArgIndex() == targetIndex && targetIndex != -1) {
                if (visited.contains(callGraph.getTargetMethod())) {
                    return;
                }
                doTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(),visited, stack, vulnResolver);
                stack.pop();
            }
        }
    }

    private void printStackTrace(Deque<MethodReference.Handle> stack) {
        Deque<MethodReference.Handle> copyStack = new LinkedList<>(stack);
        StringBuilder prefix=new StringBuilder("\t");
        for (MethodReference.Handle handle : stack) {
            logger.info(prefix+handle.getClassReference().getName()+"."+handle.getName());
            prefix.append("\t");
        }
    }

    public static void main(String[] args) throws Exception{

    }
}
