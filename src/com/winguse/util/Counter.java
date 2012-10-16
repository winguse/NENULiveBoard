package com.winguse.util;

public class Counter {
	int count = 0;

	public void add() {
		synchronized (this) {
			count++;
		}
	}

	public void decrese() {
		synchronized (this) {
			count--;
		}
	}

	public int get() {
		synchronized (this) {
			return count;
		}
	}
}
