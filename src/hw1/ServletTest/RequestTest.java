package ServletTest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

import TestHarness.TestHarness;

public class RequestTest {

	static int port;
	static String root;
	ServerSocket servSock;
	Socket sock;
	int i =0;

	@Before
	//Start the server
	public void startServer() {
		port = 8000;
		root = "/Users/karthikalle/Desktop/testfiles";
		try {
			servSock = new ServerSocket(8000, 2000);

			System.out.println("Server Started");
			sock = null;
				sock = servSock.accept();
				i++;

				//Send the request to thread pool
				//sock.close();
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void test() throws Exception {
		TestHarness t = new TestHarness();
		String args[] = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		if (i==1)
			args[2] = "cookie1";
		if(i==2)
			args[2] = "cookie2";
		t.doWork(args,sock,null);


	}

}
