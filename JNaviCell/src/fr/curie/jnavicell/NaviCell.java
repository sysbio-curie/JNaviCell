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
import org.apache.http.util.EntityUtils;
import org.apache.http.NameValuePair;

/**
 * Java binding for NaviCell REST API.
 * 
 * @author eb
 *
 */
public class NaviCell {

	private String map_url = "https://navicell.curie.fr/navicell/maps/cellcycle/master/index.php";
	private String proxy_url = "https://navicell.curie.fr/cgi-bin/nv_proxy.php";
	private int msg_id = 1000;
	private Set<String> hugo_list;
	private String session_id ="";
	
	NaviCell() {
		// nothing to be done here.
	}

	/**
	 * Set the map_url address (NaviCell or ACSN map).
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
			b.setSslcontext( sslContext);
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

	public String sendToServer(UrlEncodedFormEntity url) {
		String ret = "";
		try {
			HttpPost httppost = new HttpPost(getProxyUrl());
			httppost.setEntity(url);
			
			HttpClient client = buildHttpClient();
			
			HttpResponse response = client.execute(httppost); 
			ret = EntityUtils.toString(response.getEntity());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static void main(String[] args) {
		
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(4);
		params.add(new BasicNameValuePair("id", "1"));
		params.add(new BasicNameValuePair("perform", "genid"));
		params.add(new BasicNameValuePair("msg_id", "1001"));
		params.add(new BasicNameValuePair("mode", "session"));
		UrlEncodedFormEntity myUrl = null;
		try {
			myUrl = new UrlEncodedFormEntity(params, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		NaviCell n = new NaviCell();
		
		String ret = n.sendToServer(myUrl);
		System.out.println(ret);
		
		
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
