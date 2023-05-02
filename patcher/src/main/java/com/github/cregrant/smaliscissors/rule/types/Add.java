package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.rule.RuleParser;
import com.github.cregrant.smaliscissors.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import static com.github.cregrant.smaliscissors.rule.RuleParser.*;
import static com.github.cregrant.smaliscissors.util.Regex.matchSingleLine;

public class Add extends Rule {

    private static final Logger logger = LoggerFactory.getLogger(Add.class);
    private final String target;
    private final String source;
    private final boolean extract;

    public Add(String rawString) {
        super(rawString);
        target = matchSingleLine(rawString, TARGET);
        source = matchSingleLine(rawString, SOURCE);
        extract = RuleParser.parseBoolean(rawString, EXTRACT);
    }

    @Override
    public boolean isValid() {
        return target != null && source != null && !(extract && target.contains("\n"));
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        ArrayList<String> extractedPathList;
        String dstLocation = project.getPath() + File.separator + getFixedPath(project);
        if (extract) {
            patch.createTempDir();
            extractedPathList = extractArchive(patch.getFile(), dstLocation, patch.getTempDir().getPath(), source);
            patch.deleteTempDir();
        } else {
            extractedPathList = addFile(patch.getFile(), dstLocation, source);
        }

        project.scan(extractedPathList);
    }

    private ArrayList<String> addFile(File zipFile, String dstPath, String exactName) {
        return IO.extract(zipFile, dstPath, exactName);
    }

    private ArrayList<String> extractArchive(File zipFile, String dstPath, String tempDirPath, String exactName) {
        ArrayList<String> extractedArchive = IO.extract(zipFile, tempDirPath, exactName);
        if (extractedArchive.size() != 1) {
            logger.error("Extracted {} files, expected 1 zip file", extractedArchive.size());
            return new ArrayList<>(0);
        }
        File extractedZipFile = new File(extractedArchive.get(0));
        return IO.extract(extractedZipFile, dstPath, null);
    }

    private String getFixedPath(Project project) {  //try to resolve "Exception occurred while writing code_item for method"
        if (!target.startsWith("smali/")) {
            return target;
        }

        // smali/blahblah -> smali_classesX/blahblah
        String[] subfolders = new File(project.getPath()).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("smali_classes");
            }
        });
        if (subfolders == null || subfolders.length == 0) {
            return target;
        } else {
            return subfolders[subfolders.length - 1] + target.substring(5);
        }
    }

    public String getTarget() {
        return target;
    }

    public String getSource() {
        return source;
    }

    public boolean isExtract() {
        return extract;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    ADD_FILES\n");
        if (name != null) {
            sb.append("Name:    ").append(name).append('\n');
        }
        sb.append("Target:  ").append(target).append("\n");
        sb.append("Source:  ").append(source).append('\n');
        sb.append("Extract: ").append(extract);
        return sb.toString();
    }
}
