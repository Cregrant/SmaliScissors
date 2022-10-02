package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.util.IO;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class RemoveFiles implements Rule {
    private String name;
    private List<String> targets;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return targets != null && !targets.isEmpty();
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
        HashSet<File> possibleEmptyFolders = new HashSet<>();
        int deletedCount = 0;

        for (String target : targets) {
            List<String> removed = project.removeLoadedFile(project, target);
            deletedCount += removed.size();
            for (String str : removed) {
                File file = new File(project.getPath() + File.separator + str);
                IO.delete(file);
                possibleEmptyFolders.add(file.getParentFile());
                if (Prefs.logLevel.getLevel() == Prefs.Log.DEBUG.getLevel()) {
                    Main.out.println(file + " deleted");
                }
            }
        }

        for (File file : possibleEmptyFolders) {
            String[] subs = file.list();
            while (subs != null && subs.length == 0) {
                file.delete();
                file = file.getParentFile();     //delete parent folder if it is empty
                subs = file.list();
            }
        }
        if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel()) {
            Main.out.println(deletedCount + " files deleted");
        }
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: REMOVE_FILES.\n");
        if (name != null) {
            sb.append("Name: ").append(name).append('\n');
        }
        sb.append("Targets:\n");
        for (int i = 0; i < targets.size(); i++) {
            String target = targets.get(i);
            sb.append("    ").append(target).append("\n");
            if (Prefs.logLevel.getLevel() >= Prefs.Log.INFO.getLevel() && i >= 30) {
                sb.append("    ... + ").append(targets.size() - i - 1).append(" more lines\n");
                break;
            }
        }
        return sb.toString();
    }
}
