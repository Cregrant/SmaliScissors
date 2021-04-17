package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.structures.SmaliMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

public class RemoveCode {
    private Stack<String> registers = new Stack<>();
    private Stack<Integer> registerStartPosition = new Stack<>();
    public static boolean deleteInsteadofComment = false;

    public void cleanupMethod(SmaliMethod method, String target) {
        String body = method.getBody();
        String[] lines = body.split("\\R");
        scanMethodSignature(method, target);
        scanMethodBody(lines, target);
        deleteRegisters(method, lines);
        StringBuilder sb = new StringBuilder(200);
        for (String s : lines) {
            sb.append(s);
            if (Prefs.isWindows)
                sb.append("\r\n");
            else
                sb.append("\n");
        }
        String newBody = sb.toString();
        method.setBody(newBody);
    }

    private void deleteRegisters(SmaliMethod method, String[] lines) {
        long time = System.currentTimeMillis();
        ArrayList<String> preventLoopsIf = new ArrayList<>();
        while (!registers.isEmpty()) {
            String register = registers.pop();
            int i = registerStartPosition.pop();
            ArrayList<Integer> preventLoopsGoto = new ArrayList<>();

            for (; i<lines.length; i++) {
                if (System.currentTimeMillis()-time>1000) {
                    Main.out.println("Too long operation: " + method.getPath() + " - " + register);
                }
                if (lines[i].length()<=4 || lines[i].startsWith("#"))
                    continue;
                String line = lines[i];
                String outputRegister = outputRegister(lines, i);
                boolean inputRegisterUsed = inputRegisterUsed(line, register);
                if (line.startsWith("    return")) {
                    if (inputRegisterUsed)
                        addHelpMark(method, lines, i);
                    break;
                }
                else if (line.startsWith("    goto")) {
                    if (!preventLoopsGoto.contains(i)) {
                        preventLoopsGoto.add(i);
                        i = searchTag(lines, i);
                    }
                    else
                        break;
                }
                else if (line.startsWith("    if")) {
                    if (inputRegisterUsed)
                        addHelpMark(method, lines, i);
                    if (!preventLoopsIf.contains(register+i)) {
                        preventLoopsIf.add(register+i);
                        registers.add(register);
                        registerStartPosition.add(searchTag(lines, i));
                    }
                }
                else if (inputRegisterUsed) {
                    deleteLine(lines, i);
                    if (outputRegister!=null && !outputRegister.equals(register)) {
                        registers.add(register);
                        registerStartPosition.add(i);
                    }
                }
                else if (outputRegister!=null && outputRegister.equals(register))
                    break;
            }
        }
    }

    private String outputRegister(String[] lines, int i) {
        String line = lines[i];
        if (line.startsWith("    invoke") && lines[i+2].startsWith("    move-result"))
            return lines[i+2].substring(lines[i+2].lastIndexOf(' ') + 1);
        else if (line.startsWith("    sub") || line.startsWith("    if") || line.startsWith("    sget") ||
                line.startsWith("    iget") || line.startsWith("    const") || line.startsWith("    new") ||
                (!line.startsWith("    move-result") && line.startsWith("    move")))
            return line.substring(line.indexOf(' ', 6) + 1, line.indexOf(','));
            //else if (line.startsWith("    return"))
            //    return line.substring(line.indexOf(' ', 6) + 1);
        else
            return null;
    }



    private void scanMethodSignature(SmaliMethod method, String target) {
        ArrayList<String> inputObjects = method.inputObjects;
        int size = inputObjects.size();
        ArrayList<String> inputObjectsCleaned = new ArrayList<>(size);

        for (int i=0; i<size; i++) {
            String obj = inputObjects.get(i);
            if (obj.contains(target)) {
                registerStartPosition.add(0);
                if (method.isStatic)
                    registers.add('p' + String.valueOf(i+1));
                else
                    registers.add('p' + String.valueOf(i));
            }
            else
                inputObjectsCleaned.add(obj);
        }
        method.inputObjectsCleaned = inputObjectsCleaned;
    }

    private void scanMethodBody(String[] lines, String target) {
        int size = lines.length;
        for (int i=0; i<size; i++) {
            if (lines[i].contains(target)) {
                String register = outputRegister(lines, i);
                deleteLine(lines, i);
                if (register != null) {
                    registers.add(register);
                    registerStartPosition.add(i);
                }
            }
        }
        registers.sort(Collections.reverseOrder());
        registerStartPosition.sort(Collections.reverseOrder());
    }

    static void addHelpMark(SmaliMethod method, String[] lines, int i) {
        if (!lines[i].contains("    #HELP")) {
            lines[i] = lines[i] + "    #HELP";
            Main.out.println(method.getPath() + " problem detected");
        }
    }
}

