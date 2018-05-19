package org.hp.qc.web.restapi.docexamples.docexamples.infrastructure;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.net.ssl.*;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * This class keeps the state of the connection for the examples. This class is
 * a thus sharing state singleton. All examples get the instance in their
 * default constructors - (cookies, server url).
 *
 * Some simple methods are implemented to get commonly used paths.
 *
 */
public class RestConnector {

	protected Map<String, String> cookies;
	/**
	 * This is the URL to the ALM application. For example:
	 * http://myhost:8080/qcbin. Make sure that there is no slash at the end.
	 */
	protected String serverUrl;
	protected String domain;
	protected String project;
	HttpClient httpclient=null;
	public RestConnector init(Map<String, String> cookies, String serverUrl, String domain, String project) {

		this.cookies = cookies;
		this.serverUrl = serverUrl;
		this.domain = domain;
		this.project = project;
//		insertCertificateValidation();
		httpclient=HttpClients.createDefault();
		return this;
	}


	private RestConnector() {
	}

	private static RestConnector instance = new RestConnector();

	public static RestConnector getInstance() {
		return instance;
	}

	public String buildEntityCollectionUrl(String entityType) {
		return buildUrl("rest/domains/" + domain + "/projects/" + project + "/" + entityType + "s");
	}

	/**
	 * @param path
	 *            on the server to use
	 * @return a url on the server for the path parameter
	 */
	public String buildUrl(String path) {

		return String.format("%1$s/%2$s", serverUrl, path);
	}

	/**
	 * @return the cookies
	 */
	public Map<String, String> getCookies() {
		return cookies;
	}

	/**
	 * @param cookies
	 *            the cookies to set
	 */
	public void setCookies(Map<String, String> cookies) {
		this.cookies = cookies;
	}

	public Response httpPut(String url, byte[] data, Map<String, String> headers) throws Exception {

		return doHttp("PUT", url, null, data, headers, cookies);
	}

	public Response httpPost(String url, byte[] data, Map<String, String> headers) throws Exception {

		return doHttp("POST", url, null, data, headers, cookies);
	}

	public Response httpDelete(String url, Map<String, String> headers) throws Exception {

		return doHttp("DELETE", url, null, null, headers, cookies);
	}

	public Response httpGet(String url, String queryString, Map<String, String> headers) throws Exception {

		return doHttp("GET", url, queryString, null, headers, cookies);
	}

	/**
	 * @param type
	 *            http operation: get post put delete
	 * @param url
	 *            to work on
	 * @param queryString
	 * @param data
	 *            to write, if a writable operation
	 * @param headers
	 *            to use in the request
	 * @param cookies
	 *            to use in the request and update from the response
	 * @return http response
	 * @throws Exception
	 */
	private Response doHttp(String type, String url, String queryString, byte[] data, Map<String, String> headers,
			Map<String, String> cookies) throws Exception {


		
		HttpGet get=new HttpGet(url);
		HttpPost post=new HttpPost(url);
		HttpPut put=new HttpPut(url);
		HttpDelete delete=new HttpDelete(url);
		if ((queryString != null) && !queryString.isEmpty()) {

			url +="?" + queryString;
			
			URL url1 = new URL(url);   
            URI uri = new URI(url1.getProtocol(), url1.getHost()+":8443", url1.getPath(), url1.getQuery(), null);   
      
            get=new HttpGet(uri.toString());
		}
		String cookieString = getCookieString();
		HttpResponse r=null;
		switch(type)
		{
			case "GET":
				prepareHttpRequest(get, headers, data, cookieString);
				r=httpclient.execute(get);
				break;
			case "POST":
				prepareHttpRequest(post, headers, data, cookieString);
				if(null!=data)
					post.setEntity(new ByteArrayEntity(data));
				r=httpclient.execute(post);
				break;
			case "PUT":
				prepareHttpRequest(put, headers, data, cookieString);
				if(null!=data)
					put.setEntity(new ByteArrayEntity(data));
				r=httpclient.execute(put);
				break;
			case "DELETE":
				prepareHttpRequest(delete, headers, data, cookieString);
				r=httpclient.execute(delete);
				break;
		}
//		

		
		Response ret = retrieveHtmlResponse(r);

		updateCookies(ret);

		return ret;
	}

