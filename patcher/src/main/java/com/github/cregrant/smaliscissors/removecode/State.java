package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;

import java.util.HashSet;
import java.util.List;

class State {
    HashSet<SmaliFile> files;
    HashSet<SmaliFile> deletedFiles;
    HashSet<SmaliClass> patchedClasses;
    HashSet<SmaliTarget> removedTargets;

    State(List<SmaliFile> files) {
        this.files = new HashSet<>(files);
        this.deletedFiles = new HashSet<>();
        this.patchedClasses = new HashSet<>();
        this.removedTargets = new HashSet<>();
    }

    State(State state) {
        this.files = new HashSet<>(state.files);
        this.deletedFiles = new HashSet<>(state.deletedFiles);
        this.patchedClasses = new HashSet<>(state.patchedClasses);
        this.removedTargets = new HashSet<>(state.removedTargets);
    }
}
