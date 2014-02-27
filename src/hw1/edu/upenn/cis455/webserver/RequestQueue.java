package edu.upenn.cis455.webserver;

import java.net.Socket;
import java.util.Queue;
import java.util.LinkedList;

public class RequestQueue {
	Queue<Socket> queue;

	RequestQueue() {
		queue = new LinkedList<Socket>();
	}

	public void push(Socket sock) {
		synchronized(queue){
			queue.add(sock);
			queue.notify();
		}
	}

	public Socket pop(int id) throws InterruptedException {
		synchronized(queue){	
			while(queue.size() == 0)
			{
				queue.wait();
			}
			return queue.remove();
		}
	}
}
