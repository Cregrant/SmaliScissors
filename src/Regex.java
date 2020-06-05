import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.lang.System.out;

class Regex {
    private static ArrayList<String> matchList = new ArrayList<>();
    private static ArrayList<String> assignList = new ArrayList<>();
    private static ArrayList<String> replaceList = new ArrayList<>();
    private static ArrayList<String> projectSmaliList = new ArrayList<>();
    private static ArrayList<String> projectSmaliText = new ArrayList<>();
    private static ArrayList<String> ruleReplacementIntArr = new ArrayList<>();


    private static String rulesHandler(String projectPath, String rule){
        String patDetect = "\\[(.+?)][\\S\\s]*?\\[/.+?]";
        String patTarget = "TARGET:\\n(.+)";
        String patSource = "SOURCE:\\n(.+)";
        String patAssign = "\\n(.+?)=\\$\\{GROUP\\d\\}";
        String patMatch = "MATCH:\\n(.+)";
        String patReplacement = "REPLACE:\\n([\\S\\s]*?)\\n?\\[\\/MATCH_REPLACE]";
        String patReplacementInt = "REPLACE:\\n[\\S\\s]*?(\\$\\{GROUP\\d\\})";

        if (Main.rules_mode == 0){
            //other strings
            out.println();
        }

        String ruleType = match(patDetect, rule, "");
        String ruleTarget = match(patTarget, rule, "").replace("smali*\\*.smali", "*\\.smali").replace("/", File.separator);

        if (Main.verbose_level == 0) {
            out.println("Type - " + ruleType);
            out.println("Target - " + ruleTarget);
        }
        if (!ruleTarget.contains(".smali") && !(ruleType.equals("ADD_FILES") || ruleType.equals("REMOVE_FILES"))){
            out.println("Sorry, only smali rules supported.");
            ruleType="";
        }

        if (ruleTarget.contains("smali*\\*.smali")) ruleTarget = ruleTarget.replace("smali*\\*.smali", "smali");

        if (ruleType.equals("MATCH_ASSIGN")){
            String ruleMatch =  match(patMatch, rule, "");
            if (Main.verbose_level == 0) out.println("Match - " + ruleMatch);
            match(patAssign, rule, "assign");
            for (String text : projectSmaliList){
                if (text.contains(ruleTarget)){
                    String content = projectSmaliText.get(projectSmaliList.indexOf(text));
                    match(ruleMatch, content, "match");
                }
            }
            if (!assignList.isEmpty() && !matchList.isEmpty()) {
                for (int i = 0; i < assignList.size(); ) {
                    if (Main.verbose_level <= 1) out.println("Assign added: " + assignList.get(i) + " - " + matchList.get(i));
                    i++;
                }
            }
            else {
                out.println("Nothing found in assign rule.");
            }

        }

        if (ruleType.equals("MATCH_REPLACE")){
            String ruleMatch = match(patMatch, rule, "");
            if (!assignList.isEmpty() && !matchList.isEmpty()) {
                for (int i = 0; i < assignList.size(); ) {
                    if (assignList.size()!=matchList.size()){
                        out.println("Error in ASSIGN rule!!!");
                        out.println("assignList - " + assignList);
                        out.println("matchList - " + matchList);
                        System.exit(1);
                    }
                    ruleMatch = ruleMatch.replace("${" + assignList.get(i) + "}", matchList.get(i));
                    i++;
                }
            }
            String ruleReplacement = match(patReplacement, rule, "");
            match(patReplacementInt, rule, "replacementGroupNum");
            if (Main.verbose_level == 0) {
                out.println(rule);
                out.println("To replace - " + ruleMatch);
                out.println("Replacement - " + ruleReplacement);
            }
            int c = 0;
            int clone = -1;
            int j = 0;
            try{
                ruleMatch = "[\\S\\s]*?(?:" + ruleMatch + ")[\\S\\s]*?";           //fix no match issue?
                for (String str; j < projectSmaliText.size();) {                //refactor this method
                    str = projectSmaliText.get(j);
                    if (projectSmaliList.get(j).contains(ruleTarget) && str.matches(ruleMatch)) {
                        match(ruleMatch, str, "replace");
                        if (!ruleReplacementIntArr.isEmpty()) {
                            out.print("click\n");
                            for (int i = 0; i < replaceList.size(); ) {
                                ruleReplacement = ruleReplacement.replaceAll(ruleReplacementIntArr.get(i), replaceList.get(i));
                                i++;
                            }
                        }
                        IO.write(projectSmaliList.get(j), str.replaceAll(ruleMatch, ruleReplacement));
                        if (Main.verbose_level == 0){
                            if (clone != j){                                          //suppress clone messages
                                out.println(projectSmaliList.get(j) + " patched.");
                                clone = j;
                            }
                        }
                        c++;
                        replaceList.clear();
                        j--;
                    }
                    j++;
                }
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
                out.println("You can try again. It was (assign rule bug) or (your rule error). Sorry =(");
                return "error";
            }
            if (Main.verbose_level <= 1) out.println(c + " files patched.");
        }

        if (ruleType.equals("ADD_FILES")){
            String ruleSource = match(patSource, rule, "");
            if (Main.verbose_level == 0) out.println("Source - " + ruleSource);
            String src = System.getProperty("user.dir")+File.separator+"patches"+File.separator+"temp"+File.separator+ruleSource;
            String dst = projectPath+File.separator+ruleTarget;
            File file = new File(dst);
            if (file.exists()) IO.deleteInDirectory(file);
            IO.copy(src, dst);
            if (ruleSource.contains(".smali")){
                if (Main.verbose_level <= 1) out.println("Added.");
                projectSmaliList.add(projectPath+File.separator+ruleTarget);
                projectSmaliText.add(IO.read(projectPath+File.separator+ruleTarget));
            }
        }

        if (ruleType.equals("REMOVE_FILES")){
            String dst = projectPath+File.separator+ruleTarget;
            if (Main.verbose_level == 0) out.println("Dst - " + dst);
            IO.deleteInDirectory(new File(dst));
            ArrayList<String> temp = projectSmaliList;
            int i=0;
            while (i < temp.size()) {
                if (projectSmaliList.get(i).contains(ruleTarget)) {
                    projectSmaliText.remove(i);
                    projectSmaliList.remove(i);
                }
                i++;
            }
            if (Main.verbose_level <= 1) out.println("Removed.");
        }
        out.println();
        return "ok";
    }


