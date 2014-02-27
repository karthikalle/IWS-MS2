package ServletTest;

import java.util.Enumeration;
import java.util.Iterator;

import TestHarness.MyServletContext;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ContextTest {
	MyServletContext myContext;
	@Before
	public void initialize() {
		myContext = new MyServletContext();
	}
	@Test
	public void testGetAttribute1() {
		assertNull(myContext.getAttribute("a"));
	}
	
	@Test
	public void testGetAttribute2() {
		myContext.setAttribute("a", "aaa");
		assertEquals("aaa",myContext.getAttribute("a"));
	}
	
	@Test
	public void testGetAttributeNames() {
		myContext.setAttribute("b", "bbb");
		myContext.setAttribute("c", "ccc");
		myContext.setAttribute("d", "ddd");
		Enumeration a = myContext.getAttributeNames();
		while(a.hasMoreElements()){
			String t = (String) a.nextElement();
			assertTrue(t.equals("b")||t.equals("c")||t.equals("d"));
		}
	}
}
