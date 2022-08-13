package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;

import java.io.IOException;

public interface Rule {
    String getName();

    boolean isValid();

    boolean smaliNeeded();

    boolean xmlNeeded();

    String nextRuleName();

    void apply(Project project, Patch patch) throws IOException;

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
}
