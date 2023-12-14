package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;

import java.util.Collection;
import java.util.HashSet;

class State {
    final HashSet<SmaliFile> files;
    final HashSet<SmaliFile> deletedFiles;
    final HashSet<SmaliClass> patchedClasses;
    final HashSet<SmaliTarget> removedTargets;

    State(Collection<SmaliFile> files) {
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
