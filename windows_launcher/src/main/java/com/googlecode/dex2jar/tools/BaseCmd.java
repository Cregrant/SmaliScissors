/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2012 Panxiaobo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.dex2jar.tools;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

public abstract class BaseCmd {
    public static String getBaseName(String fn) {
        int x = fn.lastIndexOf('.');
        return x >= 0 ? fn.substring(0, x) : fn;
    }

    public static String getBaseName(Path fn) {
        return getBaseName(fn.getFileName().toString());
    }

    public interface FileVisitorX {
        // change the relative from Path to String
        // java.nio.file.ProviderMismatchException on jdk8
        void visitFile(Path file, String relative);
    }

    public static void createParentDirectories(Path p) throws IOException {
        // merge patch from t3stwhat, fix crash on save to windows path like 'C:\\abc.jar'
        Path parent = p.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    public static FileSystem createZip(Path output) throws IOException {
        Map<String, Object> env = new HashMap<>();
        env.put("create", "true");
        Files.deleteIfExists(output);

        createParentDirectories(output);

        for (FileSystemProvider p : FileSystemProvider.installedProviders()) {
            String s = p.getScheme();
            if ("jar".equals(s) || "zip".equalsIgnoreCase(s)) {
                return p.newFileSystem(output, env);
            }
        }
        throw new IOException("cant find zipfs support");
    }

    @SuppressWarnings("serial")
    protected static class HelpException extends RuntimeException {

        public HelpException() {
            super();
        }

        public HelpException(String message) {
            super(message);
        }

    }

    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = { ElementType.FIELD })
    public @interface Opt {
        String argName() default "";

        String description() default "";

        boolean hasArg() default true;

        String longOpt() default "";

        String opt() default "";

        boolean required() default false;
    }

    static protected class Option implements Comparable<Option> {
        public String argName = "arg";
        public String description;
        public Field field;
        public boolean hasArg = true;
        public String longOpt;
        public String opt;
        public boolean required = false;

        @Override
        public int compareTo(Option o) {
            int result = s(this.opt, o.opt);
            if (result == 0) {
                result = s(this.longOpt, o.longOpt);
                if (result == 0) {
                    result = s(this.argName, o.argName);
                    if (result == 0) {
                        result = s(this.description, o.description);
                    }
                }
            }
            return result;
        }

        private static int s(String a, String b) {
            if (a != null && b != null) {
                return a.compareTo(b);
            } else if (a != null) {
                return 1;
            } else if (b != null) {
                return -1;
            } else {
                return 0;
            }
        }

        public String getOptAndLongOpt() {
            StringBuilder sb = new StringBuilder();
            boolean havePrev = false;
            if (opt != null && opt.length() > 0) {
            sb.append("-").append(opt);
                havePrev = true;
            }
            if (longOpt != null && longOpt.length() > 0) {
                if (havePrev) {
                sb.append(",");
            }
                sb.append("--").append(longOpt);
            }
            return sb.toString();
        }

    }

    protected Map<String, Option> optMap = new HashMap<>();

    protected String[] remainingArgs;
    protected String[] orginalArgs;

    public BaseCmd() {
    }

    public BaseCmd(String cmdLineSyntax, String header) {
        super();
    }

    public BaseCmd(String cmdName, String cmdSyntax, String header) {
        super();
    }

    private static Set<Option> collectRequriedOptions(Map<String, Option> optMap) {
        Set<Option> options = new HashSet<>();
        for (Map.Entry<String, Option> e : optMap.entrySet()) {
            Option option = e.getValue();
            if (option.required) {
                options.add(option);
            }
        }
        return options;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static Object convert(String value, Class type) {
        if (type.equals(String.class)) {
            return value;
        }
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return Integer.parseInt(value);
        }
        if (type.equals(long.class) || type.equals(Long.class)) {
            return Long.parseLong(value);
        }
        if (type.equals(float.class) || type.equals(Float.class)) {
            return Float.parseFloat(value);
        }
        if (type.equals(double.class) || type.equals(Double.class)) {
            return Double.parseDouble(value);
        }
        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        if (type.equals(File.class)) {
            return new File(value);
        }
        if (type.equals(Path.class)) {
            return new File(value).toPath();
        }
        try {
            type.asSubclass(Enum.class);
            return Enum.valueOf(type, value);
        } catch (Exception ignored) {
        }

        throw new RuntimeException("can't convert [" + value + "] to type " + type);
    }

    protected abstract void doCommandLine() throws Exception;

