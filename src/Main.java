import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;

import static java.lang.System.out;

public class Main {
    static int rules_mode = 1;    //0=TruePatcher, 1=AE
    static int verbose_level = 0;
    static int arch_device = 0;

    public static void main(String[] args) {
        loadConf();
        final String arch = System.getProperty("os.arch");
        String MainDirPath = "";
        ArrayList<String> projectsList = new ArrayList<>();
        assert arch != null;
        if (arch.equals("amd64") || arch.equals("x86") || arch.equals("i386") || arch.equals("ppc")) {
            MainDirPath = "C:\\BAT\\_INPUT_APK";                 // путь
            //MainFrame.startFrame();
        }
        else if (arch.contains("aarch")){
            //MainDirPath = Environment.getExternalStorageDirectory() + "/ApkEditor/decoded";
            projectsList.add(MainDirPath);
            arch_device = 1;
        }

        File MainDir = new File(MainDirPath);
        if (!MainDir.isDirectory()) {
            out.println("Error loading projects folder");
            System.exit(0);
        }
        if (projectsList.isEmpty()){
            for (String MainDirFolder : Objects.requireNonNull(MainDir.list())) {      //убираем апк файлы
                File f = new File(MainDirPath + File.separator + MainDirFolder);
                if (f.isDirectory()) projectsList.add(MainDirFolder);
            }
        }
        String msg = "Select project. Enter = all. Example: 0 or 012 (means 0 and 1 and 2).";
        long startTime = 0;
        for (String projectPath : select(projectsList, msg)){
            startTime = System.currentTimeMillis();
            if (arch_device == 0) projectPath = MainDirPath + File.separator + projectPath;      //путь до проекта
            else projectPath = MainDirPath;
            if (Regex.doPatch(projectPath).equals("error")) Regex.doPatch(projectPath);
        }
        long timeSpent = System.currentTimeMillis() - startTime;
        out.println("All done in " + timeSpent + " ms");
        saveConf();
    }

    static ArrayList<String> select(ArrayList<String> stringsList, String msg) {
        out.println(msg);
        for (String i : stringsList) {
            out.println(stringsList.indexOf(i) + " - " + i);
        }
        Scanner br = new Scanner(System.in);
        StringBuilder name = new StringBuilder(br.nextLine());
        if (name.toString().equals("")){                       //uncomment for release
            for (String str : stringsList) name.append(stringsList.indexOf(str));
        }
        if (name.length() == 0) {
            out.println("No way...");
            System.exit(0);
        }
        ArrayList<String> out = new ArrayList<>();
        for (String o : name.toString().split("")) {
            out.add(stringsList.get(Integer.parseInt(o)));
        }

        return out;
    }

    private static void loadConf() {
        Properties props = new Properties();
        final String settingsFilename = System.getProperty("user.dir") + File.separator + "config" + File.separator + "conf.txt";
        out.println(settingsFilename);
        try {
            FileInputStream input = new FileInputStream(settingsFilename);
            props.load(input);
            input.close();
        } catch(Exception e){
            out.println("Error loading conf!");
            e.printStackTrace();
        }

        try {
            verbose_level = Integer.parseInt(props.getProperty("verbose_level"));

        } catch(NumberFormatException e) {
            out.println("Error reading conf! ");
        }
    }

    private static void saveConf() {
        try {
            final String settingsFilename = System.getProperty("user.dir") + File.separator + "config" + File.separator + "conf.txt";
            FileOutputStream output = new FileOutputStream(settingsFilename);
            Properties props = new Properties();
            props.store(output, "");
            output.close();
            out.println("Conf wrote");
        }
        catch(Exception ignore) {
            out.println("Error writing conf");
        }
    }
}
