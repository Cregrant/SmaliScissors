package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;
import com.github.cregrant.smaliscissors.structures.DecompiledFile;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class MatchGoto implements IRule {
    public String name;
    public String match;
    public String goTo;
    public boolean isRegex;
    private boolean found;
    public boolean isSmali = false;
    public boolean isXml = false;
    public ArrayList<String> targets;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return targets != null && !targets.isEmpty() && match != null && goTo != null;
    }

    @Override
    public String nextRuleName() {
        return found ? goTo : null;
    }

    @Override
    public boolean canBeMerged(IRule otherRule) {
        return false;
    }

    @Override
    public void apply(Project project, Patch patch) {
        AtomicBoolean running = new AtomicBoolean(true);
        Pattern pattern = Pattern.compile(patch.applyAssign(match));

        ArrayList<DecompiledFile> files = new ArrayList<>(0);
        if (isSmali)
            files.addAll(project.getSmaliList());
        else if (isXml)
            files.addAll(project.getXmlList());

        int totalNum = files.size();
        try {
            ArrayList<Future<?>> futures = new ArrayList<>(totalNum);
            for (int num = 0; num < totalNum; num++) {
                int finalNum = num;
                Runnable r = () -> {
                    if (running.get()) {
                        String body = files.get(finalNum).getBody();
                        if (Regex.matchSingleLine(body, pattern) != null) {
                            found = true;
                            running.set(false);
                            Main.out.println("Match found!");
                        }
                    }
                };
                futures.add(BackgroundWorker.executor.submit(r));
            }
            BackgroundWorker.compute(futures);
        } catch (Exception e) {
            Main.out.println(e.getMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  MATCH_GOTO.\n");
        if (name != null)
            sb.append("Name:  ").append(name).append('\n');
        sb.append("Targets:\n");
        for (String target : targets)
            sb.append("    ").append(target).append("\n");
        sb.append("Match: ").append(match).append('\n');
        sb.append("Goto:  ").append(goTo).append('\n');
        return sb.toString();
    }
}
