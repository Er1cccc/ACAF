package com.er1cccc.acaf.core.audit.auditcore;

import com.er1cccc.acaf.core.entity.ClassFile;
import com.er1cccc.acaf.core.entity.ClassReference;
import com.er1cccc.acaf.core.entity.MethodReference;
import org.apache.log4j.Logger;
import org.objectweb.asm.*;

import java.util.*;

@SuppressWarnings("all")
public class DiscoveryClassVisitor extends ClassVisitor {
    private static final Logger logger = Logger.getLogger(DiscoveryClassVisitor.class);

    private String name;
    private String superName;
    private String[] interfaces;
    private boolean isInterface;
    private List<ClassReference.Member> members;
    private ClassReference.Handle classHandle;
    private Set<String> annotations;
    private List<ClassReference> discoveredClasses;
    private List<MethodReference> discoveredMethods;
    private ClassFile classFile;

    public DiscoveryClassVisitor(List<ClassReference> discoveredClasses,
                                 List<MethodReference> discoveredMethods,
                                 ClassFile classFile) {
        super(Opcodes.ASM6);
        this.discoveredClasses = discoveredClasses;
        this.discoveredMethods = discoveredMethods;
        this.classFile=classFile;
    }

    @Override
    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces) {
        this.name = name;
        this.superName = superName;
        this.interfaces = interfaces;
        this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        this.members = new ArrayList<>();
        this.classHandle = new ClassReference.Handle(name);
        annotations = new HashSet<>();
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        annotations.add(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        if ((access & Opcodes.ACC_STATIC) == 0) {
            Type type = Type.getType(desc);
            String typeName;
            if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                typeName = type.getInternalName();
            } else {
                typeName = type.getDescriptor();
            }
            members.add(new ClassReference.Member(name, access, new ClassReference.Handle(typeName)));
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        discoveredMethods.add(new MethodReference(
                classHandle,
                name,
                desc,
                isStatic));
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        ClassReference classReference = new ClassReference(
                name,
                superName,
                Arrays.asList(interfaces),
                isInterface,
                members,
                annotations);
        discoveredClasses.add(classReference);
        super.visitEnd();
    }
}
