package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.BackgroundWorker;
import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.structures.*;

import java.util.ArrayList;
import java.util.concurrent.Future;

public class SmaliAnalyzer {

    public void analyze(ArrayList<DecompiledFile> rawSmaliFiles, Rule rule) {
        for (String target : rule.targets) {
            if (!target.startsWith("L"))
                target = "L" + target;
            if (target.endsWith("*"))
                target = target.replace("*", "");
            else if (target.endsWith(".smali"))
                target = target.replace(".smali", ";"); //target is a single file
            ArrayList<SmaliClass> classes = separateSmali(rawSmaliFiles, target);
            ArrayList<SmaliField> fields = extractFields(classes, target);
            ArrayList<SmaliMethod> methods = extractMethods(classes, target);
            ArrayList<SmaliMethod> cleanedMethods = new ArrayList<>(methods.size());

            BackgroundWorker.createIfTerminated();
            Main.out.println("Begin cleaning " + methods.size() + " methods (" + target + ").");

            ArrayList<Future<?>> futures = new ArrayList<>(100);
            for (SmaliMethod method : methods) {
                String finalTarget = target;
                //Runnable r = () -> {
                new RemoveCode().cleanupMethod(method, finalTarget);
                //todo add error handling
                cleanedMethods.add(method);
                //};
                //futures.add(BackgroundWorker.executor.submit(r));
            }
            //BackgroundWorker.compute(futures);
            SmaliClass smaliClass;
            String body;
            int count = 0;
            for (SmaliMethod method : cleanedMethods) {
                smaliClass = method.getParentClass();
                body = smaliClass.getFile().getBody();
                int startIndex = body.indexOf(method.getOldSignature());
                if (startIndex == -1) {
                    Main.out.println("Error: old method not found!");
                    continue;
                }
                count++;
                int endIndex = body.indexOf(".end method", startIndex) + 12;
                String newBody = body.substring(0, startIndex) + method.getNewSignature() + '\n' + method.getBody() + body.substring(endIndex);
                if (body.equals(newBody))
                    Main.out.println("Error: nothing changed in " + smaliClass.getPath());
                smaliClass.getFile().setBody(newBody);
            }
            Main.out.println(count + " methods cleaned successfully.\n");
        }
    }

    private ArrayList<SmaliClass> separateSmali(ArrayList<DecompiledFile> rawSmaliFiles, String target) {
        ArrayList<SmaliClass> classes = new ArrayList<>(100);
        BackgroundWorker.createIfTerminated();
        ArrayList<Future<?>> futures = new ArrayList<>(100);
        for (DecompiledFile df : rawSmaliFiles) {
            //Runnable r = () -> {
            String body = df.getBody();
            int pos = body.indexOf(target);
            while (pos >= 0) {
                char c = 0;
                int backPos = pos;
                while (!(c == '\n' || c == '#' || c == '\"'))
                    c = body.charAt(--backPos);

                if (c == '\n') {     //line is not commented out and not inside an annotation
                    SmaliClass smaliClass = new SmaliClass(df, target);
                    classes.add(smaliClass);
                    break;
                }
                pos = body.indexOf(target, pos+5);
            }
            //};
            //futures.add(BackgroundWorker.executor.submit(r));
        }
        BackgroundWorker.compute(futures);
        return classes;
    }

    ArrayList<SmaliField> extractFields(ArrayList<SmaliClass> classes, String target) {
        int count = 0;
        ArrayList<SmaliField> fields = new ArrayList<>();
        for (SmaliClass smaliClass : classes) {
            String body = smaliClass.getFile().getBody();
            String[] lines;
            int end = body.indexOf(".method");
            String oldPiece = null;
            if (end == -1)
                lines = body.split("\\R");
            else {
                oldPiece = body.substring(0, end);
                lines = oldPiece.split("\\R");
            }

            boolean changed = false;
            for (int i = 0, linesLength = lines.length; i < linesLength; i++) {
                String line = lines[i];
                if (line.startsWith(".field") && line.contains(target)) {
                    String name = line.substring(line.lastIndexOf(" ") + 1);
                    SmaliField field = new SmaliField(smaliClass, name);
                    fields.add(field);
                    lines[i] = "#" + line;
                    changed = true;
                }
            }
            if (changed) {
                StringBuilder sb = new StringBuilder(2000);
                for (String str : lines) {
                    sb.append(str).append(System.lineSeparator());
                }

                String newBody;
                if (end == -1)
                    newBody = sb.toString();
                else {
                    newBody = body.replace(oldPiece, sb.toString());
                }

                smaliClass.getFile().setBody(newBody);
                count++;
            }
        }
        Main.out.println(count + " fields deleted.");
        return fields;          //catch all fields that stores some targets.
    }

    ArrayList<SmaliMethod> extractMethods(ArrayList<SmaliClass> classes, String target) {
        ArrayList<SmaliMethod> methods = new ArrayList<>();
        for (SmaliClass smaliClass : classes) {
            String classBody = smaliClass.getFile().getBody();
            int s = classBody.indexOf(".method");
            if (s == -1)
                continue;
            String[] lines = classBody.substring(s).split("\\R");
            int size = lines.length;
            for (int i = 0; i < size; ) {
                if (lines[i].startsWith(".method")) {
                    String signature = lines[i];
                    StringBuilder bodyBuilder = new StringBuilder();
                    i++;
                    while (!lines[i].contains(".end method")) {
                        bodyBuilder.append(lines[i]).append(System.lineSeparator());
                        i++;
                    }
                    bodyBuilder.append(".end method");

                    String methodBody = bodyBuilder.toString();
                    if (signature.contains(target) || methodBody.contains(target)) {
                        SmaliMethod method = new SmaliMethod(smaliClass, signature, methodBody);
                        methods.add(method);
                    }
                } else
                    i++;
            }
        }
        return methods;          //catch all methods that stores some targets.
    }
}
