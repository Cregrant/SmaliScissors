package com.github.cregrant.smaliscissors.manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.InputMismatchException;

public class DecompiledParser {

    private static final Logger logger = LoggerFactory.getLogger(DecompiledParser.class);
    private final String body;
    private int pos;

    public DecompiledParser(String body) {
        this.body = body;
    }

    public String getApplicationPath() {
        if (hasNext("<application ")) {
            String result = getNext();
            pos = 0;
            return result;
        }
        throw new InputMismatchException();
    }

    public ArrayList<String> getActivityPaths() {
        ArrayList<String> result = new ArrayList<>();
        while (hasNext("<activity ")) {
            result.add(getNext());
        }
        pos = 0;
        if (result.isEmpty()) {
            throw new InputMismatchException();
        }
        return result;
    }

    public ArrayList<String> getLauncherActivityPaths() {
        ArrayList<String> result = new ArrayList<>();
        while (hasNext("<activity ")) {
            if (hasLauncherIntent()) {
                result.add(getNext());
            } else {
                pos++;
            }
        }
        pos = 0;
        if (result.isEmpty()) {
            throw new InputMismatchException();
        }
        return result;
    }

    public HashSet<String> getProtectedClasses() {
        HashSet<String> classes = new HashSet<>();
        String[] targets = new String[]{"<activity ", "<provider ", "<receiver ", "<service ", "<meta-data "};
        for (String target : targets) {
            while (hasNext(target)) {
                classes.add(getNext());
            }
            pos = 0;
        }
        return classes;
    }

    private boolean hasLauncherIntent() {
        int intentPos = body.indexOf("\"android.intent.category.LAUNCHER\"", pos);
        int endPos = body.indexOf("</activity>", pos);
        return intentPos != -1 && intentPos < endPos;
    }

    private boolean hasNext(String target) {
        pos = body.indexOf(target, pos);
        return pos != -1;
    }

    private String getNext() {
        int start = body.indexOf("name=\"", pos) + 6;
        int end = body.indexOf("\"", start);
        String tmp = body.substring(start, end).replace('.', '/');
        pos = end;
        return tmp;
    }
}
