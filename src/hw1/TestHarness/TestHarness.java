package TestHarness;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Todd J. Green, modified by Nick Taylor
 */
public class TestHarness {	
	HashMap<String, Object> requestParams = new HashMap<String,Object>();
	Socket s;

	static class Handler extends DefaultHandler {
		private int m_state = 0;
		private String m_servletName;
		private String m_paramName;
		HashMap<String,String> m_servlets = new HashMap<String,String>();
		HashMap<String,String> m_contextParams = new HashMap<String,String>();
		HashMap<String,HashMap<String,String>> m_servletParams = new HashMap<String,HashMap<String,String>>();

		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if (qName.compareTo("servlet-name") == 0) {
				m_state = 1;
			} else if (qName.compareTo("servlet-class") == 0) {
				m_state = 2;
			} else if (qName.compareTo("context-param") == 0) {
				m_state = 3;
			} else if (qName.compareTo("init-param") == 0) {
				m_state = 4;
			} else if (qName.compareTo("param-name") == 0) {
				m_state = (m_state == 3) ? 10 : 20;//If 3 then context, else servlet config
			} else if (qName.compareTo("param-value") == 0) {
				m_state = (m_state == 10) ? 11 : 21;//If param value, then value 11 for context, or 21 for config
			}
		}

		public void characters(char[] ch, int start, int length) {
			String value = new String(ch, start, length);
			if (m_state == 1) {
				m_servletName = value;
				m_state = 0;
			} else if (m_state == 2) {
				m_servlets.put(m_servletName, value);
				m_state = 0;
			} else if (m_state == 10 || m_state == 20) {
				m_paramName = value;
			} else if (m_state == 11) {
				if (m_paramName == null) {
					System.err.println("Context parameter value '" + value + "' without name");
					System.exit(-1);
				}
				m_contextParams.put(m_paramName, value);
				m_paramName = null;
				m_state = 0;
			} else if (m_state == 21) {
				if (m_paramName == null) {
					System.err.println("Servlet parameter value '" + value + "' without name");
					System.exit(-1);
				}
				HashMap<String,String> p = m_servletParams.get(m_servletName);
				if (p == null) {
					p = new HashMap<String,String>();
					m_servletParams.put(m_servletName, p);
				}
				p.put(m_paramName, value);
				m_paramName = null;
				m_state = 0;
			}

		}
		public void printEverything() {
			System.out.println("Printing Servlets:");
			for(String servlet:m_servlets.keySet()){
				System.out.println(servlet+" "+m_servlets.get(servlet));
			}
			System.out.println("\n");
			System.out.println("Printing Servlet COntext:");
			for(String servlet:m_contextParams.keySet()){
				System.out.println(servlet+" "+m_contextParams.get(servlet));
			}
			System.out.println("\n");

			System.out.println("Printing Servlet Config:");
			for(String servlet:m_servletParams.keySet()){
				System.out.println(servlet+" "+m_servletParams.get(servlet));
			}
			System.out.println("\n");

			return;
		}

	}

	private static Handler parseWebdotxml(String webdotxml) throws Exception {
		Handler h = new Handler();
		File file = new File(webdotxml);
		if (file.exists() == false) {
			System.err.println("error: cannot find " + file.getPath());
			System.exit(-1);
		}
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		parser.parse(file, h);

		return h;
	}

	private static MyServletContext createContext(Handler h) {
		MyServletContext fc = new MyServletContext();
		for (String param : h.m_contextParams.keySet()) {
			fc.setInitParam(param, h.m_contextParams.get(param));
		}
		return fc;
	}

	private static HashMap<String,HttpServlet> createServlets(Handler h, MyServletContext fc) throws Exception {
		HashMap<String,HttpServlet> servlets = new HashMap<String,HttpServlet>();
		for (String servletName : h.m_servlets.keySet()) {
			MyServletConfig config = new MyServletConfig(servletName, fc);
			String className = h.m_servlets.get(servletName);
			Class servletClass = Class.forName(className);
			HttpServlet servlet = (HttpServlet) servletClass.newInstance();
			HashMap<String,String> servletParams = h.m_servletParams.get(servletName);
			if (servletParams != null) {
				for (String param : servletParams.keySet()) {
					config.setInitParam(param, servletParams.get(param));
				}
			}
			servlet.init(config);
			servlets.put(servletName, servlet);
		}
		return servlets;
	}

	private static void usage() {
		System.err.println("usage: java TestHarness <path to web.xml> " 
				+ "[<GET|POST> <servlet?params> ...]");
	}

	public boolean doWork(String[] args,Socket sock, HashMap<String,Object> att) throws Exception {
		requestParams = att;
		s = sock;
		if (args.length < 3 || args.length % 2 == 0) {
			usage();
			System.exit(-1);
		}

		Handler h = parseWebdotxml(args[0]);
		MyServletContext context = createContext(h);

		HashMap<String,HttpServlet> servlets = createServlets(h, context);

		MySession fs = null;


		for (int i = 1; i < args.length - 1; i += 2) {
			MyRequest request = new MyRequest(fs);
			MyResponse response = new MyResponse(this);

			parseRequest(sock.getInputStream(),request);
			//
			//	response.addServSock(sock);
			//

			System.out.println(args[i+1]+ "  "+i);
			String[] strings = args[i+1].split("\\?|&|=");

			/*
			 * Added for getting Servlet Name 
			 * and path info
			 * and requestURL
			 */
			String[] requestURL = args[i+1].split("[?]");
			request.setAttribute("requestURL", requestURL[0]);
			String totalPath = strings[0];
			String[] urlpath = totalPath.split("/");
			HttpServlet servlet = servlets.get(urlpath[0]);

			if(urlpath.length>=2)
				request.setAttribute("path-info", totalPath.substring(urlpath[0].length()));
			//System.out.println("servlet-name "+urlpath[0]);
			//System.out.println("path-info "+request.getAttribute("path-info"));
			if(strings.length>=2)
				request.setAttribute("query-string", args[i+1].substring(totalPath.length()+1));
			//	System.out.println("query-string "+request.getAttribute("query-string"));
			//	System.out.println("requestURL "+request.getAttribute("requestURL"));
			//Till here


			//There is no servlet for that request
			if (servlet == null) {
				System.err.println("error: cannot find mapping for servlet " + strings[0]);
				//System.exit(-1);
				return false;
			}
			for (int j = 1; j < strings.length - 1; j += 2) {
				System.out.println(strings[j]+" "+strings[j+1]);
				request.setParameter(strings[j], strings[j+1]);
			}

			if (args[i].compareTo("GET") == 0 || args[i].compareTo("POST") == 0) {
				request.setMethod(args[i]);
				servlet.service(request, response);

				if(!response.isCommitted()) {
					writeHeader(response);
					
					writeBody(response);
					
				}

			} else {
				System.err.println("error: expecting 'GET' or 'POST', not '" + args[i] + "'");
				usage();
				return false;
				//System.exit(-1);
			}
			//h.printEverything();
			fs = (MySession) request.getSession(false);		
		}
		return true;
	}

	public void writeBody(MyResponse response) throws IOException {
		OutputStream out = s.getOutputStream();
		System.out.println("Response Body:");
		BufferedOutputStream o = new BufferedOutputStream(out);
		PrintWriter pw = new PrintWriter(o,true);
		pw.print(response.buffer.toString());
		pw.flush();
	}

	public void writeHeader(MyResponse response)
			throws IOException {
		OutputStream out = s.getOutputStream();
		System.out.println("Response headers:");
		if(response.statuscodes.isEmpty()) {
		System.out.println(requestParams.get("requestVersion")+" 200 OK\r\n");
		out.write((requestParams.get("requestVersion")+" 200 OK\r\n").getBytes());       
		}
		else {
			Set<Integer> key = response.statuscodes.keySet();
			Iterator<Integer> a = key.iterator();
			int k = a.next();
			out.write((requestParams.get("requestVersion")+" "+k+response.statuscodes.get(k)+"\r\n").getBytes());
			System.out.println((requestParams.get("requestVersion")+" "+k+" "+response.statuscodes.get(k)+"\r\n"));

		}
		for(String k: response.m_props.keySet()) {
			System.out.print(k+":"+response.m_props.get(k)+"\r\n");
			out.write((k+":"+response.m_props.get(k)+"\r\n").getBytes());
		}
		out.write("\r\n".getBytes());
		response.isCommitted = true;
		out.flush();

	}

	private void parseRequest(InputStream input, MyRequest request) {
		System.out.println("inside printing params");
		System.out.println("Printing parameters");
		for(String p:requestParams.keySet()) {
			request.setAttribute(p, requestParams.get(p).toString());
			System.out.println(p+" "+requestParams.get(p));
		}

	}


}

