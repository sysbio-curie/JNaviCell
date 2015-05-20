package fr.curie.jnavicell;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import org.json.simple.JSONValue;
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
	private Set<String> hugo_list = new HashSet<String>();
	private JSONArray biotype_list;
	private JSONArray module_list;
	private JSONArray datatable_list;
	private JSONArray datatable_sample_list; 
	private JSONArray datatable_gene_list;
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

	public JSONArray getBiotypeList() {
		return(biotype_list);
	}
	
	public JSONArray getModuleList() {
		return(module_list);
	}

	public JSONArray getDatatableList() {
		return(datatable_list);
	}

	public JSONArray getDatatableSampleList() {
		return(datatable_sample_list);
	}

	public JSONArray getDatatableGeneList() {
		return(datatable_gene_list);
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
	 * @param module
	 */
	public void unhighlightAllEntities(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_unhighlight_all_entities", new ArrayList<Object>());
		if (url != null) {
			sendToServer(url);
		}
	}

	
	/* ------------------------------------------------------------------------
	 * Get info from maps functions.
	 * ------------------------------------------------------------------------
	 */
	
	/**
	 * Get the list of the HUGO gene symbols for the current map and set the field hugo_list.
	 * @param module
	 */
	public void getHugoList(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_get_hugo_list", new ArrayList<Object>());
		if (url != null) {
			String rep = sendToServer(url);
			JSONObject obj = (JSONObject) JSONValue.parse(rep);
			JSONArray ar = (JSONArray) obj.get("data");
			for (int i=0;i<ar.size();i++)
				hugo_list.add((String) ar.get(i));
		}
	}
	
	/**
	 * get the list of NaviCell internal data types (biotypes).
	 * @param module
	 */
	public void getBiotypes(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_get_biotype_list", new ArrayList<Object>());
		if (url != null) {
			String rep = sendToServer(url);
			JSONObject obj = (JSONObject) JSONValue.parse(rep);
			JSONArray ar = (JSONArray) obj.get("data");
			biotype_list = ar;
		}
	}

	/**
	 * Get the list of modules defined on the current map.
	 * @param module
	 */
	public void getModules(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_get_module_list", new ArrayList<Object>());
		if (url != null) {
			String rep = sendToServer(url);
			JSONObject obj = (JSONObject) JSONValue.parse(rep);
			JSONArray ar = (JSONArray) obj.get("data");
			module_list = ar;
		}
	}

	/**
	 * Get the list of imported datatables.
	 * @param module
	 */
	public void getImportedDatatables(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_get_datatable_list", new ArrayList<Object>());
		if (url != null) {
			String rep = sendToServer(url);
			JSONObject obj = (JSONObject) JSONValue.parse(rep);
			JSONArray ar = (JSONArray) obj.get("data");
			datatable_list = ar;
		}
	}

	/**
	 * Get the list of samples from all imported datatables.
	 * @param module
	 */
	public void getDatatableSamples(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_get_datatable_sample_list", new ArrayList<Object>());
		if (url != null) {
			String rep = sendToServer(url);
			JSONObject obj = (JSONObject) JSONValue.parse(rep);
			JSONArray ar = (JSONArray) obj.get("data");
			datatable_sample_list = ar;
		}
	}
	
	/**
	 * Get the list of genes from all imported datatables.
	 * @param module
	 */
	public void getDatatableGenes(String module) {

		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_get_datatable_gene_list", new ArrayList<Object>());
		if (url != null) {
			String rep = sendToServer(url);
			JSONObject obj = (JSONObject) JSONValue.parse(rep);
			JSONArray ar = (JSONArray) obj.get("data");
			datatable_gene_list = ar;
		}
	}


	/* ------------------------------------------------------------------------
	 * Data import functions.
	 * ------------------------------------------------------------------------
	 */
	

	/**
	 * Load data from a file.
	 * if select is true, only genes present on the map are kept. The filtering 
	 * is done on HUGO gene symbols (hugo_list).
	 * @param fileName Path to the file
	 * @param select Boolean true: select genes preset on the map
	 * @return NaviCell compatible string data (String)
	 */
	public String loadDataFromFile(String fileName, Boolean select) {
		
		// get hugo gene list if it's not set
		if (select == true && hugo_list.size() == 0)
			getHugoList("");
		
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(fileName)));
			String line;
			int ct=0;
			int count_genes=0;
			int count_lines=0;
			sb.append("@DATA\n");
			while((line = br.readLine()) != null) {
				if (select == true && ct>1) {
					String[] tokens = line.split("\\s|\\t");
					// select genes that are present on the map
					if (hugo_list.contains(tokens[0])) {
						sb.append(line+"\n");
						count_genes++;
					}
				}
				else {
					sb.append(line+"\n");
					count_lines++;
				}
				ct++;
			}
			br.close();
			if (select == true)
				System.out.println(count_genes + " genes selected.");
			else
				System.out.println(count_lines-1 + " samples selected.");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	/**
	 * Import data into current NaviCell session.
	 * @param module module name.
	 * @param fileName file (with complete path) name.
	 * @param biotype NaviCell data type.
	 * @param datatableName name of the datatable.
	 */
	public void importData(String module, String fileName, String biotype, String datatableName) {
		String str_data = loadDataFromFile(fileName, true);
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_import_datatables", 
				new ArrayList<Object>(Arrays.asList(biotype, datatableName, "", str_data, new JSONObject())));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Import a sample annotation file.
	 * @param module
	 * @param fileName
	 */
	public void importSampleAnnotation(String module, String fileName) {
		increaseMessageID();
		String str_data = loadDataFromFile(fileName, false);
		UrlEncodedFormEntity url = buildUrl(module, "nv_sample_annotation_perform", 
				new ArrayList<Object>(Arrays.asList("import", str_data)));
		if (url != null) {
			sendToServer(url);
		}
	}

	/* ------------------------------------------------------------------------
	 * Drawing Configuration Dialog functions.
	 * ------------------------------------------------------------------------
	 */
	
	/**
	 * Open the drawing configuration dialog.
	 * @param module
	 */
	public void drawingConfigOpen(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("open")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Close the drawing configuration dialog.
	 * @param module
	 */
	public void drawingConfigClose(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("close")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Apply modifications for drawing configuration dialog.
	 * @param module
	 */
	public void drawingConfigApply(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("apply")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Apply modifications for drawing configuration dialog and close the window.
	 * @param module
	 */
	public void drawingConfigApplyAndClose(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("apply_and_close")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Cancel modifications for drawing configuration dialog.
	 * @param module
	 */
	public void drawingConfigCancel(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("cancel")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select Heatmap in drawing configuration dialog.
	 * @param module
	 * @param check Boolean 
	 */
	public void drawingConfigSelectHeatmap(String module, Boolean check) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("select_heatmap", check)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select Barplot in drawing configuration dialog.
	 * @param module
	 * @param check Boolean 
	 */
	public void drawingConfigSelectBarplot(String module, Boolean check) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("select_barplot", check)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select Glyph in drawing configuration dialog.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 * @param check Boolean 
	 */
	public void drawingConfigSelectGlyph(String module, int glyph_num, Boolean check) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("select_glyph", glyph_num, check)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select Map Staining in drawing configuration dialog.
	 * @param module
	 * @param check Boolean 
	 */
	public void drawingConfigSelectMapStaining(String module, Boolean check) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("select_map_staining", check)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select 'Display all genes' option in drawing configuration dialog.
	 * @param module
	 */
	public void drawingConfigSelectDisplayAllGenes(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("display_all_genes")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select 'Display selected genes' option in drawing configuration dialog.
	 * @param module
	 */
	public void drawingConfigSelectDisplaySelectedGenes(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_drawing_config_perform", new ArrayList<Object>(Arrays.asList("display_selected_genes")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/* ------------------------------------------------------------------------
	 * My Data Dialog functions.
	 * ------------------------------------------------------------------------
	 */
	
	/**
	 * Open 'My Data' dialog.
	 * @param module
	 */
	public void mydataDialogOpen(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_mydata_perform", new ArrayList<Object>(Arrays.asList("open")));
		if (url != null) {
			sendToServer(url);
		}
	}
	

	/**
	 * Close 'My Data' dialog.
	 * @param module
	 */
	public void mydataDialogClose(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_mydata_perform", new ArrayList<Object>(Arrays.asList("close")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set the datatables tab active for 'My Data' dialog.
	 * @param module
	 */
	public void mydataDialogSetDatatables(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_mydata_perform", new ArrayList<Object>(Arrays.asList("select_datatables")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set the Sample tab active for 'My Data' dialog.
	 * @param module
	 */
	public void mydataDialogSetSamples(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_mydata_perform", new ArrayList<Object>(Arrays.asList("select_samples")));
		if (url != null) {
			sendToServer(url);
		}
	}

	/**
	 * Set the Gene tab active for 'My Data' dialog.
	 * @param module
	 */
	public void mydataDialogSetGenes(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_mydata_perform", new ArrayList<Object>(Arrays.asList("select_genes")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set the Groups tab active for 'My Data' dialog.
	 * @param module
	 */
	public void mydataDialogSetGroups(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_mydata_perform", new ArrayList<Object>(Arrays.asList("select_groups")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set the Module tab active for 'My Data' dialog.
	 * @param module
	 */
	public void mydataDialogSetModules(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_mydata_perform", new ArrayList<Object>(Arrays.asList("select_modules")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/* ------------------------------------------------------------------------
	 * My Data Dialog functions.
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Open the glyph editor dialog.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 */
	public void glyphEditorOpen(String module, int glyph_num) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("open", glyph_num)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Close the glyph editor dialog.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 */
	public void glyphEditorClose(String module, int glyph_num) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("close", glyph_num)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Apply changes to the glyph editor dialog.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 */
	public void glyphEditorApply(String module, int glyph_num) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("apply", glyph_num)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Apply changes to the glyph editor and close the dialog.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 */
	public void glyphEditorApplyAndClose(String module, int glyph_num) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("apply_and_close", glyph_num)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Cancel changes on the glyph editor.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 */
	public void glyphEditorCancel(String module, int glyph_num) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("cancel", glyph_num)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select a sample in the glyph editor.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 * @param sample_name sample name (String)
	 */
	public void glyphEditorSelectSample(String module, int glyph_num, String sample_name) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("select_sample", glyph_num, sample_name)));
		if (url != null) {
			System.out.println(sendToServer(url));
		}
	}
	
	/**
	 * Select datatable for glyph shape in glyph editor.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 * @param datatable_name datatable name (String)
	 */
	public void glyphEditorSelectShapeDatatable(String module, int glyph_num, String datatable_name) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("select_datatable_shape", glyph_num, datatable_name)));
		if (url != null) {
			sendToServer(url);
		}
	}

	/**
	 * Select datatable for glyph color in glyph editor.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 * @param datatable_name datatable name (String)
	 */
	public void glyphEditorSelectColorDatatable(String module, int glyph_num, String datatable_name) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("select_datatable_color", glyph_num, datatable_name)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select datatable for glyph size in glyph editor.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 * @param datatable_name datatable name (String)
	 */
	public void glyphEditorSelectSizeDatatable(String module, int glyph_num, String datatable_name) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("select_datatable_size", glyph_num, datatable_name)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set the transparency parameter in the glyph editor.
	 * @param module
	 * @param glyph_num glyph number (integer between 1 and 5)
	 * @param value transparency value (integer between 1 and 100)
	 */
	public void glyphEditorSetTransparency(String module, int glyph_num, int value) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_glyph_editor_perform", new ArrayList<Object>(Arrays.asList("set_transparency", glyph_num, value)));
		if (url != null) {
			sendToServer(url);
		}
	}

	/* ------------------------------------------------------------------------
	 * Barplot editor functions.
	 * ------------------------------------------------------------------------
	 */
	
	/**
	 * Open the barplot editor.
	 * @param module
	 */
	public void barplotEditorOpen(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("open")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Close the barplot editor.
	 * @param module
	 */
	public void barplotEditorClose(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("close")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Apply changes to the barplot editor.
	 * @param module
	 */
	public void barplotEditorApply(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("apply")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Apply changes and close the barplot editor.
	 * @param module
	 */
	public void barplotEditorApplyAndClose(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("apply_and_close")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Cancel changes in the barplot editor.
	 * @param module
	 */
	public void barplotEditorCancel(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("cancel")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select a sample or group in the barplot editor.
	 * @param module
	 * @param col_num
	 * @param sample_name
	 */
	public void barplotEditorSelectSample(String module, int col_num, String sample_name) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("select_sample", col_num, sample_name)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select a datatable in the barplot editor.
	 * @param module
	 * @param datatable_name
	 */
	public void barplotEditorSelectDatatable(String module, String datatable_name) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("select_datatable", datatable_name)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Clear all samples in the barplot editor.
	 * @param module
	 */
	public void barplotEditorClearSamples(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("clear_samples")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select all samples in the barplot editor.
	 * @param module
	 */
	public void barplotEditorSelectAllSamples(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("all_samples")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select all groups in the barplot editor.
	 * @param module
	 */
	public void barplotEditorSelectAllGroups(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("all_groups")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set the transparency parameter in the barplot editor. 
	 * @param module
	 * @param value transparency value (integer between 1 and 100)
	 */
	public void barplotEditorSetTransparency(String module, int value) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_barplot_editor_perform", new ArrayList<Object>(Arrays.asList("set_transparency", value)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/* ------------------------------------------------------------------------
	 * Heatmap editor functions.
	 * ------------------------------------------------------------------------
	 */
	
	/**
	 * Open the heatmap editor.
	 * @param module
	 */
	public void heatmapEditorOpen(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("open")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Close the heatmap editor.
	 * @param module
	 */
	public void heatmapEditorClose(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("close")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Cancel changes in the heatmap editor.
	 * @param module
	 */
	public void heatmapEditorCancel(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("cancel")));
		if (url != null) {
			sendToServer(url);
		}
	}

	
	/**
	 * Apply changes in the heatmap editor.
	 * @param module
	 */
	public void heatmapEditorApply(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("apply")));
		if (url != null) {
			sendToServer(url);
		}
	}

	/**
	 * Apply changes to the heatmap editor and close the window.
	 * @param module
	 */
	public void heatmapEditorApplyAndClose(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("apply_and_close")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select a sample in the heatmap editor.
	 * @param module
	 * @param col_num column index (integer starting from 0)
	 * @param sample_name sample name (String)
	 */
	public void heatmapEditorSelectSample(String module, int col_num, String sample_name) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("select_sample", col_num, sample_name)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select a datatable in the heatmap editor.
	 * @param module
	 * @param row_num row index (starting from 0)
	 * @param datatable_name datatable name (String)
	 */
	public void heatmapEditorSelectDatatable(String module, int row_num, String datatable_name) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("select_datatable", row_num, datatable_name)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Clear the samples in the heatmap editor.
	 * @param module
	 */
	public void heatmapEditorClearSamples(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("clear_samples")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select all the samples in the heatmap editor.
	 * @param module
	 */
	public void heatmapEditorSelectAllSamples(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("all_samples")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Select all the groups in the heatmap editpr.
	 * @param module
	 */
	public void heatmapEditorSelectAllGroups(String module) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("all_groups")));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set the transparency parameter in the heatmap editor.
	 * @param module
	 * @param value transparency value (integer between 1 and 100)
	 */
	public void heatmapEditorSetTransparency(String module, int value) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_heatmap_editor_perform", new ArrayList<Object>(Arrays.asList("set_transparency", value)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/* ------------------------------------------------------------------------
	 * Unordered discrete configuration editor functions.
	 * ------------------------------------------------------------------------
	 */
	
	/**
	 * Open unordered discrete configuration editor for a given type of parameter.
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter String, either 'shape' or 'color' or 'size'.
	 */
	public void unorderedConfigOpen(String module, String datatable_name, String datatable_parameter) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("open", datatable_name, datatable_parameter)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Open unordered discrete configuration editor for a given type of parameter. 
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter String, either 'shape' or 'color' or 'size'.
	 */
	public void unorderedConfigClose(String module, String datatable_name, String datatable_parameter) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("close", datatable_name, datatable_parameter)));
		if (url != null) {
			sendToServer(url);
		}
	}
	

	/**
	 * Cancel changes for unordered discrete configuration editor for a given type of parameter.
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter String, either 'shape' or 'color' or 'size'.
	 */
	public void unorderedConfigCancel(String module, String datatable_name, String datatable_parameter) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("cancel", datatable_name, datatable_parameter)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Apply changes to unordered discrete configuration editor for a given type of parameter.
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter String, either 'shape' or 'color' or 'size'.
	 */
	public void unorderedConfigApply(String module, String datatable_name, String datatable_parameter) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("apply", datatable_name, datatable_parameter)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Apply changes to unordered discrete configuration editor for a given type of parameter, and close the window. 
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter String, either 'shape' or 'color' or 'size'.
	 */
	public void unorderedConfigApplyAndClose(String module, String datatable_name, String datatable_parameter) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("apply_and_close", datatable_name, datatable_parameter)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Open/close advanced configuration for unordered discrete configuration editor for a given type of parameter.
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter
	 * @param check
	 */
	public void unorderedConfigSetAdvancedConfig(String module, String datatable_name, String datatable_parameter, boolean check) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("set_advanced_configuration", datatable_name, datatable_parameter, check)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * "Set discrete value for unordered discrete configuration editor for a given type of parameter.
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter A string, 'shape' or 'color' or 'size'
	 * @param sample_or_group A string, either 'sample' or 'group'
	 * @param index integer value
	 * @param value double value
	 */
	public void unorderedConfigSetDiscreteValue(String module, String datatable_name, String datatable_parameter, 
			String sample_or_group, int index, double value) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("set_discrete_value", datatable_name, datatable_parameter, 
						sample_or_group, index, value)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set color value for unordered discrete configuration editor.
	 * @param module
	 * @param datatable_name
	 * @param sample_or_group string 'sample' or 'group'
	 * @param index integer value
	 * @param color string hex code color value, e.g. 'FF0000'
	 */
	public void unorderedConfigSetDiscreteColor(String module, String datatable_name, String sample_or_group, int index, String color) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("set_discrete_color", datatable_name, "color",
						sample_or_group, index, color)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set size value for unordered discrete configuration editor. 
	 * @param module
	 * @param datatable_name
	 * @param sample_or_group string 'sample' or 'group'
	 * @param index integer value
	 * @param size integer value
	 */
	public void unorderedConfigSetDiscreteSize(String module, String datatable_name, String sample_or_group, int index, int size) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("set_discrete_size", datatable_name, "size",
						sample_or_group, index, size)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set shape value for unordered discrete configuration editor.
	 * @param module
	 * @param datatable_name
	 * @param sample_or_group string 'sample' or 'group'
	 * @param index integer value
	 * @param shape integer value
	 */
	public void unorderedConfigSetDiscreteShape(String module, String datatable_name, String sample_or_group, int index, int shape) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("set_discrete_shape", datatable_name, "shape",
						sample_or_group, index, shape)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Set condition value for unordered discrete configuration editor.
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter 'size' or 'shape' or 'color'
	 * @param sample_or_group 'sample' or 'group'
	 * @param index integer value
	 * @param condition integer value
	 */
	public void unorderedConfigSetDiscreteCondition(String module, String datatable_name, String datatable_parameter, 
			String sample_or_group, int index, int condition) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("set_discrete_cond", datatable_name, datatable_parameter, sample_or_group, index, condition)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	/**
	 * Switch to sample tab for unordered discrete configuration editor.
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter string 'size' or 'shape' or 'color'.
	 */
	public void unorderedConfigSwitchSampleTab(String module, String datatable_name, String datatable_parameter) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("switch_sample_tab", datatable_name, datatable_parameter)));
		if (url != null) {
			sendToServer(url);
		}
	}
	

	/**
	 * Switch to group tab for unordered discrete configuration editor. 
	 * @param module
	 * @param datatable_name
	 * @param datatable_parameter string, 'size' or 'shape' or 'color'
	 */
	public void unorderedConfigSwitchGroupTab(String module, String datatable_name, String datatable_parameter) {
		increaseMessageID();
		UrlEncodedFormEntity url = buildUrl(module, "nv_display_unordered_discrete_config_perform", 
				new ArrayList<Object>(Arrays.asList("switch_group_tab", datatable_name, datatable_parameter)));
		if (url != null) {
			sendToServer(url);
		}
	}
	
	
	
	// for testing purpose
	public static void main(String[] args) {
		NaviCell n = new NaviCell();
		
		n.launchBrowser();
		n.importData("", "/Users/eric/wk/RNaviCell_test/DU145_mut.txt", "Mutation data", "test");
		//n.importData("", "/Users/eric/wk/RNaviCell_test/DU145_CN.txt", "Discrete Copy number data", "test");
		//n.importData("", "/Users/eric/wk/RNaviCell_test/ovca_copynumber.txt", "Discrete Copy number data", "test");
		
		n.unorderedConfigOpen("", "test", "size");
		
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		n.unorderedConfigSetDiscreteValue("", "test", "size", "sample", 0, 8);
		
//		n.importData("", "/Users/eric/wk/RNaviCell_test/DU145_data.txt", "mRNA Expression data", "test");
//		n.heatmapEditorOpen("");
//		n.heatmapEditorSelectSample("", 0,"data");
//		//n.heatmapEditorSelectDatatable("", 0, "test");
//		n.heatmapEditorClearSamples("");
		
		
//		n.importData("", "/Users/eric/wk/RNaviCell_test/DU145_data.txt", "mRNA Expression data", "test");
//		n.glyphEditorOpen("", 1);
//		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
//		n.glyphEditorSelectSample("", 1, "data");
//		n.glyphEditorSelectColorDatatable("", 1, "test");
//		n.glyphEditorSelectSizeDatatable("", 1, "test");
//		n.glyphEditorSelectShapeDatatable("", 1, "test");
//		n.glyphEditorSetTransparency("", 1, 50);
//		n.glyphEditorApply("", 1);
		
//		n.importData("", "/Users/eric/wk/RNaviCell_test/DU145_data.txt", "mRNA Expression data", "test");
//		n.mydataDialogOpen("");
//		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
//		n.mydataDialogSetGenes("");
//		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
//		n.mydataDialogSetGroups("");
//		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
//		n.mydataDialogSetModules("");
//		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
//		n.mydataDialogSetSamples("");
		
		
		
//		n.drawingConfigOpen("");
//		try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
//		n.drawingConfigSelectDisplaySelectedGenes("");
//		try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
//		n.drawingConfigSelectDisplayAllGenes("");		
		
		//		n.importData("", "/Users/eric/wk/RNaviCell_test/ovca_expression.txt", "mRNA Expression data", "test");
//		n.importSampleAnnotation("", "/Users/eric/wk/RNaviCell_test/ovca_sampleinfo.txt");
		
//		n.getModules("");
//		n.getDatatableGenes("");
//		System.out.println(n.getDatatableGeneList());
		
		//n.importData("", "/Users/eric/wk/RNaviCell_test/ovca_expression.txt", "mRNA Expression data", "test");
		
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
