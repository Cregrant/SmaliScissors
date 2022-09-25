package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.util.IO;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

public class Add implements Rule {
    private String name;
    private String target;
    private String source;
    private boolean extract;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return getTarget() != null && getSource() != null;
    }

    @Override
    public boolean smaliNeeded() {
        return false;
    }

    @Override
    public boolean xmlNeeded() {
        return false;
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        ArrayList<String> extractedPathList;
        String dstLocation = project.getPath() + File.separator + getLastSmaliFolder(project);
        if (isExtract()) {
            patch.createTempDir();
            ArrayList<String> extractedList = IO.extract(patch.getFile(), patch.getTempDir().getPath(), getSource());
            if (extractedList.size() != 1) {
                Main.out.println("What? Zip extract failed???");
                return;
            }
            File extractedZipFile = new File(extractedList.get(0));
            extractedPathList = IO.extract(extractedZipFile, dstLocation, null);
            patch.deleteTempDir();
        } else {
            extractedPathList = IO.extract(patch.getFile(), dstLocation, getSource());
        }

        project.scan(extractedPathList);
    }

    private String getLastSmaliFolder(Project project) {
        if (!getTarget().startsWith("smali/")) {
            return getTarget();
        }

        String[] subfolders = new File(project.getPath()).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("smali");
            }
        });
        if (subfolders == null || subfolders.length == 1) {
            return getTarget();
        }

        for (int i = subfolders.length - 1; i >= 0; i--) {
            String sub = subfolders[i];
            if (sub.startsWith("smali_classes")) {
                return sub + getTarget().substring(5);
            }
        }
        return getTarget();
    }

    String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    boolean isExtract() {
        return extract;
    }

    public void setExtract(boolean extract) {
        this.extract = extract;
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
