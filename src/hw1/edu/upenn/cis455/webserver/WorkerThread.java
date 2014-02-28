package edu.upenn.cis455.webserver;

import TestHarness.MyContainer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/*
 * Worker threads:
 * 1. Wait for the queue to get a request 
 * 2. Parses the input
 * 3. Validate if it is inside root, if outside, send forbidden request
 * 4. If request is shutdown, send shutdown
 * 5. If request is control, send control panel
 * 6. If request is directory, send directory files
 * 7. If request is a file, read corresponding request and send them
 * 8. Send corresponding request headers
 * 
 */
public class WorkerThread extends Thread {

	RequestQueue queue;
	Socket sock;
	String root;
	String request;
	String fileRequest;
	ThreadPool threadPool; 
	boolean isRunning;
	Logger log;
	InputStream input;
	OutputStream output;
	String requestVersion;
	String pathToWebXML;
	int port;
	HashMap<String,Object> att;
	MyContainer container;

	WorkerThread(RequestQueue q, String r, ThreadPool tp, int p, MyContainer c, Logger l) {
		root = r;
		queue = q;
		threadPool = tp;
		isRunning = true;
		input = null;
		requestVersion = null;
		output = null;
		port = p;
		log = l;
		container = c;

	}

	@Override
	public void run() {

		log.info("Thead id: "+Thread.currentThread().getId()+" is waiting to process a request");
		while(isRunning) {
			try {
				input = null;
				requestVersion = null;
				output = null;
				fileRequest = "";
				sock = null;
				att = new HashMap<String,Object>();
				//Get a request from the queue
				sock = queue.pop((int) Thread.currentThread().getId());

				//Start Processing the request;
				processRequest(sock);
				sock.close();
			}

			catch(IOException e) {
				log.warning("Inside Process Request"+e.toString());
			}
			catch (InterruptedException e) {
				log.info("Thread :"+Thread.currentThread().getId()+" has been interrupted");
				Thread.currentThread().interrupt();
				isRunning = false;
			}
			finally {
				try {
					if(sock!=null){
						if(!sock.isClosed())
							sock.close();
					}
				} catch (IOException e) {
					log.warning("Socket closing in finally:"+e.toString());
				}
			} 
		}
	}

	private void processRequest(Socket sock) {
		try {
			log.info("Thread "+Thread.currentThread().getId()+" started processing a request");
			input = sock.getInputStream();
			output = sock.getOutputStream();

			//Parse the input first and get the parameters
			if(parseInput()) {

				//If Request is null, return
				if(fileRequest.equals(null)||fileRequest.equals("/favicon.ico"))
					return;

				//If request is to servlet
				if(ifRequestToServlet()){
					return;
				}
				//Validate the Path if it is inside the root
				if(validatePath()) {

					//Perform respective actions based on request		
					if(fileRequest.substring(1).equals("shutdown")) {
						performShutDown();
						return;
					}
					else if(fileRequest.substring(1).equals("control")) {
						sendControlPanel();
						sock.close();
						return;
					}					
					else if(fileRequest.substring(1).equals("errorlog")) {
						sendErrorLog();
						sock.close();
						return;
					}		
					//Get the file or else
					else{					
						getFile();
					}
				}
				// If request is for a file outside root
				else
					sendForbidden();
			}
		}
		catch(Exception e) {
			log.warning("Exception in processRequest():"+e.toString());
			sendServerError();
		}
	}

