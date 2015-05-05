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
	
	public void setZoom(String module, int zoomLevel) {
		
		increaseMessageID();
		JSONArray a = new JSONArray();
		a.add(new Integer(zoomLevel));
		JSONObject jo = new JSONObject();
		jo.put("module", module);
		jo.put("args", a);
		jo.put("msg_id", msg_id);
		jo.put("action", "nv_set_zoom");
		
		String str_data = "@COMMAND " + jo.toJSONString();
		System.out.println(str_data);
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("id", session_id));
		params.add(new BasicNameValuePair("mode", "cli2srv"));
		params.add(new BasicNameValuePair("perform", "send_and_rcv"));
		params.add(new BasicNameValuePair("data", str_data));
		System.out.println(params);
		
		UrlEncodedFormEntity url = null;
		try {
			url = new UrlEncodedFormEntity(params, "UTF-8");
			String rep = sendToServer(url);
			System.out.println(rep);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
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
	

	/**
	 * Returns true if NaviCell server is ready.
	 * 
	 * @return boolean
	 */
	public boolean serverIsReady() {
		boolean ret = false;
		increaseMessageID();
		
		JSONObject jo = new JSONObject();
		jo.put("module", "");
		jo.put("args", new JSONArray());
		jo.put("msg_id", msg_id);
		jo.put("action", "nv_is_ready");
		
		String str_data = "@COMMAND " + jo.toJSONString();
		//System.out.println(str_data);
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("id", session_id));
		params.add(new BasicNameValuePair("mode", "cli2srv"));
		params.add(new BasicNameValuePair("perform", "send_and_rcv"));
		params.add(new BasicNameValuePair("data", str_data));
		
		UrlEncodedFormEntity url = null;
		try {
			url = new UrlEncodedFormEntity(params, "UTF-8");
			String rep = sendToServer(url);
			// normal answer: {"status":0,"msg_id":1001,"data":true}
			JSONParser parser = new JSONParser();
			JSONObject o = (JSONObject) parser.parse(rep);
			if (o.get("data").equals("true"))
				ret = true;
			
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
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
		}
		catch (java.io.IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	// for testing purpose
	public static void main(String[] args) {
		NaviCell n = new NaviCell();
		try {
			n.generateSessionID();
			n.launchBrowser();
			Thread.sleep(4000);
			n.setZoom("", 4);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
