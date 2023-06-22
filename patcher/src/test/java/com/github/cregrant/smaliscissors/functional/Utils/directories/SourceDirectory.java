package com.github.cregrant.smaliscissors.functional.Utils.directories;

import java.io.File;
import java.util.ArrayList;

public class SourceDirectory extends TestDirectory {

    private File apKFile;

    public SourceDirectory(File rootFolder) {
        super(rootFolder);
        rescan();
    }

    @Override
    public void rescan() {
        File[] files = getFiles();
        sourcesArchive = scanFile(files, ".tar.xz");
        apKFile = scanFile(files, ".apk");
        sources = scanSources(files);
    }

    @Override
    protected ArrayList<File> scanSources(File[] files) {
        ArrayList<File> result = super.scanSources(files);
        result.remove(apKFile);
        return result;
    }

    public File getApkFile() {
        return apKFile;
    }
}
