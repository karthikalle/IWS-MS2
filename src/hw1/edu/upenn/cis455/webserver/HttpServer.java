package edu.upenn.cis455.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import TestHarness.MyContainer;
import TestHarness.MyServletContext;

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
public class HttpServer {
	static int port;
	static String root;
	ServerSocket servSock;
	static String pathToWebXML;
	public static MyServletContext servletContext;
	Logger log;

	public static void main(String args[]) throws Exception {

		//Check if two arguments are entered in command line
		if(args.length<3){
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
		log = Logger.getLogger(HttpServer.class.getName());
		FileHandler fh = null;
		try {
			fh = new FileHandler("./errorlogs/error.log");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}  
        log.addHandler(fh);  
        //logger.setLevel(Level.ALL);  
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);  
        //log.info("Error log");  
		log.setLevel(Level.WARNING);
	}

	//Start the server
	private void startServer() throws Exception {
		servletContext = null;
		try {
			servSock = new ServerSocket(port, 2000);
			System.out.println("Server Started");
			Socket sock = null;
			MyContainer container = new MyContainer();

			if(!container.initialize(pathToWebXML, log)){
				System.err.print("Invalid path to web.xml");
				return;
			}
			
				ThreadPool t = new ThreadPool(root,this,port,container, log);

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
				log.warning("Exception while starting the server: Stopped the server"+"\n");
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
				log.warning("Exception while closing the socket"+"\n");

			}
		}
	}
