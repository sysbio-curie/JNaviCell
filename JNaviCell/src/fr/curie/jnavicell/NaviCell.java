package fr.curie.jnavicell;

import java.awt.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.http.NameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Java binding for NaviCell REST API.
 * 
 * @author eb
 *
 */
@SuppressWarnings("unchecked")
public class NaviCell {

	private String map_url = "https://navicell.curie.fr/navicell/maps/cellcycle/master/index.php";
	private String proxy_url = "https://navicell.curie.fr/cgi-bin/nv_proxy.php";
	private int msg_id = 1000;
	private Set<String> hugo_list;
	private String session_id = "";
	
	/**
	 * Constructor
	 */
	NaviCell() {
		// nothing to be done here.
	}

	/**
	 * Set the map_url address (NaviCell or ACSN map).
	 * 
	 * @param url string: valid NaviCell map web address.
	 */
	public void setMapUrl(String url) {
		map_url = url;
	}
	
	public void setProxyUrl (String url) {
		proxy_url = url;
	}
	
	public Set<String> getHugoList() {
		return(hugo_list);
	}

	public String getMapUrl() {
		return(map_url);
	}
	
	public String getProxyUrl() {
		return(proxy_url);
	}
	
	public String getSessionId () {
		return(session_id);
	}
	
	

	/* ------------------------------------------------------------------------
	 * Session and utility functions.
	 * ------------------------------------------------------------------------
	 */
	
	
	/**
	 * Encode URL for NaviCell server.
	 * 
	 * @param module (String)
	 * @param action (String)
	 * @param args list of objects for the 'args' array
	 * @return UrlEncodedFormEntity url
	 */
	private UrlEncodedFormEntity buildUrl(String module, String action, ArrayList<Object> args) {
		
		UrlEncodedFormEntity url = null;
		
		// encode command string
		JSONArray ja = new JSONArray();
		for (Object obj : args)
			ja.add(obj);
		JSONObject jo = new JSONObject();
		jo.put("module", module);
		jo.put("args", ja);
		jo.put("msg_id", msg_id);
		jo.put("action", action);
		String str_data = "@COMMAND " + jo.toJSONString();
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("id", session_id));
		params.add(new BasicNameValuePair("mode", "cli2srv"));
		params.add(new BasicNameValuePair("perform", "send_and_rcv"));
		params.add(new BasicNameValuePair("data", str_data));
		System.out.println(params);
		
