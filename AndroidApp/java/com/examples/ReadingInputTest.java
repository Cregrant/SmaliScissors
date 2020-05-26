package com.examples;

import static com.examples.util.Print.*;
import java.io.*;
import java.util.concurrent.*;

public class ReadingInputTest {
	private static final String NEW_LINE =
		System.getProperty("line.separator").toString();
	private static BufferedReader mReader = 
		new BufferedReader(new InputStreamReader(System.in));
	
	public static void main(String[] args) throws Exception {
		print("Enter some text...");
		String name = mReader.readLine();
		print("You've entered: " + name + NEW_LINE);
		
		print("Now wait 5 seconds...");
		TimeUnit.SECONDS.sleep(5);
		
		print("Again enter some text...");
		name = mReader.readLine();
		print("You've entered: " + name + NEW_LINE);
		
		print("And the last one...");
		int n = -1;
		while ((n = System.in.read()) != -1)
			print((char)n);
		
		print("done.");
	}
}

