package com.github.cregrant.smaliscissors.manifest;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.XmlFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class Manifest {

    private static final Logger logger = LoggerFactory.getLogger(Manifest.class);
    private final Project project;
    private HashSet<String> protectedClasses;
    private List<DecompiledFile> activityFiles;
    private List<DecompiledFile> launcherActivityFiles;
    private DecompiledFile applicationFile;

    public Manifest(Project project) {
        this.project = project;
    }

    private void parseManifestFiles() {
        XmlFile manifestFile = findManifestFile();
        if (manifestFile != null) {
            DecompiledParser decompiledParser = new DecompiledParser(manifestFile.getBody());
            activityFiles = findSmaliFile(decompiledParser.getActivityPaths());
            launcherActivityFiles = findSmaliFile(decompiledParser.getLauncherActivityPaths());
            applicationFile = findSmaliFile(decompiledParser.getApplicationPath());
        } else {
            throw new InputMismatchException("This patch requires xml files to be decompiled.");
        }
    }

    private DecompiledFile findSmaliFile(String innerPath) {
        return findSmaliFile(Collections.singletonList(innerPath)).get(0);
    }

    private List<DecompiledFile> findSmaliFile(List<String> innerPaths) {
        List<DecompiledFile> result = new ArrayList<>();
        File[] projectFolders = new File(project.getPath()).listFiles();
        for (String innerPath : innerPaths) {
            if (!innerPath.endsWith(".smali")) {
                innerPath += ".smali";
            }

            boolean success = false;
            for (File folder : Objects.requireNonNull(projectFolders)) {
                File file = new File(folder, innerPath);
                if (file.exists()) {
                    result.add(new SmaliFile(project, file.getPath().substring(project.getPath().length() + 1)));
                    success = true;
                    break;
                }
            }
            if (!success) {
                logger.error("Cannot find smali file {}", innerPath);
                throw new IllegalStateException();
            }
        }
        return result;
    }

    private HashSet<String> parseProtectedClasses() {
        XmlFile manifestFile = findManifestFile();
        if (manifestFile != null) {
            return new DecompiledParser(manifestFile.getBody()).getProtectedClasses();
        }
        if (project.getApkPath() != null) {
            BinaryParser parser = new BinaryParser(project.getApkPath());
            return parser.getStrings();
        }
        logger.warn("Both the AndroidManifest.xml and the apk file path are missing. [REMOVE_CODE] may break your project.");
        return new HashSet<>();
    }

    private XmlFile findManifestFile() {
        File manifestFile = new File(project.getPath() + File.separator + "AndroidManifest.xml");
        if (manifestFile.exists()) {
            return new XmlFile(project, "AndroidManifest.xml");
        }
        return null;
    }

    public HashSet<String> getProtectedClasses() {
        if (protectedClasses == null) {
            protectedClasses = parseProtectedClasses();
        }
        return protectedClasses;
    }

    public DecompiledFile getApplicationFile() {
        if (applicationFile == null) {
            parseManifestFiles();
        }
        return applicationFile;
    }

    public List<DecompiledFile> getActivityFiles() {
        if (activityFiles == null) {
            parseManifestFiles();
        }
        return activityFiles;
    }

    public List<DecompiledFile> getLauncherActivityFiles() {
        if (launcherActivityFiles == null) {
            parseManifestFiles();
        }
        return launcherActivityFiles;
    }

}