		try {
			url = new UrlEncodedFormEntity(params, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return url;
	}
	
	/**
	 * Build a HttpClient object trusting any SSL certificate.
	 * 
	 * @return HttpClient object
	 */
	@SuppressWarnings("deprecation")
	private HttpClient buildHttpClient() {
		HttpClientBuilder b = HttpClientBuilder.create();

		SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
					return true;
				}
			}).build();
			b.setSslcontext(sslContext);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// or SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
		//HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", PlainConnectionSocketFactory.getSocketFactory())
		        .register("https", sslSocketFactory)
		        .build();

		// allows multi-threaded use
		PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		b.setConnectionManager(connMgr);

		HttpClient client = b.build();
		
		return client;
	}

	/**
	 * Send a POST request to the NaviCell server.
	 * 
	 * @param url
	 * @return String server response
	 */
	private String sendToServer(UrlEncodedFormEntity url) {
		String ret = "";
		try {
			//System.out.println(EntityUtils.toString(url));
			HttpPost httppost = new HttpPost(getProxyUrl());
			httppost.setEntity(url);
			HttpClient client = buildHttpClient();
			HttpResponse response = client.execute(httppost); 
			//System.out.println(response);
			ret = EntityUtils.toString(response.getEntity());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	/**
	 * Generate a valid session ID.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	private void generateSessionID() throws UnsupportedEncodingException {
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(4);
		params.add(new BasicNameValuePair("id", "1"));
		params.add(new BasicNameValuePair("perform", "genid"));
		params.add(new BasicNameValuePair("msg_id", Integer.toString(msg_id)));
		params.add(new BasicNameValuePair("mode", "session"));
		UrlEncodedFormEntity myUrl = null;
		myUrl = new UrlEncodedFormEntity(params, "UTF-8");
		
		String ID = sendToServer(myUrl);
		session_id = ID;
	}
	
	private void increaseMessageID() {
		msg_id++;
	}
	
	private void waitForReady(String module) {
		while (serverIsReady(module) == false) {
			try {
				//System.out.println("waiting for server..");
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns true if NaviCell server is ready.
	 * 
	 * @return boolean
	 */
	public boolean serverIsReady(String module) {
		boolean ret = false;
		increaseMessageID();

		UrlEncodedFormEntity url = buildUrl(module, "nv_is_ready", new ArrayList<Object>());
		if (url != null) {
			String rep = sendToServer(url);
			JSONParser parser = new JSONParser();
			JSONObject o;
			try {
				o = (JSONObject) parser.parse(rep);
				if (o.get("data").toString().equals("true"))
					ret = true;
			} catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
			}
		}
		
		return ret;
	}
	
	
	/**
	 * Launch a browser session with the current ID and map URL.
	 */
	public void launchBrowser() {
		increaseMessageID();
		try {
			if (session_id == "")
				generateSessionID();
			String url = map_url + "?id=" + session_id;
			java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
			waitForReady("");
		}
		catch (java.io.IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	
	/* ------------------------------------------------------------------------
	 * Navigation and zooming functions.
	 * ------------------------------------------------------------------------
	 */
	
	
	/**
	 * Set the zoom level on the current map.
	 * 
	 * @param module
	 * @param zoomLevel
	 */
	public void setZoom(String module, int zoomLevel) {
		
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_set_zoom", new ArrayList<Object>(Arrays.asList(zoomLevel)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set the relative position of the map center. 
	 * @param module
	 * @param location = 'MAP_CENTER' or 'MAP_EAST' or 'MAP_SOUTH' or MAP_NORTH' or 'MAP_SOUTH_WEST' or 'MAP_SOUTH_EAST' or 'MAP_NORTH_EAST'.
	 */
	public void setMapCenter(String module, String location) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_set_center", new ArrayList<Object>(Arrays.asList(location)));
		if (url != null) {
			sendToServer(url);
		}
	}

	/**
	 * Set the absolute position of the map center.
	 * 
	 * @param module
	 * @param x x-coordinate (integer)
	 * @param y y-coordinate (integer)
	 */
	public void setMapCenterAbsolute(String module, int x, int y) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_set_center", new ArrayList<Object>(Arrays.asList("ABSOLUTE", x, y)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Move the map center (relative).
	 * 
	 * @param module
	 * @param x relative x-coordinate
	 * @param y relative y-coordinate
	 */
	public void moveMapCenter(String module, int x, int y) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_set_center", new ArrayList<Object>(Arrays.asList("RELATIVE", x, y)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	
	/* ------------------------------------------------------------------------
	 * Entity selection functions.
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Find and select an entity on the map.
	 * 
	 * @param module
	 * @param entity entity name (String)
	 */
	public void selectEntity(String module, String entity) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_find_entities", new ArrayList<Object>(Arrays.asList(entity)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Find one or more entities on the map according to a pattern (e.g. AKT*).
	 * 
	 * @param module
	 * @param pattern entity's name pattern (String)
	 * @param bubble display the bubble (boolean)
	 */
	public void findEntities(String module, String pattern, boolean bubble) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_find_entities", new ArrayList<Object>(Arrays.asList(pattern, bubble)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Uncheck all entities on the map.
	 * 
	 * @param module
	 */
	public void uncheckEntities(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_uncheck_all_entities", new ArrayList<Object>());
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Un-highlight all entities on the map.
	 * 
	 * @param module
	 */
	public void unhighlightAllEntities(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_unhighlight_all_entities", new ArrayList<Object>());
		if (url != null) {
			sendToServer(url);
		}
	}

	// for testing purpose
	public static void main(String[] args) {
		NaviCell n = new NaviCell();
		n.launchBrowser();
		n.findEntities("", "AT*", false);
		n.unhighlightAllEntities("");
		
//		try {
//			n.generateSessionID();
//			n.launchBrowser();
//			Thread.sleep(4000);
//			n.setZoom("", 4);
//		
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
//		JSONObject obj=new JSONObject();
//		obj.put("data", 10);
//		System.out.println(obj);
//		JSONArray l = new JSONArray();
//		obj.put("test", l);
//		System.out.println(obj);
		
//		NaviCell n = new NaviCell();
//
//		//HttpClient httpclient = HttpClients.createDefault();
//		HttpClient httpclient = n.buildHttpClient();
//		HttpPost httppost = new HttpPost(n.getProxyUrl());
//
//		// Request parameters and other properties.
//		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(4);
//		params.add(new BasicNameValuePair("id", "1"));
//		params.add(new BasicNameValuePair("perform", "genid"));
//		params.add(new BasicNameValuePair("msg_id", "1001"));
//		params.add(new BasicNameValuePair("mode", "session"));
//		try {
//			UrlEncodedFormEntity myUrl = new UrlEncodedFormEntity(params, "UTF-8");
//			System.out.println(EntityUtils.toString(myUrl));
//			httppost.setEntity(myUrl);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		//Execute and get the response.
//		HttpResponse response = null;
//		try {
//			response = httpclient.execute(httppost);
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		HttpEntity entity = response.getEntity();
//		try {
//			System.out.println(EntityUtils.toString(entity));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
	}
	
}
