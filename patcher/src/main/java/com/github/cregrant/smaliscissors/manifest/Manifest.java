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
                throw new InputMismatchException("Cannot find smali file " + innerPath);
            }
        }
        return result;
    }

    private HashSet<String> parseProtectedClasses() {
        HashSet<String> strings = null;
        XmlFile manifestFile = findManifestFile();
        if (manifestFile != null) {
            strings = new DecompiledParser(manifestFile.getBody()).getProtectedClasses();
            if (!strings.isEmpty()) {
                logger.debug("Used decompiled AndroidManifest.xml");
                return strings;
            }
        }

        if (project.getApkPath() != null) {
            BinaryParser parser = new BinaryParser(project.getApkPath());
            strings = parser.getStrings();
        }
        if (strings == null || strings.isEmpty()) {
            throw new InputMismatchException("Unable to parse either the AndroidManifest.xml or an APK file. Please provide the APK file next to your project folder or decompile your project with resources.");
        }
        return strings;
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
            logger.debug("Parsed {} protected classes", protectedClasses.size());
        }
        return protectedClasses;
    }

    public DecompiledFile getApplicationFile() {
        if (applicationFile == null) {
            parseManifestFiles();
            logger.debug("Parsed application file: {}", applicationFile.getPath());
        }
        return applicationFile;
    }

    public List<DecompiledFile> getActivityFiles() {
        if (activityFiles == null) {
            parseManifestFiles();
            logger.debug("Parsed {} activity files", activityFiles.size());
        }
        return activityFiles;
    }

    public List<DecompiledFile> getLauncherActivityFiles() {
        if (launcherActivityFiles == null) {
            parseManifestFiles();
            logger.debug("Parsed {} launcher activity files", launcherActivityFiles.size());
        }
        return launcherActivityFiles;
    }

}
