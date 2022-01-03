package com.er1cccc.acaf.util;


import lombok.Getter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

@Getter
public class ClassNameClassVisitor extends ClassVisitor {
    private String name;

    public ClassNameClassVisitor() {
        super(Opcodes.ASM6);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name=name;
    }

}
