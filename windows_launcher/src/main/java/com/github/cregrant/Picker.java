package com.github.cregrant;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Picker {
    ArrayList<String> strings;
    String selectMsg;

    public Picker(ArrayList<String> strings, String selectMsg) {
        this.strings = strings;
        this.selectMsg = selectMsg;
    }

    public ArrayList<String> getChoice() {
        ArrayList<String> result = new ArrayList<>();
        if (strings.size() == 0) {
            Main.out.println("Error: no choices available.");
            return result;
        } else if (strings.size() == 1) {
            result = strings;
        } else {
            Main.out.println(selectMsg);
            for (int i = 0; i < strings.size(); i++) {
                String choice = strings.get(i);
                File file = new File(choice);
                Main.out.println(i + " - " + file.getName());
            }
            while (result.isEmpty()) {
                result = parseInput(strings);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String s : result)
            sb.append(s).append("\n");
        Main.out.println("\nSelected:\n" + sb);
        return result;
    }

    private ArrayList<String> parseInput(ArrayList<String> stringsList) {
        Scanner br = new Scanner(System.in);
        ArrayList<String> outArr = new ArrayList<>();
        while (outArr.size() == 0) {
            String inputString = br.nextLine();
            if (inputString == null)
                continue;

            if (inputString.isEmpty())
                return stringsList;

            if (inputString.equals("X") || inputString.equals("x") || inputString.equals("х") || inputString.equals("Х"))
                return outArr;      //empty

            for (String o : inputString.split(" ")) {
                try {
                    outArr.add(stringsList.get(Integer.parseInt(o)));
                }
                catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    Main.out.println("Nope... Try again:");
                }
            }
        }
        return outArr;
    }
}