package com.multithreaded.server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

public class UnitTests {
	private static final int Number = 1500;

	@Test
	public void SendRequest() throws UnknownHostException, IOException {
		WebServer ws = new WebServer();
		int port = ws.getPort();
		ws.start();
		for (int i = 0; i < Number; i++) {
			URL url = new URL("http://localhost:" + port + "/blah.txt");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setReadTimeout(0);
			con.setRequestMethod("GET");
			con.addRequestProperty("Content-Type", "text/plain");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String text = "";
				String line = null;
				while ((line = reader.readLine()) != null) {
					text += line + "\r\n";
				}

				con.disconnect();
				assertTrue(text.equals("ABCD\r\n"));
			} catch (IOException e) {
				fail();
			}
		}
		ws.close();

	}

	@Test
	public void Post() throws IOException, InterruptedException {
		WebServer ws = new WebServer();
		int port = ws.getPort();
		ws.start();
		for (int i = 0; i < Number; i++) {
			try (Socket socket = new Socket("localhost", port);
					BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
				bf.write("POST /b/blah.txt HTTP/1.1\r\n" + "Content-Type: text/plain\r\n\r\n" + i + "\r\n");
				bf.flush();
			}
		}

		ws.close();
	}

}
