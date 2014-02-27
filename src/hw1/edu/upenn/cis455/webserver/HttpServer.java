package edu.upenn.cis455.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * The HTTP Server accepts two arguments: 
 * the port and the root
 * 
 * The port number has to be an integer and should be greater than 1023
 * 
 * For checking If-Modified-Since, please only use the 
 * " Wed Feb 11 19:38:12 EST 2013 "
 * format as the file.lastModified() uses that format
 * 
 */
class HttpServer {
	static int port;
	static String root;
	ServerSocket servSock;
	static String pathToWebXML;
	
	public static void main(String args[]) throws IOException {

		//Check if two arguments are entered in command line
		if(args.length<2){
			System.out.println("*** Karthik Alle: kalle");
			return;
		}

		try {	
			int root = Integer.parseInt(args[0]);
			if(root<1023) {
				System.out.println("Invalid Port Number");
				return;
			}
			HttpServer server = new HttpServer(Integer.parseInt(args[0]),args[1],args[2]);
			server.startServer();
		}

		catch(NumberFormatException e) {
			System.out.println("Invalid Port number");
		}
	}

	//Initialize Server
	HttpServer(int p, String r,String pathTowebxml) {
		servSock = null;
		port = p;
		root = r;
		pathToWebXML = pathTowebxml;
	}

	//Start the server
	private void startServer() {

		try {
			servSock = new ServerSocket(8000, 2000);
			System.out.println("Server Started");
			Socket sock = null;
			ThreadPool t = new ThreadPool(root,this,port,pathToWebXML);

			//Until a shutdown request has been sent
			while (t.intFlag == 0) {
				sock = servSock.accept();
				//Send the request to thread pool
				t.handleRequest(sock);
			}
			sock.close();
		}
		catch (IOException e) {
			System.out.println("Stopped the server");
		}
	}

	/*Stop the Server when a shutdown is requested
	 * Thread pool will request for the shutdown
	 */
	public void stopServer() {
		try {
			servSock.close();
		} catch (IOException e) {
			System.out.println("Cannot close the socket");
		}
	}
}