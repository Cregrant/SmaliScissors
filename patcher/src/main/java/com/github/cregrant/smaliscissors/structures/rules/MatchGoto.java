package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;
import com.github.cregrant.smaliscissors.structures.common.DecompiledFile;
import com.github.cregrant.smaliscissors.utils.Regex;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class MatchGoto implements IRule {
    public String name;
    public String target;
    public String match;
    public String goTo;
    public boolean isRegex;
    public boolean isSmali;
    public boolean isXml;
    private volatile boolean found;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return target != null && match != null && goTo != null;
    }

    @Override
    public boolean smaliNeeded() {
        return isSmali;
    }

    @Override
    public boolean xmlNeeded() {
        return isXml;
    }

    @Override
    public String nextRuleName() {
        return found ? goTo : null;
    }

    @Override
    public void apply(Project project, Patch patch) {
        Pattern matchPattern = Pattern.compile(patch.applyAssign(match));
        Pattern targetPattern = Pattern.compile(Regex.globToRegex(target));

        ArrayList<DecompiledFile> files = new ArrayList<>(0);
        if (isSmali)
            files.addAll(project.getSmaliList());
        else if (isXml)
            files.addAll(project.getXmlList());

        try {
            ArrayList<Future<?>> futures = new ArrayList<>(files.size());
            for (DecompiledFile df : files) {
                Runnable r = () -> {
                    if (!found) {
                        if (!targetPattern.matcher(df.getPath()).matches())
                            return;

                        String body = df.getBody();
                        if (Regex.matchSingleLine(body, matchPattern) != null) {
                            found = true;
                            if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel())
                                Main.out.println("Match found!");
                        }
                    }
                };
                futures.add(BackgroundWorker.submit(r));
            }
            BackgroundWorker.compute(futures);
        } catch (Exception e) {
            Main.out.println(e.getMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:   MATCH_GOTO.\n");
        if (name != null)
            sb.append("Name:   ").append(name).append('\n');
        sb.append("Target: ").append(target).append("\n");
        sb.append("Match:  ").append(match).append('\n');
        sb.append("Goto:   ").append(goTo);
        return sb.toString();
    }
}
