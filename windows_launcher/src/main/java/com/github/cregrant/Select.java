package com.github.cregrant;

import java.util.ArrayList;
import java.util.Scanner;

public class Select {

    static public ArrayList<String> select(ArrayList<String> stringsList, String msg, String msgError) {
        Main.out.println(msg);
        if (stringsList.size() == 0) {
            Main.out.println(msgError);
            System.exit(1);
        } else if (stringsList.size() == 1) {
            return stringsList;
        }
        for (String i : stringsList) {
            Main.out.println(stringsList.indexOf(i) + " - " + i);
        }
        ArrayList<String> outArr = new ArrayList<>();
        while (outArr.isEmpty()) {
            outArr = getInput(stringsList);
        }
        return outArr;
    }

    static private ArrayList<String> getInput(ArrayList<String> stringsList) {
        Scanner br = new Scanner(System.in);
        ArrayList<String> outArr = new ArrayList<>();
        while (outArr.size() == 0) {
            String inputString = br.nextLine();
            if (inputString.isEmpty()) {
                return stringsList;
            }
            if (inputString.equals("X") | inputString.equals("x") | inputString.equals("х") | inputString.equals("Х")) {
                outArr.add("cancel");
                return outArr;
            }
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