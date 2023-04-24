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
            ArrayList<String> extractedList = IO.extract(patch.getFile(), patch.getTempDir().getPath(), source);
            if (extractedList.size() != 1) {
                logger.error("Extracted {} files, expected 1 zip file", extractedList.size());
                return;
            }
            File extractedZipFile = new File(extractedList.get(0));
            extractedPathList = IO.extract(extractedZipFile, dstLocation, null);
            patch.deleteTempDir();
        } else {
            extractedPathList = IO.extract(patch.getFile(), dstLocation, source);
        }

        project.scan(extractedPathList);
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
