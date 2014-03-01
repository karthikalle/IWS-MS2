package ServletTest;

import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis455.webserver.HttpServer;
import TestHarness.MyContainer;
import TestHarness.MyResponse;

public class CookieTest {
	Socket sock;
	MyContainer t;
	public StringWriter buffer;
	StringBuilder test;

	@Before
	//Start the server
	public void startServer() {
		Logger log = Logger.getLogger(HttpServer.class.getName());
		t = new MyContainer(){
		
		};
		String args[] = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		try {
			t.initialize(args[0], log);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testExpirationDate() {
		MyResponse r = new MyResponse(t);
		Cookie c = new Cookie("1","1");
		System.out.println(r.getExpirationDate(c));
		Calendar now = Calendar.getInstance(); 
		SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US); 
		df.setTimeZone(TimeZone.getTimeZone("GMT")); 
		String date1 = df.format(now.getTimeInMillis());	
		assertEquals(r.getExpirationDate(c),date1);

	}
	
	@Test
	public void test2(){

		MyResponse r = new MyResponse(t){
			public PrintWriter getWriter() {
				buffer = new StringWriter(4096);
				PrintWriter pw = new PrintWriter(buffer,false);
				return pw;
			}
			public Socket getSocket() {
				try {
					Socket s = new Socket("localhost",8000) {
						public OutputStream getOutputStream()
						{
							OutputStream fakeOutputStream = 
								    new ServletOutputStream() {

								        @Override
								        public void write(int b) throws IOException {
								            // do nothing. Everything written to this stream is ignored
								        }
								    };
							return fakeOutputStream;
						}
					};
					return s;

				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
			
		};
		Cookie c = new Cookie("TestCookie", "54321");
		c.setMaxAge(3600);
		r.addCookie(c);

		try {
			PrintWriter pw = r.getWriter();
			r.flushBuffer();
			System.out.println(r.buffer.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
