package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;

import java.io.IOException;

public interface IRule {
    enum Type {
        MATCH_ASSIGN,
        MATCH_REPLACE,
        MATCH_GOTO,
        ADD_FILES,
        REMOVE_FILES,
        REMOVE_CODE,
        DUMMY,
        GOTO,
        EXECUTE_DEX
    }

    String getName();

    boolean integrityCheckPassed();

    boolean smaliNeeded();

    boolean xmlNeeded();

    String nextRuleName();

    void apply(Project project, Patch patch) throws IOException;
}
