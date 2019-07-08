package com.multithreaded.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Program {

	public static void main(String[] args) {
		try {
			WebServer webserver = new WebServer();
			Thread thread = new Thread(() -> {
				webserver.serve();
			});
			thread.start();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))){
				while(reader.readLine().equals("stop")) {
				}
			}
 			webserver.close();
 		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
