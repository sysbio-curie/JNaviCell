package fr.curie.jnavicell;

public class Test {

	public static void main(String[] args) {
		NaviCell n = new NaviCell();

		n.launchBrowser();
		//n.importData("", "/Users/eric/wk/RNaviCell_test/DU145_mut.txt", "Mutation data", "test");
		//n.importData("", "/Users/eric/wk/RNaviCell_test/DU145_CN.txt", "Discrete Copy number data", "test");
		//n.importData("", "/Users/eric/wk/RNaviCell_test/DU145_data.txt", "mRNA Expression data", "test");
		//n.importData("", "/Users/eric/wk/RNaviCell_test/ovca_copynumber.txt", "Discrete Copy number data", "test");

		n.importData("", "/Users/eric/wk/RNaviCell_test/ovca_expression.txt", "mRNA Expression data", "test");
		n.importSampleAnnotation("", "/Users/eric/wk/RNaviCell_test/ovca_sampleinfo.txt");
		n.sampleAnnotationSelectOpen("");
		n.sampleAnnotationSelectAnnotation("", "Ploidy", true);
		n.sampleAnnotationSelectApply("");
		n.sampleAnnotationSelectClose("");
		
		
//		n.mapStainingEditorOpen("");
//		n.mapStainingEditorSelectDatatable("", "test");
//		n.mapStainingEditorSelectSample("", "data");
//		n.mapStainingEditorSetTransparency("", 10);
//		n.mapStainingEditorApply("");

		//n.continuousConfigOpen("", "test", "color");
		//		n.continuousConfigSetAbsVal("", "color", "test", true);
		//		n.continuousConfigApply("", "test", "color");
		//		n.continuousConfigSetSampleMethod("", "color", "test", 0);
		//n.continuousConfigSwitchGroupTab("", "test", "test");
		//n.continuousConfigSetGroupMethod("", "color", "test", 1);
		//n.continuousConfigSetSelectionShape("", "test", "sample", 0, 1);
		//n.continuousConfigSetStepCount("", "sample", "color", "test", 5);
		//n.continuousConfigSetColorAt("", "test", "sample", 0, "000000");
		//n.continuousConfigSetValueAt("", "test", "color", "sample", 0, -1);

		//		try {
		//			Thread.sleep(1000);
		//		} catch (InterruptedException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

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
