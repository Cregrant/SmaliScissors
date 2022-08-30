package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.rule.types.Replace;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SmaliKeeper {
    private final Project project;
    private boolean firebaseCrashlyticsFound = false;
    private boolean firebaseCrashlyticsPatched = false;

    private boolean firebaseAnalyticsFound = false;
    private boolean firebaseAnalyticsPatched = false;

    public SmaliKeeper(Project project) {
        this.project = project;

        for (String s : project.getProtectedClasses()) {
            if (s.startsWith("com/google/firebase/components:com/google/firebase/crashlytics/")) {
                firebaseCrashlyticsFound = true;
            } else if (s.startsWith("com/google/firebase/components:com/google/firebase/analytics")) {
                firebaseAnalyticsFound = true;
            }
        }
    }

    void changeTargets(Patch patch, RemoveCode rule) {
        changeFirebaseCrashlytics(patch, rule);    //delete network calls because the code uses reflection
        //changeFirebaseAnalytics(patch, rule);
    }

    void keepClasses(SmaliWorker.State state) {
        HashSet<SmaliClass> returned = new HashSet<>();
        for (SmaliFile file : state.deletedFiles) {      //keep activities, services anf other AndroidManifest.xml things
            String path = file.getPath();
            String shortPath = path.substring(path.indexOf("/") + 1, path.length() - 6);
            if (project.getProtectedClasses().contains(shortPath)) {
                SmaliClass smaliClass = new SmaliClass(project, file, file.getBody().replace("\r", ""));
                keepClass(state, returned, smaliClass);
                returned.add(smaliClass);
            }
        }
        state.patchedClasses.addAll(returned);
        for (SmaliClass smaliClass : returned) {
            state.deletedFiles.remove(smaliClass.getFile());
        }
    }

    private void keepClass(SmaliWorker.State state, HashSet<SmaliClass> set, SmaliClass smaliClass) {
        smaliClass.makeStub();
        set.add(smaliClass);
        String shortPath = smaliClass.getSuperclass().substring(1, smaliClass.getSuperclass().length() - 1);
        if (!shortPath.startsWith("android") && project.getProtectedClasses().contains(shortPath)) {
            for (SmaliFile file : state.deletedFiles) {
                if (file.getPath().endsWith(shortPath)) {
                    SmaliClass superclass = new SmaliClass(project, file, file.getBody().replace("\r", ""));
                    keepClass(state, set, superclass);
                    break;
                }
            }
        }
    }

    private void changeFirebaseCrashlytics(Patch patch, RemoveCode rule) {
        if (firebaseCrashlyticsFound) {      //keep the code but delete network calls
            List<String> crashlyticsList = Arrays.asList("com/crashlytics/", "com/google/firebase/crashlytics/", "com/google/firebase/crash/", "io/fabric/", "io/invertase/firebase/crashlytics/");
            if (rule.getTargets().removeAll(crashlyticsList)) {
                if (firebaseCrashlyticsPatched) {
                    return;
                }
                Main.out.println("It is not possible to remove firebase crashlytics from code. Deleting network calls...");
                Replace replaceRule = createReplaceRule("\\\".*?crashlytics\\.com.*?\\\"");
                replaceRule.apply(project, patch);
                firebaseCrashlyticsPatched = true;
            }
        }
    }

    private void changeFirebaseAnalytics(Patch patch, RemoveCode rule) {
        if (firebaseAnalyticsFound) {      //keep the code but delete network calls
            List<String> analyticsList = Arrays.asList("com/google/firebase/analytics/", "com/google/firebase/firebase_analytics/");
            if (rule.getTargets().removeAll(analyticsList)) {
                if (firebaseAnalyticsPatched) {
                    return;
                }
                Main.out.println("It is not possible to remove firebase analytics from code. Deleting network calls...");
                Replace replaceRule = createReplaceRule("\\\".*?app-measurement\\.com.*?\\\"");
                replaceRule.apply(project, patch);
                firebaseAnalyticsPatched = true;
            }
        }
    }

    private Replace createReplaceRule(String match) {
        Replace replaceRule = new Replace();
        replaceRule.setTarget("smali*/*.smali");
        replaceRule.setMatch(match);
        replaceRule.setReplacement("\"a\"");
        replaceRule.setRegex(true);
        replaceRule.setSmali(true);
        return replaceRule;
    }
}
