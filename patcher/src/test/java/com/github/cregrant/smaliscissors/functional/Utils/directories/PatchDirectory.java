package com.github.cregrant.smaliscissors.functional.Utils.directories;

import com.github.cregrant.smaliscissors.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatchDirectory extends TestDirectory {

    private static final Logger logger = LoggerFactory.getLogger(PatchDirectory.class);
    private File patchFile;
    private File removeStringsFile;
    private List<String> removeStrings;

    public PatchDirectory(File rootFolder) {
        super(rootFolder);
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

        String errorMsg = "";
        if (sourcesArchive == null && sources.isEmpty()) {
            errorMsg += "No sources found inside " + rootFolder;
        }
        if (patchFile == null && removeStrings.isEmpty()) {
            errorMsg += "Not zip patch nor txt file found inside " + rootFolder;
        }
        if (errorMsg.length() > 0) {
            logger.warn(errorMsg);
        }

        if (sourcesArchive == null) {
            sourcesArchive = new File(rootFolder, "source_patched.tar.xz");
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
