package TestHarness;

/*
 * have to implement
 * getContext()
 * getServlets()
 * 
 */


import javax.servlet.*;

import java.util.*;

/**
 * @author Nick Taylor
 */
public class MyServletContext implements ServletContext {
	private HashMap<String,Object> attributes;
	private HashMap<String,String> initParams;
	
	public MyServletContext() {
		attributes = new HashMap<String,Object>();
		initParams = new HashMap<String,String>();
	}
	
	/*Returns the servlet container attribute with the given name, 
	*or null if there is no attribute by that name.
	*/
	public Object getAttribute(String name) {
		return attributes.get(name);
	}
	
	public Enumeration getAttributeNames() {
		Set<String> keys = attributes.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}
	
	//Confused about the spec, where is the Context in the URL? Is it in the file system?
	public ServletContext getContext(String name) {
		if(!name.startsWith("/"))
			return null;
		
		return null;
	}
	
	
	public String getInitParameter(String name) {
		return initParams.get(name);
	}
	
	public Enumeration getInitParameterNames() {
		Set<String> keys = initParams.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}
	
	public int getMajorVersion() {
		return 2;
	}
	
	//Not Required
	public String getMimeType(String file) {
		return null;
	}
	
	public int getMinorVersion() {
		return 4;
	}

	//Not Required
	public RequestDispatcher getNamedDispatcher(String name) {
		return null;
	}
	
	public String getRealPath(String path) {
		return null;
	}
	
	public RequestDispatcher getRequestDispatcher(String name) {
		if(!name.startsWith("/"))
			return null;
		
		try {
			return (RequestDispatcher) Class.forName(name).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//Not Required
	public java.net.URL getResource(String path) {
		return null;
	}
	
	//Not Required
	public java.io.InputStream getResourceAsStream(String path) {
		return null;
	}
	
	//Not Required
	public java.util.Set getResourcePaths(String path) {
		return null;
	}
	
	public String getServerInfo() {
		return initParams.get("display-name");
	}
	
	public Servlet getServlet(String name) {
		try {
			return (Servlet)Class.forName(name).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * Returns the name of this web application corresponding to this ServletContext 
	 * as specified in the deployment descriptor for this web application 
	 * by the display-name element.
	 */
	public String getServletContextName() {
		return (String) attributes.get("display-name");
	}
	
	public Enumeration getServletNames() {
		return null;
	}
	
	public Enumeration getServlets() {
		return null;
	}
	
	//Not Required
	public void log(Exception exception, String msg) {
		log(msg, (Throwable) exception);
	}
	
	//Not Required
	public void log(String msg) {
		System.err.println(msg);
	}
	
	//Not required
	public void log(String message, Throwable throwable) {
		return;
		/*System.err.println(message);
		throwable.printStackTrace(System.err);
		*/
	}
	
	public void removeAttribute(String name) {
		attributes.remove(name);
	}
	
	public void setAttribute(String name, Object object) {		
		//If a null value is passed, the effect is the same as calling removeAttribute()
		//http://docs.oracle.com/javaee/6/api/javax/servlet/ServletContext.html#setAttribute(java.lang.String, java.lang.Object)
		if(object==null)
			removeAttribute(name);
		else
			attributes.put(name, object);
	}
	
	public void setInitParam(String name, String value) {
		initParams.put(name, value);
	}
}
