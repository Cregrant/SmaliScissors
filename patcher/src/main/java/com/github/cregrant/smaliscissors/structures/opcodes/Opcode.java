package com.github.cregrant.smaliscissors.structures.opcodes;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.smali.RemoveCode;

public interface Opcode {

    void deleteLine(String[] lines, int i);

    static boolean inputRegisterUsed(String line, String register) {
        if (!line.startsWith("    invoke") && !line.startsWith("    iput") &&
                !line.startsWith("    if") && !line.startsWith("    sput"))
            return false;

        if (line.contains("..")) {
            int registerNum = Integer.parseInt(register.substring(1));
            int from = Integer.parseInt(line.substring(line.indexOf("{", 8)+2, line.indexOf("..")-1));
            int to = Integer.parseInt(line.substring(line.indexOf("..")+4, line.indexOf("}")));
            char type = line.charAt(line.indexOf("{")+1);
            return register.charAt(0) == type && registerNum >= from && registerNum <= to;
        }
        int start;
        int end;
        if (line.contains("{")) {
            start = line.indexOf("{", 9)+1;
            end = line.lastIndexOf("}")+1;
        }
        else {
            start = line.indexOf(" ", 9);
            end = line.length();
        }
        char[] chars = line.substring(start, end).toCharArray();
        StringBuilder sb = new StringBuilder(3);
        for (char ch : chars) {
            if (ch=='v' || ch=='p') {
                sb = new StringBuilder(3);
                sb.append(ch);
            }
            else if (ch>=48 && ch<=57)
                sb.append(ch);
            else if (ch==',' || ch=='}')
                if (sb.toString().equals(register))
                    return true;
        }
        return false;
    }

    static int searchTag(String[] lines, int i) {
        String line = lines[i];
        String tag;
        int start = line.lastIndexOf(':');
        int end = line.indexOf(' ', start);

        if (end!=-1)
            tag = "    " + line.substring(start, end);
        else
            tag = "    " + line.substring(start);

        int size = lines.length;
        for (int j=0; j<size; j++) {
            if (lines[j].startsWith(tag)) {
                return j;
            }
        }
        Main.out.println("Critical error: tag" + tag.replace("   ", "") + " not found.");
        System.exit(1);
        return 0;
    }

    static String commentLine(String line) {
        if (RemoveCode.deleteInsteadofComment)
            return "";
        else {
            if (line.startsWith("#"))
                return line;
            else
                return '#' + line;
        }
    }
}
