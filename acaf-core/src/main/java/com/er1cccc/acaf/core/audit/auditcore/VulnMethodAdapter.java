package com.er1cccc.acaf.core.audit.auditcore;

import com.er1cccc.acaf.config.PassthroughRegistry;
import com.er1cccc.acaf.core.entity.MethodReference;
import com.er1cccc.acaf.core.entity.impl.VisitMethodInsnInfoInstruction;
import com.er1cccc.acaf.core.resolver.Resolver;
import com.er1cccc.acaf.core.resolver.impl.CompositResolver;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VulnMethodAdapter extends CoreMethodAdapter<Boolean> {
    private final int access;
    private final String desc;
    private final int methodArgIndex;
    private final List<Boolean> pass;
    private Resolver vulnResolver;

    public VulnMethodAdapter(int methodArgIndex, List<Boolean> pass, InheritanceMap inheritanceMap,
                             Map<MethodReference.Handle, Set<Integer>> passthroughDataflow,
                             int api, MethodVisitor mv, String owner, int access, String name,
                             String desc, String signature, String[] exceptions, Resolver vulnResolver) {
        super(inheritanceMap, passthroughDataflow, api, mv, owner, access, name, desc, signature, exceptions);
        this.access = access;
        this.desc = desc;
        this.methodArgIndex = methodArgIndex;
        this.pass = pass;
        this.vulnResolver=vulnResolver;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        int localIndex = 0;
        int argIndex = 0;
        if ((this.access & Opcodes.ACC_STATIC) == 0) {
            localIndex += 1;
            argIndex += 1;
        }
        for (Type argType : Type.getArgumentTypes(desc)) {
            if (argIndex == this.methodArgIndex) {
                localVariables.set(localIndex, true);
            }
            localIndex += argType.getSize();
            argIndex += 1;
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        Set<Integer> controllableArgIndex = getControllableArgIndex(desc);
        boolean resolve = vulnResolver.resolve(new VisitMethodInsnInfoInstruction(opcode, owner, name, desc, itf, controllableArgIndex));
        super.visitMethodInsn(opcode, owner, name, desc, itf);
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

//    @Override
//    public void visitFieldInsn(int opcode, String owner, String name, String desc){
//        if(opcode==Opcodes.GETFIELD){
//            Set<Boolean> obj = operandStack.get(0);
//            super.visitFieldInsn(opcode,owner,name,desc);
//            //如果对象可控，那么认为对象的字段也是可控的
//            if(obj.contains(true))
//                operandStack.get(0).add(true);
//        }else{
//            super.visitFieldInsn(opcode,owner,name,desc);
//        }
//    }

}
