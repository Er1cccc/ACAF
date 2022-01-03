package com.er1cccc.acaf.core.entity;

import com.beust.jcommander.Parameter;

import java.util.List;

public class Command {
    @Parameter(names = {"-h", "--help"}, description = "Help Info", help = true)
    public boolean help;

    @Parameter(names = {"-j", "--jar"}, description = "Scan Jar File")
    public List<String> jars;

    @Parameter(names = {"-b", "--boot"}, description = "Scan SpringBoot File")
    public List<String> boots;

    @Parameter(names = {"-t", "--template"}, description = "Scan SpringBoot File")
    public List<String> templates;

    @Parameter(names = {"-p", "--pack"}, description = "SpringBoot Package Name")
    public String packageName;
}
