package cn.edu.nenu.acm.board.backend;

import java.sql.SQLException;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws InterruptedException, SQLException {
		IRunListenerControler listener = new RunsListener();
		while (true) {
			System.out.println("Trying...");
			try {
				listener.begin();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (listener.isBegined())
				break;
			System.out.println("Board begined fail, try 10s later...");
			Thread.sleep(10000);
		}
		System.out.println("Board begined, input anyting to stop:");
		Scanner in = new Scanner(System.in);
		in.next();
		System.out.print("Accepted someinput...");
		listener.terminate();
		listener=null;
	}

}