	/**
	 * @param con
	 *            connection to set the headers and bytes in
	 * @param headers
	 *            to use in the request, such as content-type
	 * @param bytes
	 *            the actual data to post in the connection.
	 * @param cookieString
	 *            the cookies data from clientside, such as lwsso, qcsession,
	 *            jsession etc.
	 * @throws java.io.IOException
	 */
	private void prepareHttpRequest(HttpRequestBase con, Map<String, String> headers, byte[] bytes,
			String cookieString) throws IOException {

		String contentType = null;

		// attach cookie information if such exists
		if ((cookieString != null) && !cookieString.isEmpty()) {

			con.setHeader("Cookie", cookieString);
		}

		// send data from headers
		if (headers != null) {

			// Skip the content-type header - should only be sent
			// if you actually have any content to send. see below.
			contentType = headers.remove("Content-Type");

			Iterator<Entry<String, String>> headersIterator = headers.entrySet().iterator();
			while (headersIterator.hasNext()) {
				Entry<String, String> header = headersIterator.next();
				con.setHeader(header.getKey(), header.getValue());
			}
		}

		// If there's data to attach to the request, it's handled here.
		// Note that if data exists, we take into account previously removed
		// content-type.
		if ((bytes != null) && (bytes.length > 0)) {

//			con.setDoOutput(true);

			// warning: if you add content-type header then you MUST send
			// information or receive error.
			// so only do so if you're writing information...
			if (contentType != null) {
				con.setHeader("Content-Type", contentType);
			}			
			 
		}
	}

	/**
	 * @param con
	 *            that is already connected to its url with an http request, and
	 *            that should contain a response for us to retrieve
	 * @return a response from the server to the previously submitted http
	 *         request
	 * @throws Exception
	 */
	private Response retrieveHtmlResponse(HttpResponse r) throws Exception {

		Response ret = new Response();

		ret.setStatusCode(r.getStatusLine().getStatusCode());
		ret.setResponseHeaders(r.getAllHeaders());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    r.getEntity().writeTo(baos);
	   
		ret.setResponseData(baos.toByteArray());

		return ret;
	}

	private void updateCookies(Response response) {

////		 newCookies = response.getResponseHeaders()..get("Set-Cookie");
//		if (newCookies != null) {
//
//			for (String cookie : newCookies) {
//				int equalIndex = cookie.indexOf('=');
//				int semicolonIndex = cookie.indexOf(';');
//
//				String cookieKey = cookie.substring(0, equalIndex);
//				String cookieValue = cookie.substring(equalIndex + 1, semicolonIndex);
//
//				cookies.put(cookieKey, cookieValue);
//			}
//		}
		String cookie;
		Header[] headers=response.getResponseHeaders();
		for (Header header : headers) {
			if(header.getName().contains("Set-Cookie")) {
		
				cookie=header.getValue();
				int equalIndex = cookie.indexOf('=');
				int semicolonIndex = cookie.indexOf(';');

				String cookieKey = cookie.substring(0, equalIndex);
				String cookieValue = cookie.substring(equalIndex + 1, semicolonIndex);

				cookies.put(cookieKey, cookieValue);
				
			break;
			}
		}
	}

	public String getCookieString() {

		StringBuilder sb = new StringBuilder();

		if (!cookies.isEmpty()) {

			Set<Entry<String, String>> cookieEntries = cookies.entrySet();
			for (Entry<String, String> entry : cookieEntries) {
				sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
			}
		}

		String ret = sb.toString();

		return ret;
	}

	/**
	 * This function fixes the 401 authentication by getting the session
	 */
	public void getQCSession() {

		String qcsessionurl = this.buildUrl("rest/site-session");
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Content-Type", "application/xml");
		requestHeaders.put("Accept", "application/xml");
		try {
			Response resp = this.httpPost(qcsessionurl, null, requestHeaders);
			this.updateCookies(resp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}