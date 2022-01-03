package com.er1cccc.acaf.util;


import com.er1cccc.acaf.core.audit.auditcore.CallGraph;
import com.er1cccc.acaf.core.entity.MethodReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

public class DataUtil {
    private static final String DataFlowFile = "dataflow.dat";
    private static final String CallGraphFile = "callgraph.dat";

    public static void SaveDataFlows(Map<MethodReference.Handle, Set<Integer>> dataFlow,
                                     Map<MethodReference.Handle, MethodReference> methodMap) {
        try {
            Path filePath = Paths.get(DataFlowFile);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            StringBuilder dataFlowStr = new StringBuilder();
            for (MethodReference.Handle handle : dataFlow.keySet()) {
                String className = methodMap.get(handle).getClassReference().getName();
                String methodName = handle.getName();
                Set<Integer> results = dataFlow.get(handle);
                if (results != null && results.size() != 0) {
                    dataFlowStr.append("results:");
                    for (Integer i : results) {
                        dataFlowStr.append(i).append(" ");
                    }
                } else {
                    continue;
                }
                dataFlowStr.append(className).append("\t");
                dataFlowStr.append(methodName).append("\t");
                dataFlowStr.append("\n");
            }
            FileUtil.writeFile(DataFlowFile, dataFlowStr.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SaveCallGraphs(Set<CallGraph> discoveredCalls) {
        try {
            Path filePath = Paths.get(CallGraphFile);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            StringBuilder callGraphStr = new StringBuilder();
            for (CallGraph callGraph : discoveredCalls) {
                String callerClass = callGraph.getCallerMethod().getClassReference().getName();
                String callerMethod = callGraph.getCallerMethod().getName();
                Integer callerArgIndex = callGraph.getCallerArgIndex();
                String targetClass = callGraph.getTargetMethod().getClassReference().getName();
                String targetMethod = callGraph.getTargetMethod().getName();
                Integer targetArgIndex = callGraph.getTargetArgIndex();
                callGraphStr.append("caller:").append(callerClass).append(".")
                        .append(callerMethod).append("(").append(callerArgIndex).append(")")
                        .append("\n").append("target:").append(targetClass).append(".")
                        .append(targetMethod).append("(").append(targetArgIndex).append(")").append("\n")
                        .append("--------------------------------------------------------------------\n");
            }
            FileUtil.writeFile(CallGraphFile, callGraphStr.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
