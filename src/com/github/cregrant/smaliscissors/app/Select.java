package com.github.cregrant.smaliscissors.app;

import java.util.ArrayList;
import java.util.Scanner;

class Select {
    Select() {
    }

    ArrayList<String> select(ArrayList<String> stringsList, String msg) {
        System.out.println(msg);
        if (stringsList.size() == 0) {
            System.out.println("Hmm... nothing to select??");
            System.exit(1);
        } else if (stringsList.size() == 1) {
            return stringsList;
        }
        for (String i : stringsList) {
            System.out.println(stringsList.indexOf(i) + " - " + i);
        }
        ArrayList<String> outArr = new ArrayList<>();
        while (outArr.isEmpty()) {
            outArr = this.getInput(stringsList);
        }
        return outArr;
    }

    private ArrayList<String> getInput(ArrayList<String> stringsList) {
        Scanner br = new Scanner(System.in);
        ArrayList<String> outArr = new ArrayList<>();
        while (outArr.size() == 0) {
            String inputString = br.nextLine();
            if (inputString.equals("")) {
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
                    System.out.println("Nope... Try again:");
                }
            }
        }
        return outArr;
    }
}