	private void sendErrorLog() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader("./error.log"));
			String line = null;
			output.write((requestVersion+" 200 OK\r\n").getBytes());                           
			output.write(("Date: "+new Date()+"\r\n").getBytes());
			output.write(("Connection: close\r\n").getBytes());
			output.write(("Content-Type: "+"text/html"+"\r\n\r\n").getBytes());
			output.write(("<html><body><p>").getBytes());

			while ((line = reader.readLine()) != null) {
				output.write((line+"\n").getBytes());
			}
			output.write("</p></body></html>".getBytes());
		} catch (IOException e) {
			log.warning("Error while getting error log:"+e);
			e.printStackTrace();
		}
	}

	private boolean ifRequestToServlet() {
		String args[] = new String[3];
		args[0] = container.pathToWebXML;
		args[1] = request;
		System.out.println("File request"+fileRequest);
		if(fileRequest.equals("/"))
			return false;
		String[] fileURL = fileRequest.split("/");
		args[2] = fileURL[1];

		try {
			return container.doWork(args,sock,att);		
		} catch (Exception e) {
			log.warning("Exception in Servlet Container "+e);
			e.printStackTrace();
		}
		return false;
	}

	private boolean parseInput() {
		BufferedReader br = new BufferedReader(new InputStreamReader(input));

		String str;
		try {
			str = br.readLine();
			if(str==null)
				return false;
			StringTokenizer tokens = new StringTokenizer(str," ");
			request = tokens.nextToken();

			/*If request is neither GET or HEAD
			 * no support for PUT or POST
			 */
			if(!(request.equals("GET")||request.equals("HEAD")||request.equals("POST")||request.equals("TRACE")||request.endsWith("DELETE")||request.equals("PUT")))
			{
				sendBadRequest();
				return false;
			}
			fileRequest = "";

			//If there is no request file
			if(!tokens.hasMoreTokens())
				return false;

			//Parse for the request file
			String nextToken = tokens.nextToken();
			do {
				fileRequest += nextToken + " ";
				nextToken = tokens.nextToken();
			} while (!(nextToken.contains("HTTP")||nextToken.contains("HTTP")));

			//Replace for spaces between file names
			fileRequest = fileRequest.replaceAll("%20"," ");
			fileRequest = fileRequest.substring(0, fileRequest.length()-1);

			//Check for HTTP Version
			requestVersion = nextToken;
			if(!(nextToken.equals("HTTP/1.1")||nextToken.equals("HTTP/1.0"))){
				sendVersionNotSupported();
				return false;		
			}		

			att.put("requestVersion", requestVersion);
			/*Check if host is given under HTTP/1.1 request
			 * else send badrequest
			 */
			if(nextToken.equals("HTTP/1.1")) {
				str = br.readLine();
				if(str == null || !str.equals("Host: localhost:"+port))
				{
					sendBadRequest();
					return false;
				}
			}

			/*
			 * Check if the request has 
			 * If-Modified-Since and 
			 * Expect: 100-Continue
			 */
			boolean expectFlag = false;
			boolean isModifiedFlag = false;
			String isModifiedDate = "";
			while(!(str=br.readLine()).equals(""))
			{
				StringTokenizer st = new StringTokenizer(str,":");
				String parameter = st.nextToken();
				att.put(parameter, str.substring(parameter.length()+2));

				if(str.contains("Expect: 100-Continue"))
					expectFlag = true;

				if((str.contains("If-Modified-Since"))) {
					isModifiedFlag = true;
					isModifiedDate = str.substring(19);
				}
				if(expectFlag&&isModifiedFlag)
					break;

			}

			//Send 100 header if there is a expected request
			if(expectFlag)
				sendExpectHeader();

			//If there is no request for date modified
			if(!isModifiedFlag)
				return true;

			//Check if date is modified and send response header only
			determineIfModified(isModifiedDate);
			br.close();
			return false;

		} catch (IOException e) {
			log.warning("Thread "+Thread.currentThread().getId()+" started processing a request");
			sendServerError();
		}
		return true;
	}

	/*
	 * Validating the path by checking
	 * if root's path is a substring of requested path
	 */
	private boolean validatePath() {
		try {
			File rootDir = new File(root);
			String rootPath = rootDir.getCanonicalPath();
			File requestDir = new File(root+"/"+fileRequest);
			String requestPath = requestDir.getCanonicalPath();
			if(requestPath.contains(rootPath)) {
				log.info("Requested path is inside root");
				return true;
			}
		}
		catch(IOException e){
			log.warning("Exception in validatePath()"+e.toString());
			sendServerError();
		}
		log.info("Requested path is outside root");
		return false;
	}

	/*
	 * Check if directory and send only the list of files
	 * Read corresponding file using a buffer and send response
	 * 
	 * We keep checking if there is an interrupt caused by request for a shutdown
	 * If it is caused, the thread will stop reading into the buffer and exits
	 * 
	 */
	private void getFile() {
		if(fileRequest.equals("/"))
			fileRequest = "";
		System.out.println("inside get file");
		System.out.println(root+fileRequest);
		try {
			try {
				if(new File(root+fileRequest).isDirectory()) {
					log.info("It is a directory");
					sendDirectoryFiles();
					return;
				}
				String contentType = getContentType();
				if(contentType == null){
					sendFileNotFound();
					return;
				}
				File f = new File(root+"/"+fileRequest);
				InputStream din = new FileInputStream(f);
				PrintStream out = new PrintStream(new BufferedOutputStream(output));
				int len = (int) f.length();      
				byte[] buf = new byte[4096];
				System.out.println(len);

				out.write((requestVersion+" 200 OK\r\n").getBytes());                           
				out.write(("Content-Length: " + len + "\r\n").getBytes()); 
				out.write(("Content-Type: "+contentType+"\r\n").getBytes());
				out.write(("Connection: close\r\n").getBytes());
				out.write(("Last-Modified: "+new Date(f.lastModified())+"\r\n\r\n").getBytes());
				if(!request.equals("HEAD"))
				{
					int in;
					while ((in = din.read(buf))>0&&isRunning) {
						out.write(buf,0,in);      
					}
					if(!isRunning) 
						log.info("Shutdown command when sending bytes of file");

				}
				out.flush();
				out.close();
				din.close();
				log.info("Sent the file!");
			}
			catch (FileNotFoundException e) {
				log.info("File not found!");
				sendFileNotFound();
			}
		} catch (IOException e) {
			log.warning("Error while accessing file:"+e);
			sendServerError();
		}
	}

	/*
	 * Determine if the file is modified
	 * since the date specified in the request
	 */
	private void determineIfModified(String isModifiedDate) {
		File f = new File(root+"/"+fileRequest);
		try {
			@SuppressWarnings("deprecation")
			Date queryDate = new Date(isModifiedDate);
			System.out.println(queryDate);
			System.out.println("1");
			System.out.println(f.lastModified());
			Date fileModifiedDate = new Date(f.lastModified());
			if(queryDate.compareTo(fileModifiedDate)>0)
			{
				System.out.println("modified");
				output.write((requestVersion+" 304 Not Modified\r\n").getBytes());
				output.write(("Date: "+fileModifiedDate).getBytes());
			}
			else {
				System.out.println("not modified");
				getFile();
			}
		} 

		catch(IOException e){
			log.warning("Exception in determineIfModified"+e);
			sendServerError();
		}

	}

	/*
	 * Ask the threadpool to perform ShutDown
	 */
	private void performShutDown() throws IOException {
		threadPool.shutDown();
		log.info("Thread "+Thread.currentThread().getId()+" received a shutdown request");
		this.isRunning = false;
	}


	/*
	 * Send corresponding Responses
	 */
	private void sendVersionNotSupported() {
		try {
			output.write((requestVersion+" 505 Forbidden Request\r\n").getBytes());                           
			output.write(("Date: "+new Date()+"\r\n").getBytes());
			output.write(("Connection: close\r\n").getBytes());

			output.write(("Content-Type: "+"text/html"+"\r\n").getBytes());
			String response = "505: Version Not Supported ";
			output.write(("Content-length: "+response.length()+"\r\n\r\n").getBytes());
			if(!request.equals("HEAD"))
				output.write(response.getBytes());
		}
		catch(Exception e) {
			log.warning(e.toString());
			sendServerError();
		}
	}

	private void sendForbidden() {
		try {
			output.write((requestVersion+" 401 Forbidden Request\r\n").getBytes());                           
			output.write(("Date: "+new Date()+"\r\n").getBytes());
			output.write(("Content-Type: "+"text/plain"+"\r\n").getBytes());
			output.write(("Connection: close\r\n").getBytes());

			String response = "401: Forbidden Request ";
			output.write(("Content-length: "+response.length()+"\r\n\r\n").getBytes());
			if(!request.equals("HEAD"))
				output.write(response.getBytes());
		}
		catch(Exception e) {
			sendServerError();
			log.warning("Exception while sending forbidden"+e.toString());
		}

	}

	private void sendDirectoryFiles() {
		try {
			if(fileRequest.equals("/"))
				fileRequest = "";
			System.out.println("File Requested:"+root+fileRequest);
			File dir = new File(root+fileRequest);
			output.write((requestVersion+" 200 OK\r\n").getBytes());                           
			output.write(("Date: "+new Date()+"\r\n").getBytes());
			output.write(("Content-Type: "+"text/html"+"\r\n").getBytes());
			output.write(("Connection: close\r\n").getBytes());

			String files = "";
			files+="<html><body>";
			output.write(("Date: "+new Date()+"\r\n").getBytes());
			if(!request.equals("HEAD")) {
				for(File f:dir.listFiles()) {
					files += f.getName()+"<br/>";
				}
				files += "</body></html>";
				output.write(("Content-length: "+files.length()+"\r\n\r\n").getBytes());
				if(!request.equals("HEAD"))
					output.write(files.getBytes());
			}
		}
		catch(IOException e) {
			log.info("Exception inside sendDirectoryFiles():"+e.toString());
			sendServerError();
		}

	}

	private void sendControlPanel() {
		try {
			StringBuffer status = new StringBuffer();

			output.write((requestVersion+" 200 OK\r\n").getBytes());                           
			output.write(("Date: "+new Date()+"\r\n").getBytes());
			output.write(("Connection: close\r\n").getBytes());

			output.write(("Content-Type: "+"text/html"+"\r\n\r\n").getBytes());
			if(!request.equals("HEAD")) {
				output.write(("<html><body><p>").getBytes());
				output.write(("Karthik Alle: kalle<br/>").getBytes());

				for(WorkerThread t: threadPool.workers) {
					if(t.getState().equals(Thread.State.RUNNABLE)) {
						output.write((t.getId() +"&nbsp;&nbsp;&nbsp;:"+t.fileRequest+"\n").getBytes());
						output.write(("<br/>").getBytes());
					}
					else {
						output.write((t.getId()+"&nbsp;&nbsp;&nbsp;:"+t.getState().toString()).getBytes());
						output.write(("<br/>").getBytes());
					}
				}
				output.write(("<html><body>"
						+"<form action=\"shutdown\" method=\"post\">"+"<button type=\"submit\" "
						+ "value=\"shutdown\">Shutdown</button></form>").getBytes());
				output.write(("<form action =\"errorlog\" method=\"post\">"+"<button type=\"submit\" "
						+ "value=\"errorlog\">Error Log</button></form>").getBytes());
				output.write(("</p></body></html>").getBytes());
			}
			status.append("\n");
			output.flush();
		}
		catch(IOException e) {
			log.warning(e.toString());
			sendServerError();
		}
	}

	private void sendFileNotFound()
			throws IOException {
		output.write((requestVersion+" 404 Not Found OK\r\n").getBytes());                           
		output.write(("Date: "+new Date()+"\r\n").getBytes());
		output.write(("Connection: close\r\n").getBytes());

		output.write(("Content-Type: "+"text/html"+"\r\n\r\n").getBytes());
		if(!request.equals("HEAD")){
			if(fileRequest.equals("/")) 
				output.write(("<html><body>"+"No file has been request"+" </body></html>").getBytes());
			else {
				output.write(("<html><body>404: "+fileRequest.substring(1)+" not found!</body></html>").getBytes());
				System.out.println("Caught File not found exception");
			}
		}
	}

	private void sendBadRequest() {
		try{
			output.write((requestVersion+" 404 Bad Request\r\n").getBytes());                           
			output.write(("Date: "+new Date()+"\r\n").getBytes());
			output.write(("Connection: close\r\n").getBytes());

			output.write(("Content-Type: "+"text/html"+"\r\n\r\n").getBytes());
			if(!request.equals("HEAD")){
				output.write(("<html><body>"+"404: Bad Request"+" </body></html>").getBytes());			
			}
		}
		catch(Exception e) {
			log.warning("Exception while sending bad request"+e);
			sendServerError();
		}
	}

	private void sendServerError() {
		try {
			output.write((requestVersion+" 500 Server Error\r\n").getBytes());
			output.write(("Date: "+new Date()+"\r\n").getBytes());
			output.write(("Connection: close\r\n").getBytes());

			output.write(("Content-Type: "+"text/html"+"\r\n\r\n").getBytes());
			if(!request.equals("HEAD"))
				output.write(("<html><body>500: Server Error</html></body>\r\n").getBytes());
		} catch (IOException e) {
			log.info(e.toString());	
		}
	}

	private void sendExpectHeader() {
		try {
			if(!requestVersion.equals("HTTP/1.1"))
				return;
			output.write((requestVersion+" 100 Continue\r\n\r\n").getBytes());
		}
		catch(IOException e){
			log.warning(e.toString());
			sendServerError();
		}
	}

	private String getContentType() {
		StringTokenizer fileType = new StringTokenizer(fileRequest,".");
		String ext=null;
		while(fileType.hasMoreTokens()) {
			ext = fileType.nextToken();
		}
		if(ext.equals("jpg") || ext.equals("jpeg"))
			return "image/jpeg";
		if(ext.equals("htm") || ext.equals("html"))
			return "text/html";
		if(ext.equals("txt"))
			return "text/plain";
		if(ext.equals("png"))
			return "image/png";
		if(ext.equals("gif"))
			return "image/gif";
		if(ext.equals("pdf"))
			return "pdf";
		if(ext.equals("css"))
			return "css";
		return null;
	}
}
