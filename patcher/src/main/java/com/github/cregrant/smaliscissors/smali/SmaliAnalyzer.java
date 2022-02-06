package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.BackgroundWorker;
import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.structures.DecompiledFile;
import com.github.cregrant.smaliscissors.structures.SmaliClass;
import com.github.cregrant.smaliscissors.structures.SmaliMethod;
import com.github.cregrant.smaliscissors.structures.rules.RemoveCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

public class SmaliAnalyzer {

    public void clear(List<DecompiledFile> rawSmaliFiles, RemoveCode rule) {
        boolean benchmarkMode = false;
        long l = System.currentTimeMillis();
        do {
            for (String target : rule.targets) {
                if (!target.startsWith("L"))
                    target = "L" + target;
                if (target.endsWith("*"))
                    target = target.replace("*", "");
                else if (target.endsWith(".smali"))
                    target = target.replace(".smali", ";");     //target is a single file

                ArrayList<SmaliClass> classes = separateSmali(rawSmaliFiles, target);
                deleteFields(classes, target);
                ArrayList<SmaliMethod> methods = extractMethods(classes, target);
                ArrayList<SmaliMethod> cleanedMethods = new ArrayList<>(methods.size());
                Main.out.println("Begin cleaning " + methods.size() + " methods (" + target + ").\n");

                ArrayList<Future<?>> futures = new ArrayList<>(100);
                for (int i = methods.size() - 1; i >= 0; i--) {
                    SmaliMethod method = methods.get(i);
                    String finalTarget = target;
                    Runnable r = () -> {
                        new MethodCleaner().cleanupMethod(method, finalTarget);
                        //todo add error handling
                        cleanedMethods.add(method);
                    };
                    futures.add(BackgroundWorker.executor.submit(r));
                }
                BackgroundWorker.compute(futures);

                if (false || benchmarkMode)     //prevent write changes
                    continue;

                SmaliClass smaliClass;
                for (SmaliMethod method : cleanedMethods) {
                    smaliClass = method.getParentClass();
                    StringBuilder sb = new StringBuilder(smaliClass.getBody());
                    int origLength = sb.length();
                    int startIndex = sb.indexOf(method.getOldSignature());
                    if (startIndex == -1) {
                        Main.out.println("Error: old method not found!");
                        continue;
                    }

                    int endIndex = sb.indexOf(".end method", startIndex) + 13;
                    String changedPart = method.getNewSignature() + System.lineSeparator() + method.getBody() + System.lineSeparator();
                    sb.replace(startIndex, endIndex, changedPart);
                    if (sb.length() == origLength)
                        Main.out.println("Error: nothing changed in " + smaliClass.getPath());
                    smaliClass.setBody(sb.toString());
                }

                for (SmaliClass readyClass : classes) {
                    readyClass.getFile().setBody(readyClass.getBody());
                }
            }

            if (benchmarkMode) {
                System.out.println("\rLoop takes " + (System.currentTimeMillis() - l) + " ms");
                l = System.currentTimeMillis();
            }
        } while (benchmarkMode && System.currentTimeMillis() != 0);
    }

    private ArrayList<SmaliClass> separateSmali(List<DecompiledFile> rawSmaliFiles, String target) {
        List<SmaliClass> classes = Collections.synchronizedList(new ArrayList<>(100));
        ArrayList<Future<?>> futures = new ArrayList<>(100);
        for (DecompiledFile df : rawSmaliFiles) {
            Runnable r = () -> {
                String body = df.getBody();
                if (body.lastIndexOf('\r', body.indexOf(';') + 3) != -1)    //get rid of windows \r
                    body = body.replace("\r", "");

                int pos = body.indexOf(target);
                while (pos >= 0) {
                    char c = 0;
                    int backPos = pos;
                    while (!(c == '\n' || c == '#' || c == '\"'))   //backward search for a chars
                        c = body.charAt(--backPos);

                    if (c == '\n') {     //line is not commented out and not inside an annotation
                        SmaliClass smaliClass = new SmaliClass(df);
                        smaliClass.setBody(body);
                        classes.add(smaliClass);
                        break;
                    }
                    pos = body.indexOf(target, pos + 5);
                }
            };
            futures.add(BackgroundWorker.executor.submit(r));
        }
        BackgroundWorker.compute(futures);
        return new ArrayList<>(classes);
    }

    void deleteFields(ArrayList<SmaliClass> classes, String target) {
        int count = 0;
        //ArrayList<SmaliField> fields = new ArrayList<>();
        for (SmaliClass smaliClass : classes) {
            String body = smaliClass.getBody();
            String[] lines;
            int end = body.indexOf(".method");
            String oldPiece = null;
            if (end == -1)
                lines = body.split("\n");
            else {
                oldPiece = body.substring(0, end);
                lines = oldPiece.split("\n");
            }

            boolean changed = false;
            for (int i = 0, linesLength = lines.length; i < linesLength; i++) {
                String line = lines[i];
                if (line.startsWith(".field") && line.contains(target)) {
                    //String name = line.substring(line.lastIndexOf(' ') + 1);
                    //SmaliField field = new SmaliField(smaliClass, name);
                    //fields.add(field);
                    lines[i] = "#" + line;
                    changed = true;
                }
            }
            if (changed) {
                StringBuilder sb = new StringBuilder(2000);
                for (String str : lines) {
                    sb.append(str).append("\n");
                }

                String newBody;
                if (end == -1)
                    newBody = sb.toString();
                else {
                    newBody = body.replace(oldPiece, sb.toString());
                }

                smaliClass.setBody(newBody);
                count++;
            }
        }
        Main.out.println(count + " fields deleted.");
    }

    ArrayList<SmaliMethod> extractMethods(ArrayList<SmaliClass> classes, String target) {
        ArrayList<SmaliMethod> methods = new ArrayList<>();
        for (SmaliClass smaliClass : classes) {
            String classBody = smaliClass.getBody();
            int s = classBody.indexOf(".method");
            if (s == -1)
                continue;
            String[] lines = classBody.substring(s).split("\n");
            for (int i = 0; i < lines.length; ) {
                if (lines[i].startsWith(".method")) {
                    String signature = lines[i];
                    int start = i;

                    StringBuilder bodyBuilder = new StringBuilder();
                    do {
                        i++;
                        bodyBuilder.append(lines[i]).append("\n");
                    }
                    while (!lines[i].equals(".end method"));
                    String methodBody = bodyBuilder.toString();

                    /*i = start;
                    do {
                        i++;
                    }
                    while (!lines[i].equals(".end method"));
                    String[] ss = Arrays.copyOfRange(lines, start + 1, i + 1);
                    if (ss.length < methodBody.length())
                        methodBody = ss[0];*/

                    if (signature.contains(target) || methodBody.contains(target)) {
                        SmaliMethod method = new SmaliMethod(smaliClass, signature);
                        method.setBody(methodBody);
                        methods.add(method);
                    }
                } else
                    i++;
            }
        }
        return methods;          //catch all methods that contain some targets.
    }
}
