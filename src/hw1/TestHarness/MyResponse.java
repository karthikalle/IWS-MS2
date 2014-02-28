package TestHarness;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.channels.IllegalSelectorException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * @author tjgreen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * 
 * https://piazza.com/class/hniyh1tao4bpq?cid=218
 */
public class MyResponse implements HttpServletResponse {

	HashMap<Integer,String> status = new HashMap<Integer,String>();

	public HashMap<String,Object> m_props;
	HashMap<Integer,String> statuscodes = new HashMap<Integer,String>();
	PrintWriter pw;
	StringWriter buffer;
	int bufferSize;
	boolean isCommitted;
	Socket s;
	TestHarness container;

	public MyResponse(TestHarness t) {
		m_props = new HashMap<String,Object>();
		pw = null;
		bufferSize = 4096;
		isCommitted = false;
		buffer = null;
		container = t;
		status.put(100, "Continue");
		status.put(200, "OK");
		status.put(204, "No Content");
		status.put(301, "Moved Permanently");
		status.put(304, "Not Modified");
		status.put(400, "Bad Request");
		status.put(401, "Unauthorized");
		status.put(403, "Forbidden");
		status.put(404, "Not Found");
		status.put(408, "Request Timeout");
		status.put(500, "Internal Server Error");
	}

	public void addCookie(Cookie arg0) {

		if(!m_props.containsKey("Set-Cookie")){
			m_props.put("Set-Cookie", arg0.getName()+"="+arg0.getValue()+"; "+"Expires"+"="+getExpirationDate(arg0));
			return;
		}
		String c = (String) m_props.get("Set-Cookie");
		//String a = c.substring(0,c.indexOf("Expires="));
		//System.out.println(a);
		m_props.put("Set-Cookie", c+"\nSet-Cookie:"+arg0.getName()+"="+arg0.getValue()+"; "+"Expires"+"="+getExpirationDate(arg0));
	}

	public String getExpirationDate(Cookie arg0) {
		Calendar now = Calendar.getInstance(); 
		SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US); 
		df.setTimeZone(TimeZone.getTimeZone("GMT")); 
		String date1 = df.format(now.getTimeInMillis()+arg0.getMaxAge());
		String currentDate = date1;
		return currentDate;
	}

	public boolean containsHeader(String arg0) {
		return m_props.containsKey(arg0);
	}

	//Add session id to the end
	public String encodeURL(String arg0) {
		if(m_props.get("session-id")!=null)
			return arg0+";jsessionid="+m_props.get("session-id");
		return arg0;
	}

	public String encodeRedirectURL(String arg0) {
		return arg0;
	}

	public String encodeUrl(String arg0) {
		return arg0;
	}

	public String encodeRedirectUrl(String arg0) {
		return arg0;
	}

	public void sendError(int arg0, String arg1) throws IOException {
		if(isCommitted())
			throw new IllegalStateException();
		else {
			statuscodes.clear();
			statuscodes.put(arg0, arg1);
		}
	}

	public void sendError(int arg0) throws IOException {
		if(isCommitted())
			throw new IllegalStateException();
		else
			statuscodes.put(arg0, "");
	}

	public void sendRedirect(String arg0) throws IOException {
		System.out.println("[DEBUG] redirect to " + arg0 + " requested");
		System.out.println("[DEBUG] stack trace: ");
		Exception e = new Exception();
		StackTraceElement[] frames = e.getStackTrace();
		for (int i = 0; i < frames.length; i++) {
			System.out.print("[DEBUG]   ");
			System.out.println(frames[i].toString());
		}
	}

	public void setDateHeader(String arg0, long arg1) {
		m_props.put(arg0, arg1);
	}

	public void addDateHeader(String arg0, long arg1) {
		String result = (String) m_props.get(arg0);
		m_props.put(arg0,result+","+arg1);
	}

	public void setHeader(String arg0,String arg1) {
		m_props.put(arg0, arg1);		
	}

	public void addHeader(String arg0, String arg1) {
		if(m_props.get(arg0)!=null)	{
			String result = (String) m_props.get(arg0);
			m_props.put(arg0,result+","+arg1);
		}
		else
			m_props.put(arg0, arg1);		

	}

	public void setIntHeader(String arg0, int arg1) {
		m_props.put(arg0, arg1);		
	}

	public void addIntHeader(String arg0, int arg1) {
		String result = (String) m_props.get(arg0);
		m_props.put(arg0,result+","+arg1);
	}

	public void setStatus(int arg0) {
		statuscodes.put(arg0, status.get(arg0));
	}

	public void setStatus(int arg0, String arg1) {
		statuscodes.put(arg0, arg1);
	}

	public String getCharacterEncoding() {
		System.out.println("here");
		if(m_props.get("Character-Encoding")==null)
			return "ISO-8859-1";
		return (String) m_props.get("Character-Encoding");
	}

	public String getContentType() {
		String contentType = (String) m_props.get("Content-Type");
		if(contentType == null)
			return null;
		return contentType;
	}

	//Not Required
	public ServletOutputStream getOutputStream() throws IOException {
		return null;
	}

	/*
	 * Get printwriter for sock.getOutputStream.
	 * output = sock.getOutputStream();
	 * PrintStream out = new PrintStream(new BufferedOutputStream(output));
	 */
	public PrintWriter getWriter() throws IOException {
		buffer = new StringWriter(bufferSize);
		pw = new PrintWriter(buffer,false);
		return pw;
	}

	public void setCharacterEncoding(String arg0) {
		m_props.put("Character-Encoding",arg0);
	}

	public void setContentLength(int arg0) {
		m_props.put("Content-Length", arg0);
	}

	public void setContentType(String arg0) {
		m_props.put("Content-Type", arg0);
	}

	/*
	 * Container shud assign the buffer size - as large as required by the servlet
	 * reuse a set of fixed size buffers
	 * call before any content is written using ServletOutputStream or Writer
	 */
	public void setBufferSize(int arg0) {
		bufferSize = arg0;
	}


	public int getBufferSize() {
		return bufferSize;
	}

	public void flushBuffer() throws IOException {
		container.writeHeader(this);
		container.writeBody(this);
		isCommitted = true;
	}

	public void resetBuffer() {
		if(!isCommitted()){
			buffer.getBuffer().setLength(0);
			System.out.println("Buffer"+buffer);
			return;
		}
		throw new IllegalStateException();		
	}

	/*
	 * isCommitted method returns a boolean value 
	 * indicating whether any response bytes have been returned to the client. 
	 */
	public boolean isCommitted() {
		return isCommitted;
	}

	public void reset() {
		if(!isCommitted()){
			buffer.getBuffer().setLength(0);
			m_props.clear();
			statuscodes.clear();
			System.out.println("Buffer"+buffer);
			return;
		}
		throw new IllegalStateException();		 
	}

	public void setLocale(Locale arg0) {
		m_props.put("Locale",arg0);
	}

	public Locale getLocale() {
		return (Locale) m_props.get("Locale");
	}

}
