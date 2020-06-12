import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.out;

class Regex {
    static ArrayList<String> matchList = new ArrayList<>();
    static ArrayList<String> assignList = new ArrayList<>();
    static ArrayList<String> replaceList = new ArrayList<>();
    static ArrayList<String> projectSmaliList = new ArrayList<>();
    static ArrayList<String> projectSmaliText = new ArrayList<>();
    static ArrayList<String> projectSmaliTextOriginal = new ArrayList<>();
    static ArrayList<String> ruleReplacementIntArr = new ArrayList<>();


    private static String rulesHandler(String projectPath, String rule){
        String patDetect = "\\[(.+?)][\\S\\s]*?\\[/.+?]";
        String patTarget = "TARGET:\\n(.+)";
        String patMatch = "MATCH:\\n(.+)";

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

        switch (ruleType) {
            case "MATCH_ASSIGN":
                Rules.assign(patMatch, ruleTarget, rule);
                break;
            case "MATCH_REPLACE":
                if (!Rules.replace(rule, patMatch, ruleTarget)) return "error";
                break;
            case "ADD_FILES":
                Rules.add(projectPath, rule, ruleTarget);
                break;
            case "REMOVE_FILES":
                Rules.remove(projectPath, ruleTarget);
                break;
        }

        out.println();
        return "ok";
    }

    static String match(String pattern, String content, String mode) {
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
        projectSmaliText.clear();
        projectSmaliList.clear();
        IO.scan(projectPath);
        if (projectSmaliList.size() == 0) {
            out.println("No smali folders provided?");
            return "ok";
        }
        projectSmaliTextOriginal = projectSmaliText;
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
