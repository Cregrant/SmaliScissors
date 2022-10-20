package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

class State {
    HashSet<SmaliFile> files;
    HashSet<SmaliFile> deletedFiles;
    HashSet<SmaliClass> patchedClasses;
    HashSet<SmaliTarget> removedTargets;
    SmaliFile[] filesArray;

    State(Project project, List<SmaliFile> files) {
        this.files = new HashSet<>(files);
        this.deletedFiles = new HashSet<>();
        this.patchedClasses = new HashSet<>();
        this.removedTargets = new HashSet<>();
        if (!project.getMemoryManager().isExtremeLowMemory()) {
            this.filesArray = files.toArray(new SmaliFile[0]);
        }
    }

    State(State state) {
        this.files = new HashSet<>(state.files);
        this.deletedFiles = new HashSet<>(state.deletedFiles);
        this.patchedClasses = new HashSet<>(state.patchedClasses);
        this.removedTargets = new HashSet<>(state.removedTargets);
        if (state.filesArray != null) {
            this.filesArray = Arrays.copyOf(state.filesArray, state.filesArray.length);
        }
    }
}
