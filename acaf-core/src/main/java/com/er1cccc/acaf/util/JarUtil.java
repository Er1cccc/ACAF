package com.er1cccc.acaf.util;

import com.er1cccc.acaf.core.entity.ClassFile;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

@SuppressWarnings("all")
public class JarUtil {
    private static final Logger logger = Logger.getLogger(JarUtil.class);
    private static final Set<ClassFile> classFileSet = new HashSet<>();
    private static Path tempDirRoot;

    static {
        tempDirRoot=Paths.get("acaf_temp");
        tempDirRoot.toFile().mkdirs();
    }


    public static List<ClassFile> resolveSpringBootJarFile(String jarPath,boolean useAllLib) {
        try {
            int slash = Math.max(jarPath.lastIndexOf(47), jarPath.lastIndexOf(92));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                closeAll();
                DirUtil.removeDir(tempDirRoot.toFile());
            }));
            resolve(jarPath);//1、把jar包解压到临时文件目录，并给每个class文件创建ClassFile对象
            resolveBoot(jarPath);//把jar包里lib下的jar包解压到临时文件目录
            File tempDir = getTempDir(jarPath);
            if(useAllLib){
                Files.list(tempDir.toPath().resolve("BOOT-INF/lib")).forEach(p -> {//给lib目录下jar包执行1操作
                    resolveNormalJarFile(p.toFile().getAbsolutePath());
                });
            }
            return new ArrayList<>(classFileSet);
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return new ArrayList<>();
    }

    public static List<ClassFile> resolveNormalJarFile(String jarPath) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                closeAll();
                DirUtil.removeDir(tempDirRoot.toFile());
            }));
            resolve(jarPath);
            return new ArrayList<>(classFileSet);
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return new ArrayList<>();
    }

    private static File getTempDir(String jarPath) throws IOException {
        int slash = Math.max(jarPath.lastIndexOf(47), jarPath.lastIndexOf(92));
        String tempDir = jarPath;
        if (slash > -1) {
            tempDir = tempDir.substring(slash + 1);
        }
        File file = new File(tempDirRoot.toFile(), tempDir);
        return file;
    }

    private static void resolve(String jarPath) {
        try {
            File tmpDir = getTempDir(jarPath);
            if(!tmpDir.exists()){
                try {
                    tmpDir.mkdirs();
                    InputStream is = new FileInputStream(jarPath);
                    JarInputStream jarInputStream = new JarInputStream(is);
                    JarEntry jarEntry;
                    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                        Path fullPath = tmpDir.toPath().resolve(jarEntry.getName());
                        if (!jarEntry.isDirectory()) {
                            if (!jarEntry.getName().endsWith(".class")) {
                                continue;
                            }
                            Path dirName = fullPath.getParent();
                            if (!Files.exists(dirName)) {
                                Files.createDirectories(dirName);
                            }
                            OutputStream outputStream = Files.newOutputStream(fullPath);
                            IOUtil.copy(jarInputStream, outputStream);
                            InputStream fis = new FileInputStream(fullPath.toFile());
                            ClassReader classReader = new ClassReader(fis);
                            ClassNameClassVisitor classNameClassVisitor = new ClassNameClassVisitor();
                            classReader.accept(classNameClassVisitor,ClassReader.EXPAND_FRAMES);

                            ClassFile classFile = new ClassFile(classNameClassVisitor.getName(), new FileInputStream(fullPath.toFile()));
                            classFileSet.add(classFile);
                        }
                    }
                } catch (Exception e) {
                    logger.error("error ", e);
                }
            }else{
                try {
                    Files.walk(tmpDir.toPath()).forEach(p -> {
                        if(p.toString().endsWith(".class")){
                            InputStream fis = null;
                            try {
                                fis = new FileInputStream(p.toFile());
                                String className=p.toString().substring(tmpDir.toString().length()+1).replace("\\","/");
                                ClassFile classFile = new ClassFile(className, fis);
                                classFileSet.add(classFile);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void resolveBoot(String jarPath) {
        try {
            InputStream is = new FileInputStream(jarPath);
            JarInputStream jarInputStream = new JarInputStream(is);
            JarEntry jarEntry;
            File tempDir = getTempDir(jarPath);
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                Path fullPath = tempDir.toPath().resolve(jarEntry.getName());
                if (!jarEntry.isDirectory()) {
                    if (!jarEntry.getName().endsWith(".jar")) {
                        continue;
                    }
                    Path dirName = fullPath.getParent();
                    if (!Files.exists(dirName)) {
                        Files.createDirectories(dirName);
                    }
                    OutputStream outputStream = Files.newOutputStream(fullPath);
                    IOUtil.copy(jarInputStream, outputStream);
                }
            }
        } catch (Exception e) {
            logger.error("error ", e);
        }
    }

    private static void closeAll() {
        List<ClassFile> classFileList = new ArrayList<>(classFileSet);
        for (ClassFile classFile : classFileList) {
            classFile.close();
        }
    }
}