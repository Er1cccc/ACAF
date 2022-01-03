package com.er1cccc.acaf.util;

import com.er1cccc.acaf.core.entity.ClassFile;
import com.google.common.reflect.ClassPath;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("all")
public class RtUtil {
    private static final Logger logger = Logger.getLogger(RtUtil.class);

    public static List<ClassFile> getAllClassesFromJars(List<String> jarPathList) {
        logger.info("get all classes");
        Set<ClassFile> classFileSet = new HashSet<>();
        for (String jarPath : jarPathList) {
            classFileSet.addAll(JarUtil.resolveNormalJarFile(jarPath));
        }
        classFileSet.addAll(getRuntimeClasses());
        return new ArrayList<>(classFileSet);
    }

    public static List<ClassFile> getAllClassesFromBoot(List<String> jarPathList,
                                                        boolean useAllLib) {
        Set<ClassFile> classFileSet = new HashSet<>();
        for (String jarPath : jarPathList) {
            classFileSet.addAll(JarUtil.resolveSpringBootJarFile(jarPath, useAllLib));
        }
        classFileSet.addAll(getRuntimeClasses());
        return new ArrayList<>(classFileSet);
    }

    private static List<ClassFile> getRuntimeClasses() {
        try {
            URL stringClassUrl = Object.class.getResource("String.class");
            URLConnection connection = null;
            if (stringClassUrl != null) {
                connection = stringClassUrl.openConnection();
            }
            Set<ClassFile> result = new HashSet<>();
            if (connection instanceof JarURLConnection) {
                URL runtimeUrl = ((JarURLConnection) connection).getJarFileURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{runtimeUrl});
                for (ClassPath.ClassInfo classInfo : ClassPath.from(classLoader).getAllClasses()) {
                    ClassNameClassVisitor classNameClassVisitor = new ClassNameClassVisitor();
                    ClassReader classReader = new ClassReader(classLoader.getResourceAsStream(classInfo.getResourceName()));
                    classReader.accept(classNameClassVisitor,ClassReader.EXPAND_FRAMES);

                    result.add(new ClassFile(classNameClassVisitor.getName(),
                            classLoader.getResourceAsStream(classInfo.getResourceName())));
                }
            }
            return new ArrayList<>(result);
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return new ArrayList<>();
    }
}
