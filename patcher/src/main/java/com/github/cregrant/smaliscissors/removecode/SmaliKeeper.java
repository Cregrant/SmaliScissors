package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Flags;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.rule.types.Replace;
import com.github.cregrant.smaliscissors.rule.types.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.firebase_analytics_patched;
import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.firebase_crashlytics_patched;

public class SmaliKeeper {

    private static final Logger logger = LoggerFactory.getLogger(SmaliKeeper.class);
    private final Project project;
    private boolean scanned = false;
    private boolean firebaseCrashlyticsFound = false;
    private boolean firebaseAnalyticsFound = false;

    public SmaliKeeper(Project project) {
        this.project = project;
    }

    private void scan() {
        if (scanned) {
            return;
        }

        for (String s : project.getProtectedClasses()) {
            if (s.startsWith("com/google/firebase/components:com/google/firebase/crashlytics/")) {
                firebaseCrashlyticsFound = true;
            } else if (s.startsWith("com/google/firebase/components:com/google/firebase/analytics")) {
                firebaseAnalyticsFound = true;
            }
        }
        scanned = true;
    }

    void changeTargets(Patch patch, RemoveCode rule) {
        changeFirebaseCrashlytics(patch, rule);    //delete network calls because the code uses reflection
    }

    void keepClasses(State state) {
        scan();
        HashSet<SmaliClass> returned = new HashSet<>();
        for (SmaliFile file : state.deletedFiles) {      //keep activities, services anf other AndroidManifest.xml things
            String path = file.getPath();
            String shortPath = path.substring(path.indexOf("/") + 1, path.length() - 6);
            if (project.getProtectedClasses().contains(shortPath)) {
                SmaliClass smaliClass = new SmaliClass(project, file, file.getBody().replace("\r", ""));
                keepClass(state, returned, smaliClass);
            }
        }
        state.patchedClasses.addAll(returned);
        for (SmaliClass smaliClass : returned) {
            state.deletedFiles.remove(smaliClass.getFile());
        }
    }

    private void keepClass(State state, HashSet<SmaliClass> set, SmaliClass smaliClass) {
        scan();
        smaliClass.makeStub();
        set.add(smaliClass);
        String superclassPath = smaliClass.getSuperclass().substring(1, smaliClass.getSuperclass().length() - 1);
        if (!superclassPath.startsWith("android") && project.getProtectedClasses().contains(superclassPath)) {
            for (SmaliFile file : state.deletedFiles) {
                if (file.getPath().endsWith(superclassPath)) {
                    SmaliClass superclass = new SmaliClass(project, file, file.getBody().replace("\r", ""));
                    keepClass(state, set, superclass);
                    break;
                }
            }
        }
    }

    private void changeFirebaseCrashlytics(Patch patch, RemoveCode rule) {
        scan();
        if (firebaseCrashlyticsFound) {      //keep the code but delete network calls
            List<String> crashlyticsList = Arrays.asList("com/crashlytics/", "com/google/firebase/crashlytics/",
                    "com/google/firebase/crash/", "io/fabric/", "io/invertase/firebase/crashlytics/");
            if (rule.getTargets().removeAll(crashlyticsList)) {
                if (Boolean.parseBoolean(project.getProperties().get(firebase_crashlytics_patched))) {
                    return;
                }

                logger.warn("Cannot remove firebase crashlytics from code. Deleting network calls instead...");
                Replace replaceRule = createReplaceRule("\\\".*?crashlytics\\.com.*?\\\"");
                replaceRule.apply(project, patch);
                project.getProperties().set(firebase_crashlytics_patched, "true");
                if (!project.isSmaliCacheEnabled() || (!Flags.SMALI_DEBUG_DO_NOT_WRITE && project.isSmaliCacheEnabled())) {
                    project.getProperties().save();
                }
            }
        }
    }

    void changeFirebaseAnalytics(Patch patch, RemoveCode rule) {
        scan();
        if (firebaseAnalyticsFound) {      //keep the code but delete network calls
            if (rule.getTargets().contains("com/google/firebase/analytics/")
                    || rule.getTargets().contains("com/google/firebase/firebase_analytics/")) {
                if (Boolean.parseBoolean(project.getProperties().get(firebase_analytics_patched))) {
                    return;
                }

                logger.warn("Cannot remove firebase analytics from code. Deleting network calls instead...");
                Replace replaceRule = createReplaceRule("\\\".*?app-measurement\\.com.*?\\\"");
                replaceRule.apply(project, patch);
                project.getProperties().set(firebase_analytics_patched, "true");
                if (!project.isSmaliCacheEnabled() || (!Flags.SMALI_DEBUG_DO_NOT_WRITE && project.isSmaliCacheEnabled())) {
                    project.getProperties().save();
                }
            }
        }
    }

    private Replace createReplaceRule(String match) {
        Replace replaceRule = new Replace();
        replaceRule.setTarget("smali*/*.smali");
        replaceRule.setMatch(match);
        replaceRule.setReplacement("\"a\"");
        replaceRule.setRegex(true);
        replaceRule.setTargetType(Rule.TargetType.SMALI);
        return replaceRule;
    }
}
