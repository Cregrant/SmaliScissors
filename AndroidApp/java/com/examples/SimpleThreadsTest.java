package com.examples;

import static com.examples.util.Print.*;

public class SimpleThreadsTest {
	public static void main(String[] args) throws InterruptedException {
		print("[1] started.");
		Thread t = new Thread() {
			private double d;
			
			@Override
			public void run() {
				print("[2] started.");
				
				// Long-running operation:
				final long COUNT = 10000000L;
				final long MARKER = COUNT / 10;
				for (long i = 1; i < COUNT; i++) {
					d += (Math.PI + Math.E) / (double) i;
					if (i % MARKER == 0)
						printnb(".");
				}
				
				print("\n[2] done.");
			}
		};
		t.start();
		print("[1] waiting for [2] to finish...");
		t.join();
		print("[1] done.");
	}
}
