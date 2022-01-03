package com.er1cccc.acaf.core.audit.auditcore;

import com.er1cccc.acaf.core.entity.MethodReference;
import com.er1cccc.acaf.core.resolver.Resolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VulnClassVisitor extends ClassVisitor {
    private final InheritanceMap inheritanceMap;
    private final Map<MethodReference.Handle, Set<Integer>> dataFlow;

    private String name;
    private String signature;
    private String superName;
    private String[] interfaces;
    Resolver vulnResolver;

    private MethodReference.Handle methodHandle;
    private int methodArgIndex;
    private List<Boolean> pass;

    public VulnClassVisitor(MethodReference.Handle targetMethod,
                            int targetIndex, InheritanceMap inheritanceMap,
                            Map<MethodReference.Handle, Set<Integer>> dataFlow,
                            Resolver vulnResolver) {
        super(Opcodes.ASM6);
        this.inheritanceMap = inheritanceMap;
        this.dataFlow = dataFlow;
        this.methodHandle = targetMethod;
        this.methodArgIndex = targetIndex;
        this.pass = new ArrayList<>();
        this.vulnResolver=vulnResolver;
    }


    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.name = name;
        this.signature = signature;
        this.superName = superName;
        this.interfaces = interfaces;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (name.equals(this.methodHandle.getName())) {
            VulnMethodAdapter vulnMethodAdapter = new VulnMethodAdapter(
                    this.methodArgIndex, this.pass,
                    inheritanceMap, dataFlow, Opcodes.ASM6, mv,
                    this.name, access, name, descriptor, signature, exceptions,
                    vulnResolver
            );
            return new JSRInlinerAdapter(vulnMethodAdapter,
                    access, name, descriptor, signature, exceptions);
        }
        return mv;
    }
}
