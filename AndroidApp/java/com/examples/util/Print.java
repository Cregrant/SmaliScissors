package com.examples.util;

public class Print {
	public static void print() {
		System.out.println();
	}
	
	public static void print(Object obj) {
		System.out.println(obj);
	}
	
	public static void print(String text) {
		System.out.println(text);
	}
	
	public static void print(String text, Object... args) {
		System.out.println(String.format(text, args));
	}
	
	public static void printnb(Object obj) {
		System.out.print(obj);
	}
	
	public static void printnb(String text) {
		System.out.print(text);
	}
	
	public static void printnb(String text, Object... args) {
		System.out.format(text, args);
	}
	
	public static void printError(Exception e) {
		System.err.println(String.format("Error: %s", e.getMessage()));
	}
	
	public static void printError(String text) {
		System.err.println(text);
	}
}
