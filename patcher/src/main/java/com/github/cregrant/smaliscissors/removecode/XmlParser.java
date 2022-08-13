package com.github.cregrant.smaliscissors.removecode;

import java.util.HashSet;

public class XmlParser {
    private final String body;
    private int pos;

    public XmlParser(String body) {
        this.body = body;
    }

    public HashSet<String> parse() {
        HashSet<String> classes = new HashSet<>();
        String[] targets = new String[] {"<activity ", "<provider ", "<receiver ", "<service "};
        for (String target : targets) {
            while (hasNext(target)) {
                classes.add(getNext());
            }
        }
        return classes;
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
