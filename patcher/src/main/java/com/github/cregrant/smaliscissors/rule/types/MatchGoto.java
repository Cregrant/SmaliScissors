package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.util.Regex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class MatchGoto implements Rule {
    private String name;
    private String target;
    private String match;
    private String goTo;
    private boolean isRegex;
    private boolean isSmali;
    private boolean isXml;
    private volatile boolean found;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return getTarget() != null && getMatch() != null && getGoTo() != null;
    }

    @Override
    public boolean smaliNeeded() {
        return isSmali();
    }

    @Override
    public boolean xmlNeeded() {
        return isXml();
    }

    @Override
    public String nextRuleName() {
        return found ? getGoTo() : null;
    }

    @Override
    public void apply(Project project, Patch patch) {
        final Pattern matchPattern = Pattern.compile(patch.applyAssign(getMatch()));
        final Pattern targetPattern = Pattern.compile(Regex.globToRegex(getTarget()));

        List<? extends DecompiledFile> files;
        if (isSmali()) {
            files = project.getSmaliList();
        } else if (isXml()) {
            files = project.getXmlList();
        } else {
            throw new IllegalStateException("Not smali nor xml rule.");
        }

        try {
            ArrayList<Future<?>> futures = new ArrayList<>(files.size());
            for (final DecompiledFile df : files) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (!found) {
                            if (!targetPattern.matcher(df.getPath()).matches()) {
                                return;
                            }

                            String body = df.getBody();
                            if (Regex.matchSingleLine(body, matchPattern) != null) {
                                found = true;
                                if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel()) {
                                    Main.out.println("Match found!");
                                }
                            }
                        }
                    }
                };
                futures.add(project.getExecutor().submit(r));
            }
            project.getExecutor().waitForFinish(futures);
        } catch (Exception e) {
            Main.out.println(e.getMessage());
        }
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getGoTo() {
        return goTo;
    }

    public void setGoTo(String goTo) {
        this.goTo = goTo;
    }

    public boolean isRegex() {
        return isRegex;
    }

    public void setRegex(boolean regex) {
        isRegex = regex;
    }

    public boolean isSmali() {
        return isSmali;
    }

    public void setSmali(boolean smali) {
        isSmali = smali;
    }

    public boolean isXml() {
        return isXml;
    }

    public void setXml(boolean xml) {
        isXml = xml;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:   MATCH_GOTO.\n");
        if (name != null) {
            sb.append("Name:   ").append(name).append('\n');
        }
        sb.append("Target: ").append(target).append("\n");
        sb.append("Match:  ").append(match).append('\n');
        sb.append("Goto:   ").append(goTo);
        return sb.toString();
    }
}
