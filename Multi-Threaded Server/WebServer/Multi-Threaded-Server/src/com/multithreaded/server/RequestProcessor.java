package com.multithreaded.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RequestProcessor implements Runnable {

	private Socket client;
	
	private static int requestsMade = 0;
	
	private static Object lock = new Object();

	public RequestProcessor(Socket socket) {
		this.client = socket;
	}

	public void run() {
		synchronized (lock) {
			requestsMade++;
			Path path = Paths.get("fileName");
			try (BufferedWriter fileWriter = Files.newBufferedWriter(path)) {
				fileWriter.write(requestsMade + "\r\n");
				fileWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		try (BufferedReader socketReader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
			String request = socketReader.readLine();
			if (request == null)
				return;
			String[] requestElements = request.split(" ");
			String method = requestElements[0];
			String requestUri = requestElements[1];
			String httpVersion = requestElements[2];

			String location = requestUri.replace('/', File.separatorChar);

			List<String> headers = new ArrayList<String>();
			extractHeaders(socketReader, headers);

			String body = extractBody(socketReader);

			switch (method) {
			case "GET":
				processGet(httpVersion, location);
				break;
			case "POST":
			case "PUT":
				processPost(httpVersion, location, body);
				break;
			}

		} catch (IOException e) {
			try (OutputStream out = client.getOutputStream()) {
				out.write(("HTTP/1.1 500 " + e.getMessage() + "\r\n").getBytes());
				out.write(("Content-Length: 0\r\n\r\n").getBytes());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String extractBody(BufferedReader socketReader) throws IOException {
		String line = null;
		String body = "";
		while (socketReader.ready() && (line = socketReader.readLine()) != null) {
			body += line + "\r\n";
		}

		return body.isEmpty() ? null : body;
	}

	private void extractHeaders(BufferedReader socketReader, List<String> headers) throws IOException {
		String header = null;
		while ((header = socketReader.readLine()) != null && !header.isEmpty()) {
			headers.add(header);
		}
	}

	private void processPost(String httpVersion, String location, String body) throws IOException {
		Path path = Paths.get("Server Folder", location);
		Files.createDirectories(path.getParent());
		try (BufferedWriter fileWriter = Files.newBufferedWriter(path);
				BufferedWriter clientResponseWriter = new BufferedWriter(
						new OutputStreamWriter(client.getOutputStream()))) {
			fileWriter.write(body);
			fileWriter.flush();
			clientResponseWriter.write(httpVersion + " 200 OK\r\nContent-Length: 0\r\n\r\n");
			clientResponseWriter.flush();
		}
	}

	private void processGet(String httpVersion, String location) throws IOException {
		Path path = Paths.get("Server Folder" , location);
		Files.createDirectories(path.getParent());
		try (BufferedReader fileReader = Files.newBufferedReader(path)) {
			String l = null;
			String responseBody = "";
			while ((l = fileReader.readLine()) != null) {
				responseBody += l + "\r\n";
			}
			byte[] responseBytes = responseBody.getBytes();
			client.getOutputStream().write((httpVersion + " 200 OK\r\n").getBytes());
			client.getOutputStream().write(("Content-Length: " + responseBytes.length + "\r\n\r\n").getBytes());
			client.getOutputStream().write(responseBytes);
		}
	}
}