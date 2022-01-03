package com.er1cccc.acaf.core.audit.discovery;

import com.er1cccc.acaf.core.audit.auditcore.CallGraph;
import com.er1cccc.acaf.core.audit.auditcore.CallGraphClassVisitor;
import com.er1cccc.acaf.core.audit.auditcore.InheritanceMap;
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
import java.util.*;

public class CallGraphDiscovery {
    private static final Logger logger = Logger.getLogger(CallGraphDiscovery.class);
    private final Set<CallGraph> discoveredCalls = new HashSet<>();

    public void discover(List<ClassFile> classFileList,Path saveDataDirPath) throws IOException{
        logger.info("build call graph");
        Map<ClassReference.Handle, ClassReference> classMap = DataLoader.loadClasses(saveDataDirPath);
        InheritanceMap inheritanceMap = InheritanceMap.load(saveDataDirPath);
        Map<MethodReference.Handle, Set<Integer>> passthroughDataflow = PassthroughDiscovery.load(saveDataDirPath);

        for (ClassFile classFile:classFileList) {
            try (InputStream in = classFile.getInputStream()) {
                ClassReader cr = new ClassReader(in);
                try {
                    cr.accept(new CallGraphClassVisitor(classMap, inheritanceMap, passthroughDataflow,discoveredCalls),
                            ClassReader.EXPAND_FRAMES);
                } catch (Exception e) {
                    logger.error("Error analyzing: " + classFile.getClassName(), e);
                }
            }
        }

    }

    public void save(Path dirPath) throws IOException {
        DataLoader.saveData(new File(dirPath.toFile(),"callgraph.dat").toPath(), new CallGraph.Factory(), discoveredCalls);
    }

    public static List<CallGraph> load(Path dirPath) throws IOException {
        return DataLoader.loadData(new File(dirPath.toFile(),"callgraph.dat").toPath(), new CallGraph.Factory());
    }
}
