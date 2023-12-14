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

import java.util.*;

import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.firebase_analytics_patched;
import static com.github.cregrant.smaliscissors.common.ProjectProperties.Property.firebase_crashlytics_patched;

public class SmaliKeeper {

    private static final Logger logger = LoggerFactory.getLogger(SmaliKeeper.class);
    private static final ArrayList<String> xmlProtectedPackages = new ArrayList<>(Collections.singletonList("Landroid/view/"));
    private final Project project;
    private HashSet<String> protectedClassRefs;
    private Map<String, String> hierarchy;
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

        protectedClassRefs = project.getManifest().getProtectedClasses();
        for (String s : protectedClassRefs) {
            if (s.startsWith("com/google/firebase/components:com/google/firebase/crashlytics/")) {
                firebaseCrashlyticsFound = true;
            } else if (s.startsWith("com/google/firebase/components:com/google/firebase/analytics")) {
                firebaseAnalyticsFound = true;
            }
        }

        fillProtectedClassRefs(protectedClassRefs);

        scanned = true;
    }

    private void fillProtectedClassRefs(HashSet<String> oldProtectedClassRefs) {
        HashSet<String> diff = new HashSet<>();
        for (String protectedClass : oldProtectedClassRefs) {
            String ref = 'L' + protectedClass + ';';
            if (hierarchy.containsKey(ref)) {
                diff.add(ref);
            }
        }
        ArrayList<String> xmlUsedClasses = new ArrayList<>();
        for (String xmlProtectedPackage : xmlProtectedPackages) {
            for (Map.Entry<String, String> entry : hierarchy.entrySet()) {
                if (entry.getValue().startsWith(xmlProtectedPackage)) {
                    findAllChildClassesRecursive(xmlUsedClasses, entry.getKey());
                }
            }
        }
        diff.addAll(xmlUsedClasses);

        HashSet<String> newProtectedClasses = new HashSet<>();
        while (!diff.isEmpty()) {
            HashSet<String> newDiff = new HashSet<>();
            for (String ref : diff) {
                String superclassRef = hierarchy.get(ref);
                if (superclassRef == null || newProtectedClasses.contains(ref)) {
                    continue;
                }
                newProtectedClasses.add(ref);
                newDiff.add(superclassRef);
            }
            diff = newDiff;
        }
        this.protectedClassRefs = newProtectedClasses;     //cleaned protected classes with their superclasses (recursive)
    }

    private void findAllChildClassesRecursive(ArrayList<String> childClasses, String superclassRef) {
        childClasses.add(superclassRef);
        for (Map.Entry<String, String> entry : hierarchy.entrySet()) {
            if (entry.getValue().equals(superclassRef)) {
                findAllChildClassesRecursive(childClasses, entry.getKey());
            }
        }
    }

    void changeTargets(Patch patch, RemoveCode rule) {
        changeFirebaseCrashlytics(patch, rule);    //delete network calls because the code uses reflection
    }

    void keepClasses(State state) {
        scan();
        HashSet<SmaliClass> setToKeep = new HashSet<>();
        for (SmaliFile file : state.deletedFiles) {      //keep activities, services and other AndroidManifest.xml things
            String path = SmaliTarget.removePathObfuscation(file.getPath());
            String ref = 'L' + path.substring(path.indexOf("/") + 1, path.length() - 6) + ';';
            if (protectedClassRefs.contains(ref)) {
                SmaliClass smaliClass = new SmaliClass(project, file, file.getBody().replace("\r", ""));
                smaliClass.makeStub();
                setToKeep.add(smaliClass);
            }
        }
        state.patchedClasses.addAll(setToKeep);
        for (SmaliClass smaliClass : setToKeep) {
            state.deletedFiles.remove(smaliClass.getFile());
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

    public void setHierarchy(Map<String, String> hierarchy) {
        this.hierarchy = hierarchy;
    }
}
