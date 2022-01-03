package com.er1cccc.acaf;

import com.beust.jcommander.JCommander;
import com.er1cccc.acaf.config.*;
import com.er1cccc.acaf.core.audit.auditcore.CallGraph;
import com.er1cccc.acaf.core.audit.auditcore.InheritanceMap;
import com.er1cccc.acaf.core.audit.auditcore.InheritanceUtil;
import com.er1cccc.acaf.core.audit.discovery.*;
import com.er1cccc.acaf.core.entity.*;
import com.er1cccc.acaf.core.resolver.impl.CompositResolver;
import com.er1cccc.acaf.core.template.VulnTemplateSinkVisitor;
import com.er1cccc.acaf.data.DataLoader;
import com.er1cccc.acaf.util.DataUtil;
import com.er1cccc.acaf.util.DirUtil;
import com.er1cccc.acaf.util.RtUtil;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Launcher {
    private static final Logger logger = Logger.getLogger(Launcher.class);

    public static void launch(ACAFConfigurer configurer,String[] args) throws Exception {
        logger.info("start code inspector");
        Command command = new Command();
        JCommander jc = JCommander.newBuilder().addObject(command).build();
        jc.parse(args);
        if (command.help) {
            jc.usage();
        }
        if (command.boots != null && command.boots.size() != 0) {
            doLaunch(command.boots, command.templates, command.packageName, configurer);
        }
    }

    private static void doLaunch(List<String> boots,  List<String> templates, String packageName, ACAFConfigurer configurer) throws Exception{
        List<ClassFile> templateClassFileList = RtUtil.getAllClassesFromJars(templates);
        List<ClassFile> targetClassFileList = RtUtil.getAllClassesFromBoot(boots, true);
        Path targetSaveDataDirPath = Paths.get("audit_target");
        Path templateSaveDataDirPath = Paths.get("audit_template");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DirUtil.removeDir(targetSaveDataDirPath.toFile());
            DirUtil.removeDir(templateSaveDataDirPath.toFile());
        }));
        prepareLaunch(templateClassFileList,templateSaveDataDirPath);
        prepareLaunch(targetClassFileList,targetSaveDataDirPath);

        // 包名
        String finalPackageName = packageName.replace(".", "/");
        // 获取全部controller
        SpringDiscovery springDiscovery = new SpringDiscovery();
        springDiscovery.discover(targetClassFileList, finalPackageName,targetSaveDataDirPath);
        List<SpringController> controllers = springDiscovery.getControllers();

        SourceRegistry sourceRegistry = new SourceRegistry();
        SanitizeRegistry sanitizeRegistry = new SanitizeRegistry();
        SinkRegistry sinkRegistry = new SinkRegistry();
        configurer.addSource(sourceRegistry);
        configurer.addSanitize(sanitizeRegistry);
        configurer.addSink(sinkRegistry);

        VulnDiscovery vulnDiscovery = new VulnDiscovery(targetClassFileList, sourceRegistry,sanitizeRegistry,sinkRegistry, targetSaveDataDirPath,templateSaveDataDirPath);
        vulnDiscovery.discover(controllers);

//        // 画出指定package的调用图
//        DrawService.start(discoveredCalls, finalPackageName, classMap, methodImplMap);
    }

    private static void prepareLaunch(List<ClassFile> classFileList, Path saveDataDirPath) throws IOException {
        if(!saveDataDirPath.toFile().exists()){
            saveDataDirPath.toFile().mkdirs();
        }
        // 读取JDK和输入Jar所有class资源
        // 获取所有方法和类
        File dir = saveDataDirPath.toFile();
        if (!new File(dir,"classes.dat").exists() || !new File(dir,"methods.dat").exists()
                || !new File(dir,"inheritanceMap.dat").exists()) {
            logger.info("Running method discovery...");
            MethodDiscovery methodDiscovery = new MethodDiscovery();
            methodDiscovery.discover(classFileList);
            methodDiscovery.save(saveDataDirPath);
        }

        if (!new File(dir,"passthrough.dat").exists()) {
            logger.info("Analyzing methods for passthrough dataflow...");
            PassthroughDiscovery passthroughDiscovery = new PassthroughDiscovery();
            passthroughDiscovery.discover(classFileList,saveDataDirPath);
            passthroughDiscovery.save(saveDataDirPath);
        }


        if (!new File(dir,"callgraph.dat").exists()) {
            logger.info("Analyzing methods in order to build a call graph...");
            CallGraphDiscovery callGraphDiscovery = new CallGraphDiscovery();
            callGraphDiscovery.discover(classFileList,saveDataDirPath);
            callGraphDiscovery.save(saveDataDirPath);
        }
    }


}