    private static String match(String pattern, String content, String mode) {
        Pattern patWhatToMatch = Pattern.compile(pattern, Pattern.MULTILINE);
        Matcher ruleMatcher = patWhatToMatch.matcher(content);
        String ruleMatched = "";
        while (ruleMatcher.find()) {
            for (int i = 1; i <= ruleMatcher.groupCount(); i++) {
                ruleMatched = ruleMatcher.group(i);
                switch (mode) {
                    case "assign":
                        assignList.add(ruleMatched);
                        break;
                    case "match":
                        if (!matchList.contains(ruleMatched)) matchList.add(ruleMatched);
                        out.print(ruleMatched + " - " + i + "\n");
                        break;
                    case "replace":
                        replaceList.add(ruleMatched);
                        if (Main.verbose_level == 0) out.print("Replaced");
                    case "replacementGroupNum":
                        ruleReplacementIntArr.add(ruleMatched);
                        out.print(ruleMatched + " -- " + i);
                }
            }
        }
        return ruleMatched;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static String doPatch(String projectPath) {
        File home = null;
        if (Main.arch_device == 0) home = new File(System.getProperty("user.dir")+File.separator+"patches");
        //else home = new File(Environment.getExternalStorageDirectory() + "/ApkEditor/patches");
        ArrayList<String> rulesFiles = new ArrayList<>();
        ArrayList<String> rulesFilesClean = new ArrayList<>();
        assert home != null;
        for (File file : Objects.requireNonNull(home.listFiles())){            //make load patches by its own method
            if (file.toString().endsWith(".zip")){
                rulesFiles.add(file.toString());
                rulesFilesClean.add(file.getName());
            }
        }
        out.println("\nScanning " + projectPath);
        long timeSpent;
        long startTime = System.currentTimeMillis();
        projectSmaliList = (IO.scan(projectPath));
        projectSmaliText.clear();
        for (String file : projectSmaliList) {
            projectSmaliText.add(IO.read(file));
        }
        timeSpent = System.currentTimeMillis()-startTime;
        out.println("Scan completed successfully for " + timeSpent + " ms. " + projectSmaliList.size() + " smali files found.\n");
        File tempFolder = new File(home + File.separator + "temp");
        if (!tempFolder.exists()) tempFolder.mkdir();
        for (String path : Main.select(rulesFilesClean, "Now select patch:")){
            path = rulesFiles.get(rulesFilesClean.indexOf(path));
            out.println("\nPatch - " + path);
            assignList.clear();
            matchList.clear();
            out.println("Started.");
            for (String rule :IO.loadRules(home, path)){
                String result = rulesHandler(projectPath, rule);
                if (Main.verbose_level > 2) out.print('.');
                if (result.equals("error")){
                    out.println("Probably bug detected. Trying to start patch again...");
                    return "error";
                }
            }
        }
        IO.deleteInDirectory(tempFolder);
        return "ok";
    }
}
