package com.github.cregrant.smaliscissors.functional.Utils.directories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

public class SourceDirectory extends TestDirectory {

    private static final Logger logger = LoggerFactory.getLogger(SourceDirectory.class);
    private File apKFile;

    public SourceDirectory(File rootFolder) {
        super(rootFolder);
    }

    @Override
    public void rescan() {
        File[] files = getFiles();
        sourcesArchive = scanFile(files, ".tar.xz");
        apKFile = scanFile(files, ".apk");
        sources = scanSources(files);

        String errorMsg = "";
        if (sourcesArchive == null && sources.isEmpty()) {
            errorMsg += "No sources found inside " + rootFolder;
        }
        if (apKFile == null) {
            errorMsg += "Apk file not found inside " + rootFolder;
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
        result.remove(apKFile);
        return result;
    }

    public File getApkFile() {
        return apKFile;
    }
}
