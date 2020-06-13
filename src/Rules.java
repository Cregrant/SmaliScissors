import java.io.File;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

import static java.lang.System.out;

class Rules {

    static boolean replace(String rule, String patMatch, String ruleTarget) {
        String patReplacement = "REPLACE:\\n([\\S\\s]*?)\\n?\\[\\/MATCH_REPLACE]";
        String patReplacementInt = "REPLACE:\\n[\\S\\s]*?(\\$\\{GROUP\\d\\})";
        String ruleMatch = Regex.match(patMatch, rule, "");
        if (!Regex.assignList.isEmpty() && !Regex.matchList.isEmpty()) {
            for (int i = 0; i < Regex.assignList.size(); ) {
                if (Regex.assignList.size()!=Regex.matchList.size()){
                    out.println("Error in ASSIGN rule!!!");
                    out.println("assignList - " + Regex.assignList);
                    out.println("matchList - " + Regex.matchList);
                    System.exit(1);
                }
                ruleMatch = ruleMatch.replace("${" + Regex.assignList.get(i) + "}", Regex.matchList.get(i));
                i++;
            }
        }
        String ruleReplacement = Regex.match(patReplacement, rule, "");
        Regex.match(patReplacementInt, rule, "replacementGroupNum");
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
            for (String str; j < Regex.projectSmaliText.size();) {                //refactor this method
                str = Regex.projectSmaliText.get(j);
                if (Regex.projectSmaliList.get(j).contains(ruleTarget) && str.matches(ruleMatch)) {
                    Regex.match(ruleMatch, str, "replace");
                    if (!Regex.ruleReplacementIntArr.isEmpty()) {
                        out.print("click\n");
                        for (int i = 0; i < Regex.replaceList.size(); ) {
                            ruleReplacement = ruleReplacement.replaceAll(Regex.ruleReplacementIntArr.get(i), Regex.replaceList.get(i));
                            i++;
                        }
                    }
                    Regex.projectSmaliText.set(j, str.replaceAll(ruleMatch, ruleReplacement));

                    if (Main.verbose_level == 0){
                        if (clone != j){                                          //suppress clones
                            out.println(Regex.projectSmaliList.get(j) + " patched.");
                            clone = j;
                            c++;
                        }
                    }
                    Regex.replaceList.clear();
                    j--;
                }
                j++;
            }
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
            out.println("You can try again. It was (assign rule bug) or (your rule error). Sorry =(");
            return false;
        }
        if (Main.verbose_level <= 1) out.println(c + " files patched.");

        for (j = 0; j < Regex.projectSmaliList.size(); j++) {
            if (!Regex.projectSmaliText.get(j).equals(Regex.projectSmaliTextOriginal.get(j))) {
                IO.write(Regex.projectSmaliList.get(j), Regex.projectSmaliText.get(j));
            }
        }
        return true;
    }

    static void assign(String patMatch, String ruleTarget, String rule){
        String patAssign = "\\n(.+?)=\\$\\{GROUP\\d\\}";
        String ruleMatch =  Regex.match(patMatch, rule, "");
        if (Main.verbose_level == 0) out.println("Match - " + ruleMatch);
        Regex.match(patAssign, rule, "assign");
        for (String text : Regex.projectSmaliList){
            if (text.contains(ruleTarget)){
                String content = Regex.projectSmaliText.get(Regex.projectSmaliList.indexOf(text));
                Regex.match(ruleMatch, content, "match");
            }
        }
        if (!Regex.assignList.isEmpty() && !Regex.matchList.isEmpty()) {
            for (int i = 0; i < Regex.assignList.size(); ) {
                if (Main.verbose_level <= 1) out.println("Assign added: " + Regex.assignList.get(i) + " - " + Regex.matchList.get(i));
                i++;
            }
        }
        else {
            out.println("Nothing found in assign rule.");
        }

    }

    static void add(String projectPath, String rule, String ruleTarget) {
        String patSource = "SOURCE:\\n(.+)";
        String ruleSource = Regex.match(patSource, rule, "");
        if (Main.verbose_level == 0) out.println("Source - " + ruleSource);
        String src = System.getProperty("user.dir")+ File.separator+"patches"+File.separator+"temp"+File.separator+ruleSource;
        String dst = projectPath+File.separator+ruleTarget;
        File file = new File(dst);
        if (file.exists()) IO.deleteInDirectory(file);
        IO.copy(src, dst);
        if (ruleSource.contains(".smali")){
            if (Main.verbose_level <= 1) out.println("Added.");
            Regex.projectSmaliList.add(projectPath+File.separator+ruleTarget);
            Regex.projectSmaliText.add(IO.read(projectPath+File.separator+ruleTarget));
        }
    }

    static void remove(String projectPath, String ruleTarget) {
        String dst = projectPath+ File.separator+ruleTarget;
        if (Main.verbose_level == 0) out.println("Dst - " + dst);
        IO.deleteInDirectory(new File(dst));
        ArrayList<String> temp = Regex.projectSmaliList;
        int i=0;
        while (i < temp.size()) {
            if (Regex.projectSmaliList.get(i).contains(ruleTarget)) {
                Regex.projectSmaliText.remove(i);
                Regex.projectSmaliList.remove(i);
            }
            i++;
        }
        if (Main.verbose_level <= 1 && !ruleTarget.contains("/temp")) out.println(ruleTarget + " removed.");
    }
}
