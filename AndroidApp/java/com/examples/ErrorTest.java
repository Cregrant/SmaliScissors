package com.examples;

import static com.examples.util.Print.*;

@SuppressWarnings("serial")
class FuuuuuuException extends RuntimeException {
	public FuuuuuuException(String message) {
		super(message);
	}
}

public class ErrorTest {
	public static void main(String[] args) {
		print("Oooops!\n");
		throw new FuuuuuuException("!!!TROLLFACE!!!");
	}
}

