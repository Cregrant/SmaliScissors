package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.utils.IO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Add implements IRule {
    public String name;
    public String target;
    public String source;
    public boolean extract;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return target != null && source != null;
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
        if (extract) {
            patch.createTempDir();
            ArrayList<String> extractedList = IO.extract(patch.getFile(), patch.getTempDir().getPath(), source);
            if (extractedList.size() != 1) {
                Main.out.println("What? Zip extract failed???");
                return;
            }
            File extractedZipFile = new File(extractedList.get(0));
            extractedPathList = IO.extract(extractedZipFile, dstLocation, null);
            patch.deleteTempDir();
        }
        else
            extractedPathList = IO.extract(patch.getFile(), dstLocation, source);

        project.scan(extractedPathList);
    }

    private String getLastSmaliFolder(Project project) {
        if (!target.startsWith("smali/"))
            return target;

        String[] subfolders = new File(project.getPath()).list((dir, name) -> name.startsWith("smali"));
        if (subfolders == null || subfolders.length == 1)
            return target;

        for (int i = subfolders.length - 1; i >= 0; i--) {
            String sub = subfolders[i];
            if (sub.startsWith("smali_classes"))
                return sub + target.substring(5);
        }
        return target;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    ADD_FILES\n");
        if (name != null)
            sb.append("Name:    ").append(name).append('\n');
        sb.append("Target:  ").append(target).append("\n");
        sb.append("Source:  ").append(source).append('\n');
        sb.append("Extract: ").append(extract);
        return sb.toString();
    }
}
