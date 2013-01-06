package com.xcj.android.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

public class CustomHttpClient {
	
	private static HttpClient mCustomHttpClient;
	
	private CustomHttpClient(){
		
	}
	
	/**
	 * Get our single instance of our HttpClient object.
	 *
	 * @return an HttpClient object with connection parameters set
	 */
	public static synchronized HttpClient getHttpClient(){
		if(mCustomHttpClient == null){
			HttpParams params = new BasicHttpParams();
			//HTTP 协议的版本
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			//默认的字符集
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			HttpProtocolParams.setUseExpectContinue(params, true);  
			HttpProtocolParams.setUserAgent(params,  
					"Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 " +
			"(KHTML, like Gecko) Version/4.0 Mobile Safari/533.1" );  
			/**
			 * 连接管理器超时
			 * 定义应用程序应等待多久才让一个连接退出连接管理器所管理的连接池
			 */
			ConnManagerParams.setTimeout(params, 1000);  
			/**
			 * (连接超时)定义应该等待多久才能通过网络连接到另一端的服务器
			 */
			HttpConnectionParams.setConnectionTimeout(params, 20*1000);  
			/**
			 * (请求超时)套接字超时
			 * 定义应该等待多久时间才能获取请求的数据
			 */
			HttpConnectionParams.setSoTimeout(params, 15*1000);  
			SchemeRegistry schReg = new SchemeRegistry();  
			/**
			 * HttpClient支持的模式
			 * HTTP / HTTPS
			 */
			schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));  
			schReg.register(new Scheme("https",SSLSocketFactory.getSocketFactory(), 443));  
			/**
			 * 负责管理HttpClient的HTTP连接
			 * 考虑多线程，需要线程安全
			 */
			ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params,schReg);  
			mCustomHttpClient = new DefaultHttpClient(conMgr, params);  
		}
		return mCustomHttpClient;
	}
	
	/**************************下面的方法仅做测试**************************/
	/**
	 * Performs an HTTP Post request to the specified url with the
	 * specified parameters.
	 *
	 * @param url The web address to post the request to
	 * @param postParameters The parameters to send via the request
	 * @return The result of the request
	 * @throws Exception
	 */
	public static String executeHttpPost(String url, List<NameValuePair> postParameters) throws Exception {
		BufferedReader in = null;
		try {
			HttpClient client = getHttpClient();
			HttpPost request = new HttpPost(url);
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
			request.setEntity(formEntity);
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			String result = sb.toString();
			return result;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Performs an HTTP GET request to the specified url.
	 *
	 * @param url The web address to post the request to
	 * @return The result of the request
	 * @throws Exception
	 */
	public static String executeHttpGet(String url) throws Exception {
		BufferedReader in = null;
		try {
			HttpClient client = getHttpClient();
			HttpGet request = new HttpGet();
			request.setURI(new URI(url));
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			String result = sb.toString();
			return result;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public Object clone() throws CloneNotSupportedException {  
		throw new CloneNotSupportedException();  
	}  
	
	
	
	public static String executeHttpPost(String url, byte[] postData) throws Exception {
		BufferedReader in = null;
		try {
			HttpClient client = getHttpClient();
			HttpPost request = new HttpPost(url);
			ByteArrayEntity byteArrayEntity = new ByteArrayEntity(postData);
			request.setEntity(byteArrayEntity);
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			String result = sb.toString();
			return result;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
