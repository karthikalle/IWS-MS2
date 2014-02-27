package ServletTest;

import static org.junit.Assert.*;

import javax.servlet.http.Cookie;

import org.junit.Test;

import TestHarness.MyResponse;

public class CookieTest {

	@Test
	public void test() {
		MyResponse r = new MyResponse();
		Cookie c = new Cookie("1","1");
		System.out.println(r.getExpirationDate(c));
		
	}
	
	@Test
	public void test2(){
		MyResponse r = new MyResponse();
		Cookie c = new Cookie("TestCookie", "54321");
		c.setMaxAge(3600);
		r.addCookie(c);
	}

}
