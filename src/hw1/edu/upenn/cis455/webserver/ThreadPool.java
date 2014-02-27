package edu.upenn.cis455.webserver;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/*
 * The thread pool holds 20 worker threads
 * 
 * 1. It adds the socket requests to the queue 
 * 2. Causes shutdown of all the threads when requested
 * 3. Gives the status of all the threads
 * 
 */
public class ThreadPool {

	ArrayList<WorkerThread> workers;
	int poolSize;
	RequestQueue reqQueue;
	String root;
	String pathToWebXML;
	int intFlag;
	HttpServer httpServer;

	//Initialize the threadpool and start all the threads
	ThreadPool (String rootFolder, HttpServer http,int port, String pathtowebxml) {
		root = rootFolder;
		poolSize = 20;
		reqQueue = new RequestQueue();
		workers = new ArrayList<WorkerThread>();
		intFlag = 0;
		httpServer = http;
		pathToWebXML = pathtowebxml;

		for(int i = 0; i<poolSize; i++) {
			WorkerThread worker = new WorkerThread(reqQueue, root, this, port, pathToWebXML);
			workers.add(worker);
			worker.start();
		}
	}

	//Push the socket to the queue
	public void handleRequest(Socket s) {
		reqQueue.push(s);
	}

	public void shutDown() throws IOException {
		for(WorkerThread t: workers) {
			t.isRunning = false;
			t.interrupt();
		}

		intFlag = 1;
		httpServer.stopServer();
	}

	//Gets the status of all the threads
	public String getStatus() {
		String status="Karthik Alle: kalle\n";
		status += "ThreadID	:"+"Status";
		for(WorkerThread t: workers) {
			if(t.getState().equals("WAITING"))
				status += t.getId() +"	:"+"WAITING\n";
			else if(t.getState().equals("RUNNABLE"))
				status += t.getId() +"	:"+t.fileRequest+"\n";
		}
		return status+"\n";
	}
}

