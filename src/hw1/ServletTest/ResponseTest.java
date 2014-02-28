package ServletTest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import TestHarness.MyResponse;
import TestHarness.TestHarness;
import static org.junit.Assert.*;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;

public class ResponseTest {
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
/*
	@Test
	public void testAddCookie() {
		Cookie c = new Cookie("1","1");
		c.setMaxAge(3600);
		MyResponse r = new MyResponse(new TestHarness());
		r.addCookie(c);
		Cookie c2 = new Cookie("2","2");
		c2.setMaxAge(3500);
		r.addCookie(c2);
		Cookie c3 = new Cookie("2","2");
		c3.setMaxAge(2500);
		r.addCookie(c3);
		System.out.println(r.m_props.get("Set-Cookie"));
	}
*/
	@Test
	public void testreset() {
		TestHarness t = new TestHarness();
		String args[] = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
			args[2] = "cookie1";
		try {
			t.doWork(args,sock,null);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		MyResponse r = new MyResponse(t);
		r.setStatus(200," OK");
		
		r.reset();
		try {
			r.flushBuffer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
