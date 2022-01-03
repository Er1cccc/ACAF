package com.er1cccc.acaf.core.audit.discovery;

import com.er1cccc.acaf.core.audit.auditcore.DiscoveryClassVisitor;
import com.er1cccc.acaf.core.audit.auditcore.InheritanceUtil;
import com.er1cccc.acaf.core.entity.ClassFile;
import com.er1cccc.acaf.core.entity.ClassReference;
import com.er1cccc.acaf.core.entity.MethodReference;
import com.er1cccc.acaf.data.DataLoader;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodDiscovery {
    private static final Logger logger = Logger.getLogger(MethodDiscovery.class);
    private final List<ClassReference> discoveredClasses = new ArrayList<>();
    private final List<MethodReference> discoveredMethods = new ArrayList<>();

    public void discover(List<ClassFile> classFileList) {
        logger.info("discover all classes");
        for (ClassFile file : classFileList) {
            try {
                DiscoveryClassVisitor dcv = new DiscoveryClassVisitor(discoveredClasses, discoveredMethods,file);
                InputStream ins = file.getInputStream();
                ClassReader cr = new ClassReader(ins);
                ins.close();
                cr.accept(dcv, ClassReader.EXPAND_FRAMES);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save(Path dirPath) throws IOException {
        DataLoader.saveData(new File(dirPath.toFile(),"classes.dat").toPath(), new ClassReference.Factory(), discoveredClasses);
        DataLoader.saveData(new File(dirPath.toFile(),"methods.dat").toPath(), new MethodReference.Factory(), discoveredMethods);

        Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
        for (ClassReference clazz : discoveredClasses) {
            classMap.put(clazz.getHandle(), clazz);
        }
        InheritanceUtil.derive(classMap).save(dirPath);
    }
}
