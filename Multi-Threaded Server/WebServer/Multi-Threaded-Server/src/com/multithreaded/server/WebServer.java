package com.multithreaded.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class WebServer extends Thread implements Closeable {

	private ExecutorService executor = Executors.newCachedThreadPool();
	private ServerSocket receiveRequestSocket;
	private static Semaphore semaphore = new Semaphore(100);
	private boolean isRunning = true;

	private int threadsExecuting = 0;

	public final Object WAIT = new Object();

	public WebServer() throws UnknownHostException, IOException {
		receiveRequestSocket = new ServerSocket(0);
		System.out.println("Server listening on port " + receiveRequestSocket.getLocalPort());
	}

	public int getPort() {
		return receiveRequestSocket.getLocalPort();
	}

	@Override
	public void run() {
		serve();
	}

	private static Object writeLock = new Object();

	public void serve() {
		while (isRunning) {
			try {
				Socket socket = receiveRequestSocket.accept();
				executor.execute(() -> {
					ThreadStarted();
					try {
						semaphore.acquire();
						synchronized (writeLock) {
							System.out.print(
									"Started {Thread ID: " + Thread.currentThread().getId() + "} {Time: " + System.currentTimeMillis() + "} ");
							if (!(Thread.currentThread().getName() == null || Thread.currentThread().getName().isEmpty())) {
								System.out.print("{Thread Name: " + Thread.currentThread().getName() + "} ");
							}
							System.out.println();
						}
						RequestProcessor rp = new RequestProcessor(socket);
						rp.run();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						semaphore.release();
						synchronized (writeLock) {
							System.out.print(
									"Stopped {Thread ID: " + Thread.currentThread().getId() + "} {Time: " + System.currentTimeMillis() + "} ");
							if (!(Thread.currentThread().getName() == null || Thread.currentThread().getName().isEmpty())) {
								System.out.print("{Thread Name: " + Thread.currentThread().getName() + "} ");
							}
							System.out.println();
						}
						synchronized (this) {
							threadsExecuting--;
							if (threadsExecuting == 0) {
								synchronized (WAIT) {
									WAIT.notify();
								}
							}
						}
					}
				});
			} catch (IOException e) {
			}
		}
	}

	private synchronized void ThreadStarted() {
		threadsExecuting++;
	}

	@Override
	public void close() throws IOException {
		while (threadsExecuting != 0) {
			try {
				synchronized (WAIT) {
					WAIT.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isRunning = false;
		receiveRequestSocket.close();
	}
}
