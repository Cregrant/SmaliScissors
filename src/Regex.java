import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.lang.System.out;

public class Regex {
    static ArrayList<String> matchList = new ArrayList<>();
    static ArrayList<String> assignList = new ArrayList<>();
    static ArrayList<String> replacementList = new ArrayList<>();
    static ArrayList<String> projectSmaliList = new ArrayList<>();
    static ArrayList<String> projectSmaliText = new ArrayList<>();
    //static ArrayList<String> projectSmaliListModified = new ArrayList<>();

    public static void rulesHandler(String projectPath, String rule){
        String patDetect = "\\[(.+?)][\\S\\s]*?\\[/.+?]";
        String patTarget = "TARGET:\\n(.+)";
        String patSource = "SOURCE:\\n(.+)";
        String patAssign = "\\n(.+?)=\\$\\{GROUP\\d}";
        String patMatch = "MATCH:\\n(.+)";
        String patReplacement = "REPLACE:\\n([\\S\\s]*?)\\n?\\[\\/MATCH_REPLACE]";
        String patReplacementInt = "REPLACE:\\n[\\S\\s]*?\\{GROUP(\\d)}";

        if (Main.rules_mode == 0){
            //other patterns
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
                    if (Main.verbose_level == 0) out.println("Assign added: " + assignList.get(i) + " - " + matchList.get(i));
                    i++;
                }
            }
            else {
                out.println("Nothing found. You can try again (java bug). Sorry =(");
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
                        System.exit(0);
                    }
                    ruleMatch = ruleMatch.replaceAll("\\$\\{" + assignList.get(i) + "}", matchList.get(i));
                    i++;
                }
            }
            String ruleReplacement = match(patReplacement, rule, "");
            String ruleReplacementStr = match(patReplacementInt, rule, "");
            int ruleReplacementInt = 0;
            if (!ruleReplacementStr.isEmpty()) ruleReplacementInt = Integer.parseInt(ruleReplacementStr);
            if (Main.verbose_level == 0) {
                out.println("To replace - " + ruleMatch);
                out.println("Replacement - " + ruleReplacement);
            }
            int c = 0;
            int j = 0;
            try{
                for (String str; j < projectSmaliText.size();) {                //fix dat fucking assign
                    str = projectSmaliText.get(j);
                    if (str.matches("[\\S\\s]*?(?:" + ruleMatch + ")[\\S\\s]*?")) {
                        match(patMatch, rule, "replace");
                        for (int i = 0; i < replacementList.size(); ) {
                            ruleReplacement = ruleReplacement.replaceAll("\\$\\{GROUP" + ruleReplacementInt + "}", replacementList.get(i));
                            i++;
                            ruleReplacementInt++;
                        }
                        if (Main.verbose_level == 0) out.println(projectSmaliList.get(j) + " patched.");
                        //projectSmaliListModified.add(projectSmaliList.get(j));
                        //projectSmaliText.set(j, str.replaceAll(ruleMatch, ruleReplacement));
                        IO.write(projectSmaliList.get(j), str.replaceAll(ruleMatch, ruleReplacement));
                        c++;
                        replacementList.clear();
                    }
                    j++;
                }
            } catch (PatternSyntaxException e) {
                out.println("You can try again. It was (assign rule bug) or (your rule error). Sorry =(");
                System.exit(1);
            }
            if (Main.verbose_level == 0) out.println(c + " files patched total.");
        }

        if (ruleType.equals("ADD_FILES")){
            String ruleSource = match(patSource, rule, "");
            if (Main.verbose_level == 0) out.println("Source - " + ruleSource);
            Path src = Paths.get(System.getProperty("user.dir")+File.separator+"patches"+File.separator+"temp"+File.separator+ruleSource);
            Path dst = Paths.get(projectPath+File.separator+ruleTarget);
            try {
                if (Files.exists(dst)) Files.delete(dst);
                Files.copy(src, dst);
                if (ruleSource.contains(".smali")){
                    if (Main.verbose_level == 0) out.println("Added.");
                    projectSmaliList.add(projectPath+File.separator+ruleTarget);
                    projectSmaliText.add(IO.read(projectPath+File.separator+ruleTarget));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (ruleType.equals("REMOVE_FILES")){
            Path dst = Paths.get(projectPath+File.separator+ruleTarget);
            if (Main.verbose_level == 0) out.println("Dst - "+dst);
            try {
                Files.delete(dst);
                ArrayList<String> temp = projectSmaliList;
                int i=0;
                while (i <= temp.size()) {
                    if (projectSmaliList.get(i).contains(ruleTarget)) {
                        projectSmaliText.remove(i);
                        projectSmaliList.remove(i);
                    }
                    i++;
                }

                if (Main.verbose_level == 0) out.println("Removed.");
            } catch (IOException ignored) {
                out.println("Nothing to remove...");
            }
        }
        out.println();
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
                            break;
                        case "replace":
                            replacementList.add(ruleMatched);
                    }
                }
            }
        return ruleMatched;
    }

    static void doPatch(String projectPath) {
        File home = new File(System.getProperty("user.dir")+File.separator+"patches");
        ArrayList<String> rulesFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(home.listFiles())){
            if (file.toString().endsWith(".zip")) rulesFiles.add(file.toString());
        }
        out.println("\nScanning " + projectPath);
        long startTime = System.currentTimeMillis();
        projectSmaliList = (IO.scan(projectPath));
        projectSmaliText.clear();
        for (String file : projectSmaliList) {
            projectSmaliText.add(IO.read(file));
        }
        long timeSpent = System.currentTimeMillis()-startTime;
        out.println("Scan completed successfully for " + timeSpent + " ms. " + projectSmaliList.size() + " smali files found.\n");
        File tempFolder = new File(home + File.separator + "temp");
        for (String path : Main.select(rulesFiles, "Now select patch:")){
            out.println("\nPatch - " + path);
            assignList.clear();
            matchList.clear();
            for (String rule :IO.loadRules(home, path)){
                rulesHandler(projectPath, rule);
                IO.deleteInDirectory(tempFolder);

            }
            //for (String str : projectSmaliListModified) {
            //    IO.write(str, projectSmaliText.get(projectSmaliList.indexOf(str)));
            //}
            //out.println("Changes wrote");
        }
    }
}
