package com.github.cregrant.smaliscissors.removecode.manifestparsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

public class DecompiledParser {

    private static final Logger logger = LoggerFactory.getLogger(DecompiledParser.class);
    private final String body;
    private int pos;

    public DecompiledParser(String body) {
        this.body = body;
    }

    public HashSet<String> parse() {
        HashSet<String> classes = new HashSet<>();
        String[] targets = new String[]{"<activity ", "<provider ", "<receiver ", "<service ", "<meta-data "};
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
