package com.er1cccc.acaf.core.audit.discovery;

import com.er1cccc.acaf.core.audit.auditcore.*;
import com.er1cccc.acaf.core.entity.ClassFile;
import com.er1cccc.acaf.core.entity.ClassReference;
import com.er1cccc.acaf.core.entity.MethodReference;
import com.er1cccc.acaf.data.DataFactory;
import com.er1cccc.acaf.data.DataLoader;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PassthroughDiscovery {
    private static final Logger logger = Logger.getLogger(PassthroughDiscovery.class);
    private final Map<MethodReference.Handle, Set<MethodReference.Handle>> methodCalls = new HashMap<>();
    private Map<MethodReference.Handle, Set<Integer>> passthroughDataflow;

    public void discover(List<ClassFile> classFileList,Path saveDataDirPath) throws IOException{
        logger.info("get data flow");

        Map<MethodReference.Handle, MethodReference> methodMap = DataLoader.loadMethods(saveDataDirPath);
        Map<ClassReference.Handle, ClassReference> classMap = DataLoader.loadClasses(saveDataDirPath);
        InheritanceMap inheritanceMap = InheritanceMap.load(saveDataDirPath);

        Map<String, ClassFile> classFileByName = discoverMethodCalls(classFileList);

        List<MethodReference.Handle> sortedMethods = topologicallySortMethodCalls();
        passthroughDataflow = calculatePassthroughDataflow(classFileByName, classMap, inheritanceMap, sortedMethods);
    }

    private Map<MethodReference.Handle, Set<Integer>> calculatePassthroughDataflow(Map<String, ClassFile> classFileByName, Map<ClassReference.Handle, ClassReference> classMap, InheritanceMap inheritanceMap, List<MethodReference.Handle> sortedMethods) {
        final Map<MethodReference.Handle, Set<Integer>> passthroughDataflow = new HashMap<>();

        for (MethodReference.Handle method : sortedMethods) {
            if (method.getName().equals("<clinit>")) {
                continue;
            }
            ClassFile file = classFileByName.get(method.getClassReference().getName());
            try {
                InputStream ins = file.getInputStream();
                ClassReader cr = new ClassReader(ins);
                ins.close();

                DataFlowClassVisitor cv = new DataFlowClassVisitor(classMap, inheritanceMap, passthroughDataflow, method);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                passthroughDataflow.put(method, cv.getReturnTaint());
            } catch (IOException e) {
                logger.error("Exception analyzing " + method.getClassReference().getName(), e);
            }
        }
        return passthroughDataflow;
    }

    private List<MethodReference.Handle> topologicallySortMethodCalls() {
        logger.info("topological sort methods");
        Map<MethodReference.Handle, Set<MethodReference.Handle>> outgoingReferences = new HashMap<>();
        for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> entry : methodCalls.entrySet()) {
            MethodReference.Handle method = entry.getKey();
            outgoingReferences.put(method, new HashSet<>(entry.getValue()));
        }
        Set<MethodReference.Handle> dfsStack = new HashSet<>();
        Set<MethodReference.Handle> visitedNodes = new HashSet<>();
        List<MethodReference.Handle> sortedMethods = new ArrayList<>(outgoingReferences.size());
        for (MethodReference.Handle root : outgoingReferences.keySet()) {
            dfsSort(outgoingReferences, sortedMethods, visitedNodes, dfsStack, root);
        }
        return sortedMethods;
    }

    public void dfsSort(Map<MethodReference.Handle, Set<MethodReference.Handle>> outgoingReferences,
                               List<MethodReference.Handle> sortedMethods, Set<MethodReference.Handle> visitedNodes,
                               Set<MethodReference.Handle> stack, MethodReference.Handle node) {
        if (stack.contains(node)) {
            return;
        }
        if (visitedNodes.contains(node)) {
            return;
        }
        Set<MethodReference.Handle> outgoingRefs = outgoingReferences.get(node);
        if (outgoingRefs == null) {
            return;
        }
        stack.add(node);
        for (MethodReference.Handle child : outgoingRefs) {
            dfsSort(outgoingReferences, sortedMethods, visitedNodes, stack, child);
        }
        stack.remove(node);
        visitedNodes.add(node);
        sortedMethods.add(node);
    }

    private Map<String, ClassFile> discoverMethodCalls(final List<ClassFile> classFileList) throws IOException {
        Map<String, ClassFile> classFileByName = new HashMap<>();
        for (ClassFile file : classFileList) {
            try {
                MethodCallClassVisitor mcv = new MethodCallClassVisitor(methodCalls);
                InputStream ins = file.getInputStream();
                ClassReader cr = new ClassReader(ins);
                ins.close();
                cr.accept(mcv, ClassReader.EXPAND_FRAMES);
                classFileByName.put(mcv.getName(), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return classFileByName;
    }

    public void save(Path dirPath) throws IOException {
        if (passthroughDataflow == null) {
            throw new IllegalStateException("Save called before discover()");
        }
        DataLoader.saveData(new File(dirPath.toFile(),"passthrough.dat").toPath(), new PassThroughFactory(), passthroughDataflow.entrySet());
    }

    public static class PassThroughFactory implements DataFactory<Map.Entry<MethodReference.Handle, Set<Integer>>> {

        @Override
        public Map.Entry<MethodReference.Handle, Set<Integer>> parse(String[] fields) {
            ClassReference.Handle clazz = new ClassReference.Handle(fields[0]);
            MethodReference.Handle method = new MethodReference.Handle(clazz, fields[1], fields[2]);

            Set<Integer> passthroughArgs = new HashSet<>();
            for (String arg : fields[3].split(",")) {
                if (arg.length() > 0) {
                    passthroughArgs.add(Integer.parseInt(arg));
                }
            }
            return new AbstractMap.SimpleEntry<>(method, passthroughArgs);
        }

        @Override
        public String[] serialize(Map.Entry<MethodReference.Handle, Set<Integer>> entry) {
            if (entry.getValue()==null||entry.getValue().size() == 0) {
                return null;
            }

            final String[] fields = new String[4];
            fields[0] = entry.getKey().getClassReference().getName();
            fields[1] = entry.getKey().getName();
            fields[2] = entry.getKey().getDesc();

            StringBuilder sb = new StringBuilder();
            for (Integer arg : entry.getValue()) {
                sb.append(Integer.toString(arg));
                sb.append(",");
            }
            fields[3] = sb.toString();

            return fields;
        }
    }

    public static Map<MethodReference.Handle, Set<Integer>> load(Path dirPath) throws IOException {
        Map<MethodReference.Handle, Set<Integer>> passthroughDataflow = new HashMap<>();
        for (Map.Entry<MethodReference.Handle, Set<Integer>> entry : DataLoader.loadData(new File(dirPath.toFile(),"passthrough.dat").toPath(), new PassThroughFactory())) {
            passthroughDataflow.put(entry.getKey(), entry.getValue());
        }
        return passthroughDataflow;
    }

}
