package fr.curie.jnavicell;

import java.util.Set;

public class NaviCell {

	private String map_url = "https://navicell.curie.fr/navicell/maps/cellcycle/master/index.php";
	private String proxy_url = "https://navicell.curie.fr/cgi-bin/nv_proxy.php";
	private int msg_id = 1000;
	private Set<String> hugo_list;
	private String session_id ="";
	
	NaviCell() {
		// nothing to be done here.
	}
	
	public void setMapUrl(String url) {
		map_url = url;
	}
	
	public void setProxyUrl (String url) {
		proxy_url = url;
	}
	
	public Set<String> getHugoList() {
		return(hugo_list);
	}
	
	public String getSessionId () {
		return(session_id);
	}
	
	
}
