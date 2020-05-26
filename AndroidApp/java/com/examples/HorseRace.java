// Sample taken from book: Thinking in Java / Bruce Eckel.—4th ed.
// Copyright © 2006 by Bruce Eckel, President, MindView, Inc.
package com.examples;

import static com.examples.util.Print.*;
import java.util.*;
import java.util.concurrent.*;

class Horse implements Runnable {
	private static int counter = 0;
	private final int id = counter++;
	private int strides = 0;
	private static Random rand = new Random();
	private static CyclicBarrier barrier;

	public Horse(CyclicBarrier b) {
		barrier = b;
	}

	public synchronized int getStrides() {
		return strides;
	}

	public void run() {
		try {
			while (!Thread.interrupted()) {
				synchronized (this) {
					strides += rand.nextInt(3) + 1; // Produces 0, 1 or 2
				}
				barrier.await();
			}
		} catch (InterruptedException e) {
			// A legitimate way to exit
		} catch (BrokenBarrierException e) {
			// This one we want to know about
			throw new RuntimeException(e);
		}
	}

	public String toString() {
		return "Horse " + id + " ";
	}

	public String tracks() {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < getStrides(); i++)
			s.append("*");
		s.append(id);
		return s.toString();
	}
}

public class HorseRace {
	static final int FINISH_LINE = 30;
	private List<Horse> horses = new ArrayList<Horse>();
	private ExecutorService exec = Executors.newCachedThreadPool();
	private CyclicBarrier barrier;

	public HorseRace(int nHorses, final int pause) {
		barrier = new CyclicBarrier(nHorses, new Runnable() {
			private StringBuilder fence = new StringBuilder();
			
			{
				for (int i = 0; i < FINISH_LINE; i++)
					fence.append("="); // The fence on the racetrack
			}
			
			public void run() {
				print(fence);
				for (Horse horse : horses)
					print(horse.tracks());
				for (Horse horse : horses)
					if (horse.getStrides() >= FINISH_LINE) {
						print(horse + "won!");
						exec.shutdownNow();
						return;
					}
				try {
					TimeUnit.MILLISECONDS.sleep(pause);
				} catch (InterruptedException e) {
					print("barrier-action sleep interrupted");
				}
			}
		});
		for (int i = 0; i < nHorses; i++) {
			Horse horse = new Horse(barrier);
			horses.add(horse);
			exec.execute(horse);
		}
	}

	public static void main(String[] args) {
		int nHorses = 7;
		int pause = 400;
		new HorseRace(nHorses, pause);
	}
}
