package TestHarness;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * @author Todd J. Green
 */
class MySession implements HttpSession {

	private Properties m_props = new Properties();
	private boolean m_valid = true;
	
	public MySession() {
		m_props.setProperty("Creation-Time", Long.toString(System.currentTimeMillis()));
	}
	
	public long getCreationTime() {
		return (Long)m_props.get("Creation-Time");
	}

	public String getId() {
		return m_props.getProperty("id");
	}

	public long getLastAccessedTime() {
		return (Long)m_props.get("Last-Accessed");
	}


	public ServletContext getServletContext() {
		return (ServletContext)m_props.get("Servlet-Context");
	}

	public void setMaxInactiveInterval(int arg0) {
		m_props.setProperty("Max-Inactive-Interval", Integer.toString(arg0));
	}

	public int getMaxInactiveInterval() {
		return (Integer)m_props.get("Max-Inactive-Interval");
	}

	public HttpSessionContext getSessionContext() {
		return null;
	}

	public Object getAttribute(String arg0) {
		return m_props.get(arg0);
	}

	public Object getValue(String arg0) {
		return m_props.get(arg0);
	}

	public Enumeration getAttributeNames() {
		return m_props.keys();
	}


	public String[] getValueNames() {
		Enumeration<Object> s = m_props.keys();
		String st[] = null;
		for(int i = 0; s.hasMoreElements(); i++){
			st[i] = (String) s.nextElement();
		}
		return st;
	}

	public void setAttribute(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	public void putValue(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		m_props.remove(arg0);
	}

	public void removeValue(String arg0) {
		m_props.remove(arg0);
	}

	public void invalidate() {
		m_valid = false;
	}

	public boolean isNew() {
		return false;
	}

	boolean isValid() {
		return m_valid;
	}
}
