package com.er1cccc.acaf.core.audit.discovery;

import com.er1cccc.acaf.core.audit.auditcore.CallGraph;
import com.er1cccc.acaf.core.entity.ClassReference;
import com.er1cccc.acaf.core.entity.MethodReference;
import com.er1cccc.acaf.util.DrawUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DrawService {
    public static void start(Set<CallGraph> discoveredCalls, String finalPackageName,
                             Map<ClassReference.Handle, ClassReference> classMap,
                             Map<MethodReference.Handle, Set<MethodReference.Handle>> methodImplMap) {
        Set<CallGraph> targetCallGraphs = new HashSet<>();
        for (CallGraph callGraph : discoveredCalls) {
            ClassReference callerClass = classMap.get(callGraph.getCallerMethod().getClassReference());
            ClassReference targetClass = classMap.get(callGraph.getTargetMethod().getClassReference());
            if (targetClass == null) {
                continue;
            }
            if (targetClass.getName().equals("java/lang/Object") &&
                    callGraph.getTargetMethod().getName().equals("<init>") &&
                    callGraph.getTargetMethod().getDesc().equals("()V")) {
                continue;
            }
            if (callerClass.getName().startsWith(finalPackageName)) {
                targetCallGraphs.add(callGraph);
            }
        }
        DrawUtil.drawCallGraph(targetCallGraphs);
    }
}