    public void doMain(String... args) {
        try {
            initOptions();
            parseSetArgs(args);
            doCommandLine();
        } catch (HelpException e) {
            String msg = e.getMessage();
            if (msg != null && msg.length() > 0) {
                System.err.println("ERROR: " + msg);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected void initOptionFromClass(Class<?> clz) {
        if (clz == null) {
            return;
        } else {
            initOptionFromClass(clz.getSuperclass());
        }

        Field[] fs = clz.getDeclaredFields();
        for (Field f : fs) {
            Opt opt = f.getAnnotation(Opt.class);
            if (opt != null) {
                f.setAccessible(true);
                Option option = new Option();
                option.field = f;
                option.description = opt.description();
                option.hasArg = opt.hasArg();
                option.required = opt.required();
                if (opt.longOpt().isEmpty() && opt.opt().isEmpty()) {   // into automode
                    option.longOpt = fromCamel(f.getName());
                    if (f.getType().equals(boolean.class)) {
                        option.hasArg=false;
                        try {
                            if (f.getBoolean(this)) {
                                throw new RuntimeException("the value of " + f + " must be false, as it is declared as no args");
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    checkConflict(option, "--" + option.longOpt);
                    continue;
                }
                if (!opt.hasArg()) {
                    if (!f.getType().equals(boolean.class)) {
                        throw new RuntimeException("the type of " + f
                                + " must be boolean, as it is declared as no args");
                    }

                    try {
                        if (f.getBoolean(this)) {
                            throw new RuntimeException("the value of " + f + " must be false, as it is declared as no args");
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                boolean haveLongOpt = false;
                if (!opt.longOpt().isEmpty()) {
                    option.longOpt = opt.longOpt();
                    checkConflict(option, "--" + option.longOpt);
                    haveLongOpt = true;
                }
                if (!opt.argName().isEmpty()) {
                    option.argName = opt.argName();
                }
                if (!opt.opt().isEmpty()) {
                    option.opt = opt.opt();
                    checkConflict(option, "-" + option.opt);
                } else {
                    if (!haveLongOpt) {
                        throw new RuntimeException("opt or longOpt is not set in @Opt(...) " + f);
                    }
                }
            }
        }
    }

    private static String fromCamel(String name) {
        if (name.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        char[] charArray = name.toCharArray();
        sb.append(Character.toLowerCase(charArray[0]));
        for (int i = 1; i < charArray.length; i++) {
            char c = charArray[i];
            if (Character.isUpperCase(c)) {
                sb.append("-").append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void checkConflict(Option option, String key) {
        if (optMap.containsKey(key)) {
            Option preOption = optMap.get(key);
            throw new RuntimeException(String.format("[@Opt(...) %s] conflict with [@Opt(...) %s]",
                    preOption.field.toString(), option.field
            ));
        }
        optMap.put(key, option);
    }

    protected void initOptions() {
        initOptionFromClass(this.getClass());
    }

    public static void main(String... args) throws Exception {
        Class<?> clz = Class.forName(args[0]);
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        if (BaseCmd.class.isAssignableFrom(clz)) {
            BaseCmd baseCmd = (BaseCmd) clz.getDeclaredConstructor().newInstance();
            baseCmd.doMain(newArgs);
        } else {
            Method m = clz.getMethod("main",String[].class);
            m.setAccessible(true);
            m.invoke(null, (Object)newArgs);
        }
    }
    
    protected void parseSetArgs(String... args) throws IllegalArgumentException, IllegalAccessException {
        this.orginalArgs = args;
        List<String> remainsOptions = new ArrayList<>();
        Set<Option> requiredOpts = collectRequriedOptions(optMap);
        Option needArgOpt = null;
        for (String s : args) {
            if (needArgOpt != null) {
                needArgOpt.field.set(this, convert(s, needArgOpt.field.getType()));
                needArgOpt = null;
            } else if (!s.isEmpty() && s.charAt(0) == '-') {// its a short or long option
                Option opt = optMap.get(s);
                requiredOpts.remove(opt);
                if (opt == null) {
                    System.err.println("ERROR: Unrecognized option: " + s);
                    throw new HelpException();
                } else {
                    if (opt.hasArg) {
                        needArgOpt = opt;
                    } else {
                        opt.field.set(this, true);
                    }
                }
            } else {
                remainsOptions.add(s);
            }
        }

        if (needArgOpt != null) {
            System.err.println("ERROR: Option " + needArgOpt.getOptAndLongOpt() + " need an argument value");
            throw new HelpException();
        }
        this.remainingArgs = remainsOptions.toArray(new String[0]);
        if (!requiredOpts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("ERROR: Options: ");
            boolean first = true;
            for (Option option : requiredOpts) {
                if (first) {
                    first = false;
                } else {
                    sb.append(" and ");
                }
                sb.append(option.getOptAndLongOpt());
            }
            sb.append(" is required");
            System.err.println(sb.toString());
            throw new HelpException();
        }

    }

}
