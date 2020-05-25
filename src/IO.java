import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;

public class IO {
    static ArrayList<String> loadRules(File home, String path){
        Pattern patRule = Pattern.compile("(\\[.+?])(?:\\nSOURCE:\\n.+)?\\nTARGET:\\n[\\s\\S]+?\\[/.+?]", Pattern.MULTILINE);
        if (Main.rules_mode == 0){
            out.println();
        }
        String content, f = "";
        ArrayList<String> rulesListArr = new ArrayList<>();
        out.println("Loading rules...");
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(path))) {
            ZipEntry entry;        //Текущий файл
            while ((entry = zip.getNextEntry()) != null) {
                FileOutputStream fout = new FileOutputStream(home + File.separator + "temp" + File.separator + entry.getName());
                for (int c = zip.read(); c != -1; c = zip.read()) {
                    fout.write(c);
                }
                fout.flush();
                zip.closeEntry();
                fout.close();
            }
            f = home + File.separator + "temp" + File.separator + "patch.txt";
            File fil = new File(f);
            if (!fil.exists()) {
                out.println("No patch.txt file in patch!");
                System.exit(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        content = IO.read(f);
        Matcher ruleMatched = patRule.matcher(content);
        while (ruleMatched.find()) {
            for (int i = 0; i < ruleMatched.groupCount(); i++) {
                String s = ruleMatched.group(i);
                rulesListArr.add(s);
            }
        }
        out.println(rulesListArr.size() + " rules found\n");
        return rulesListArr;
    }

    public static String read(String path) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[8192];
            int length;
            assert inputStream != null;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            inputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString(UTF_8);
    }

    public static void write(String path, String content)
    {
        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path));
            bufferedWriter.write(content);
            bufferedWriter.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public static void deleteInDirectory(File directoryFile) {
        try {
            for (File dir : Objects.requireNonNull(directoryFile.listFiles())){
                Path directory = Paths.get(dir.toString());
                Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        Files.delete(path);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException {
                        Files.delete(directory);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            out.println("Error deleting temp folders");
        }
    }

    static ArrayList<String> scan(String projectPath) {
        ArrayList<String> filesList = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        File projectPathFile = new File(projectPath);
        for (String i : Objects.requireNonNull(projectPathFile.list())) {
            if (i.startsWith("smali")) {
                stack.push(projectPath + File.separator + i);
            }
        }
        while (!stack.isEmpty()) {
            File ObDir = new File(stack.pop());
            for (File i : Objects.requireNonNull(ObDir.listFiles())) {
                if (i.isDirectory()) stack.push(i.toString());
                else filesList.add(i.toString());
            }
        }
        if (filesList.size() == 0) out.println("No smali folders provided?");
        return filesList;
    }
}