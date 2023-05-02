package com.github.cregrant.smaliscissors.rule;

public class RuleGenerator {

    private final StringBuilder text = new StringBuilder();
    private int endOffset;
    private int endPos;

    public RuleGenerator() {
    }

    public RuleGenerator addHeader(String header) {
        if (text.length() != 0) {
            throw new IllegalStateException();
        }

        text.append(header);
        endPos = header.indexOf('[', 1);
        endOffset = header.length() - endPos;
        return this;
    }

    public RuleGenerator addParameter(String parameter, String value) {
        if (text.length() == 0) {
            throw new IllegalStateException();
        }

        if (!value.isEmpty() && !value.endsWith("\n")) {
            value += '\n';
        }

        text.insert(endPos, parameter + value);
        endPos = text.length() - endOffset;
        return this;
    }

    @Override
    public String toString() {
        return text.toString();
    }

    public static class Headers {
        public static final String ADD = "[ADD_FILES]\n[/ADD_FILES]";
        public static final String ASSIGN = "[MATCH_ASSIGN]\n[/MATCH_ASSIGN]";
        public static final String DUMMY = "[DUMMY]\n[/DUMMY]";
        public static final String EXECUTE_DEX = "[EXECUTE_DEX]\n[/EXECUTE_DEX]";
        public static final String GOTO = "[GOTO]\n[/GOTO]";
        public static final String MATCH_GOTO = "[MATCH_GOTO]\n[/MATCH_GOTO]";
        public static final String REMOVE_CODE = "[REMOVE_CODE]\n[/REMOVE_CODE]";
        public static final String REMOVE_CODE_ACTION = "[REMOVE_CODE_ACTION]\n[/REMOVE_CODE_ACTION]";
        public static final String REMOVE_FILES = "[REMOVE_FILES]\n[/REMOVE_FILES]";
        public static final String REPLACE = "[MATCH_REPLACE]\n[/MATCH_REPLACE]";
        public static final String INVALID = "[WHAT]\n[/HELP_ME]";
    }

    public static class Parameters {
        public static final String NAME = "NAME:\n";
        public static final String TARGET = "TARGET:\n";
        public static final String SOURCE = "SOURCE:\n";
        public static final String EXTRACT = "EXTRACT:\n";
        public static final String MATCH = "MATCH:\n";
        public static final String REGEX = "REGEX:\n";
        public static final String ASSIGN = "ASSIGN:\n";
        public static final String GOTO = "GOTO:\n";
        public static final String REPLACE = "REPLACE:\n";
        public static final String SCRIPT = "SCRIPT:\n";
        public static final String SMALI_NEEDED = "SMALI_NEEDED:\n";
        public static final String MAIN_CLASS = "MAIN_CLASS:\n";
        public static final String ENTRANCE = "ENTRANCE:\n";
        public static final String PARAM = "PARAM:\n";
        public static final String ACTION = "ACTION:\n";
    }
}
