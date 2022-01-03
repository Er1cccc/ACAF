package com.er1cccc.acaf.core.template;

import com.er1cccc.acaf.config.PassthroughRegistry;
import com.er1cccc.acaf.config.Sink;
import com.er1cccc.acaf.core.audit.auditcore.CoreMethodAdapter;
import com.er1cccc.acaf.core.audit.auditcore.InheritanceMap;
import com.er1cccc.acaf.core.audit.discovery.PassthroughDiscovery;
import com.er1cccc.acaf.core.entity.MethodReference;
import com.er1cccc.acaf.core.resolver.impl.CompositResolver;
import com.er1cccc.acaf.core.resolver.impl.VisitMethodInsnInfoResolver;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class VulnTemplateSinkVisitor extends ClassVisitor {
    private static final Logger logger = Logger.getLogger(VulnTemplateSinkVisitor.class);
    private CompositResolver resolver;
    private String name;
    private String signature;
    private String superName;
    private String[] interfaces;
    private InheritanceMap inheritanceMap;
    private Map<MethodReference.Handle, Set<Integer>> passthrough;

    public VulnTemplateSinkVisitor(CompositResolver resolver, Path templateSaveDataDirPath, Sink sink) {
        super(Opcodes.ASM6);
        this.resolver=resolver;
        try{
            inheritanceMap = InheritanceMap.load(templateSaveDataDirPath);
            passthrough = PassthroughDiscovery.load(templateSaveDataDirPath);
            PassthroughRegistry passthroughRegistry = new PassthroughRegistry(passthrough);
            sink.addPassthrough(passthroughRegistry);
        }catch (IOException e){
            logger.error("error when loading inheritanceMap or passthrough",e);
        }
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
        if (name.equals("sinkMethod")) {
                VulnTemplateSinkMethodVisitor vulnTemplateSinkMethodVisitor = new VulnTemplateSinkMethodVisitor(inheritanceMap,passthrough,Opcodes.ASM6,mv,this.name,access,name,descriptor,signature,exceptions,this.resolver);
                return new JSRInlinerAdapter(vulnTemplateSinkMethodVisitor,
                        access, name, descriptor, signature, exceptions);
        }
        return mv;
    }

    private static class VulnTemplateSinkMethodVisitor extends CoreMethodAdapter<Boolean> {
        private CompositResolver resolver;
        private final int access;
        private final String desc;

        public VulnTemplateSinkMethodVisitor(InheritanceMap inheritanceMap,
                                             Map<MethodReference.Handle, Set<Integer>> passthroughDataflow,
                                             int api, MethodVisitor mv, String owner, int access, String name,
                                             String desc, String signature, String[] exceptions, CompositResolver resolver) {
            super(inheritanceMap, passthroughDataflow, api, mv, owner, access, name, desc, signature, exceptions);
            this.access = access;
            this.desc = desc;
            this.resolver=resolver;

        }


        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if(isGetParamMethod(owner,name,descriptor,isInterface)) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                operandStack.get(0).add(true);
            }else{
                Set<Integer> controllableArgIndexList = getControllableArgIndex(descriptor);
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                Type returnType = Type.getReturnType(descriptor);
                if(controllableArgIndexList.size()>0){
                    resolver.addResolver(new VisitMethodInsnInfoResolver(opcode,owner,name,descriptor,isInterface,controllableArgIndexList));
                }else if(!"void".equals(returnType.getClassName())){
                    if(operandStack.get(0).contains(true)){
                        resolver.addResolver(new VisitMethodInsnInfoResolver(opcode,owner,name,descriptor,isInterface,controllableArgIndexList));
                    }
                }
            }
        }

        private Set<Integer> getControllableArgIndex(String descriptor) {
            Type[] argTypes = Type.getArgumentTypes(descriptor);
            int stackIndex = 0;
            Set<Integer> controllableArgIndexList=new HashSet<>();
            for (int i = 0; i < argTypes.length; i++) {
                int argIndex = argTypes.length - 1 - i;
                Type type = argTypes[argIndex];
                Set<Boolean> param = operandStack.get(stackIndex);
                if (param.size() > 0 && param.contains(true)) {
                    controllableArgIndexList.add(argIndex);
                }
                stackIndex += type.getSize();
            }
            return controllableArgIndexList;
        }

        private boolean isGetParamMethod(String owner, String name, String descriptor, boolean isInterface) {
            return owner.equals("com/er1cccc/acaf/config/ControllableParam")&&
                    name.equals("getParameter")&&
                    descriptor.equals("(Ljava/lang/String;)Ljava/lang/Object;")&&
                    !isInterface;
        }
    }

}
