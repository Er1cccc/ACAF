package com.er1cccc.acaf.core.audit.discovery;

import com.er1cccc.acaf.core.audit.auditcore.SpringClassVisitor;
import com.er1cccc.acaf.core.entity.ClassFile;
import com.er1cccc.acaf.core.entity.ClassReference;
import com.er1cccc.acaf.core.entity.MethodReference;
import com.er1cccc.acaf.core.entity.SpringController;
import com.er1cccc.acaf.data.DataLoader;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpringDiscovery {
    private static final Logger logger = Logger.getLogger(SpringDiscovery.class);
    private List<SpringController> controllers=new ArrayList<>();

    public void discover(List<ClassFile> classFileList, String packageName, Path saveDataDirPath) {
        logger.info("discover spring classes");
        Map<ClassReference.Handle, ClassReference> classMap = DataLoader.loadClasses(saveDataDirPath);
        Map<MethodReference.Handle, MethodReference> methodMap = DataLoader.loadMethods(saveDataDirPath);

        for (ClassFile file : classFileList) {
            try {
                SpringClassVisitor mcv = new SpringClassVisitor(packageName,controllers,classMap,methodMap);
                InputStream ins = file.getInputStream();
                ClassReader cr = new ClassReader(ins);
                ins.close();
                cr.accept(mcv, ClassReader.EXPAND_FRAMES);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<SpringController> getControllers() {
        return controllers;
    }
}
