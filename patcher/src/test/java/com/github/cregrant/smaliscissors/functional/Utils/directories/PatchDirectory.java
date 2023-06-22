package com.github.cregrant.smaliscissors.functional.Utils.directories;

import com.github.cregrant.smaliscissors.util.IO;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatchDirectory extends TestDirectory {

    private File patchFile;
    private File removeStringsFile;
    private List<String> removeStrings;

    public PatchDirectory(File rootFolder) {
        super(rootFolder);
        rescan();
    }

    public void rescan() {
        File[] files = getFiles();
        sourcesArchive = scanFile(files, ".tar.xz");
        patchFile = scanFile(files, ".zip");
        removeStringsFile = scanFile(files, "remove.txt");
        sources = scanSources(files);

        if (removeStringsFile != null) {
            removeStrings = Arrays.asList(IO.read(removeStringsFile.getPath()).split("\\R"));
        } else {
            removeStrings = new ArrayList<>();
        }
    }

    @Override
    protected ArrayList<File> scanSources(File[] files) {
        ArrayList<File> result = super.scanSources(files);
        result.remove(patchFile);
        result.remove(removeStringsFile);
        return result;
    }

    public List<String> getRemoveStrings() {
        return removeStrings;
    }

    public File getPatchFile() {
        return patchFile;
    }
}
