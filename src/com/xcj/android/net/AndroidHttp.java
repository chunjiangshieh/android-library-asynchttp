package com.xcj.android.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Android 的Http的封装类,实现IHttp接口
 * Apache的HttpClient
 * @author chunjiang.shieh
 *
 */
public class AndroidHttp implements IHttp {
	
	private static final String TAG = AndroidHttp.class.getName();
	
	/**
	 * 分隔线 产生的随机数
	 */
	private static Random mBoundaryRandom = new Random();
	
	private HttpClient mHttpClient;
	/**
	 * 请求的URL
	 */
	private String mUrl;
	/**
	 * 请求的方法
	 * POST or GET
	 */
	private String mMethod;
	
	private boolean isWap;
	
	/**
	 * Post 方式请求的Post数据
	 */
	private byte[] mPostData;
	/**
	 * 请求头
	 */
	private Hashtable<String,String> mRequestHeaderField;
	
	private HttpResponse mHttpResponse = null;
	
	
	/**
	 * 2012-09-28 15:27
	 * 新增对文件上传的支持
	 */
	
	/**
	 * 文件的路径
	 */
	private String mMultiPartFile;
	
	/**
	 * 上传文件的请求头
	 */
	private Hashtable<String, String> mMultiParams;
	
	
	/**
	 * 增加多个文件上传的支持 2012-12-24 09:54
	 */
	private Map<String, File> mFileMap;
	
	/**
	 * 创建AndroidHttp的实例
	 * @param context
	 * @param url
	 * @param method
	 * @param httpClient
	 * @return
	 */
	public static IHttp createAndroidHttp(Context context,String url,
			String method,HttpClient httpClient){
		if(httpClient == null)
			return null;
		AndroidHttp androidHttp = new AndroidHttp();
		androidHttp.mHttpClient = httpClient;
		androidHttp.mUrl = url;
		androidHttp.mMethod = method;
		androidHttp.isWap = isWap(context);
		return androidHttp;
	}
	


	@Override
	public void close() throws IOException {
//		if(mHttpResponse != null && mHttpResponse.getEntity()!= null){
//			InputStream in = mHttpResponse.getEntity().getContent();
//			if(in != null){
//				in.close();
//			}
//		}
	}



	@Override
	public int execute() throws IOException {
		if(mMethod.equals(HttpRequest.METHOD_POST)){
			mHttpResponse = httpPost();
		}else{
			mHttpResponse = httpGet();
		}
		return getResponseCode();
	}



	@Override
	public void postByteArray(byte[] byteArray) {
		this.mPostData = byteArray;
	}


	@Override
	public void setRequestHeaderField(String key, String value) {
		if(mRequestHeaderField == null){
			mRequestHeaderField = new Hashtable<String, String>();
		}
		mRequestHeaderField.put(key, value);
	}
	

	@Override
	public String getResponseHeaderField(String key) throws IOException {
		if(mHttpResponse != null){
			Header mHeader = mHttpResponse.getFirstHeader(key);
			if(mHeader != null){
				return mHeader.getValue();
			}
		}
		return null;
	}



	@Override
	public InputStream openInputStream() throws IOException {
		if(mHttpResponse != null && mHttpResponse.getEntity() != null){
			return mHttpResponse.getEntity().getContent();
		}
		return null;
	}



	@Override
	public byte[] openByteArray() throws IOException {
		if(mHttpResponse != null && mHttpResponse.getEntity() != null){
			return EntityUtils.toByteArray(mHttpResponse.getEntity());
		}
		return null;
	}
	
	
	@Override
	public long getContentLength() {
		if(mHttpResponse != null && mHttpResponse.getEntity() != null){
			return mHttpResponse.getEntity().getContentLength();
		}
		return -1;
	}
	
	
	private HttpResponse httpGet() throws IOException{
		Log.d(TAG, "--------->httpGet Url:"+mUrl);
		buildHttpClient();
		HttpResponse mHttpResponse = null;
		
		HttpGet mHttpGet = new HttpGet(mUrl);
		if(mRequestHeaderField != null && mRequestHeaderField.size()>0){
			Enumeration<String> enu = mRequestHeaderField.keys();
			while (enu.hasMoreElements()) {
				String key = enu.nextElement();
				String value = mRequestHeaderField.get(key);
				mHttpGet.setHeader(key, value);
			}
		}
		mHttpResponse = mHttpClient.execute(mHttpGet);
		return mHttpResponse;
	}
	
	private HttpResponse httpPost() throws IOException{
		buildHttpClient();
		HttpEntity mHttpEntity = null;
		HttpResponse mHttpResponse = null;
		HttpPost mHttpPost = new HttpPost(mUrl);
		if(mRequestHeaderField != null && mRequestHeaderField.size()>0){
			Enumeration<String> enu = mRequestHeaderField.keys();
			while (enu.hasMoreElements()) {
				String key = enu.nextElement();
				String value = mRequestHeaderField.get(key);
				mHttpPost.setHeader(key, value);
			}
		}
		if(mPostData != null){
			mHttpEntity = new ByteArrayEntity(mPostData);
		}else if(mMultiPartFile != null){		//单个文件上传
			Log.d(TAG, "mMultiPartFile is not null mMultiPartFile: "+mMultiPartFile);
			final String BOUNDARY = getBoundary();  //数据分隔线
			final String END_LINE = "--" + BOUNDARY + "--\r\n";	//数据结束标志
			final String PREFIX = "--", LINEND = "\r\n";//前缀和结尾（换行）
			mHttpPost.setHeader("Content-Type",
					"multipart/form-data; boundary=" + BOUNDARY);
			StringBuffer headerStr = new StringBuffer();
			if(mMultiParams != null){
				for (Map.Entry<String, String> entry : mMultiParams.entrySet()) { 
					headerStr.append(PREFIX);  //前缀
					headerStr.append(BOUNDARY);	//分割线
					headerStr.append(LINEND);	//结尾
					headerStr.append("Content-Disposition: form-data; name=\""	
							+ entry.getKey() + "\"" + LINEND); //请求头的KEY
					//请求头的值
					headerStr.append(LINEND);
					headerStr.append(entry.getValue());
					headerStr.append(LINEND);  //必须换行
				} 
			}
		
			byte[] headerData = headerStr.toString().getBytes();
			
			byte[] endData = END_LINE.getBytes();
			
			File file = new File(mMultiPartFile);
			/**
			 * 文件的信息
			 */
			StringBuffer fileInfoStr = new StringBuffer();
			fileInfoStr.append(PREFIX);
			fileInfoStr.append(BOUNDARY);
			fileInfoStr.append(LINEND);
			fileInfoStr.append("Content-Disposition: form-data; name=\"file\"; filename=\""
					+ file.getName() + "\"" + LINEND);
			fileInfoStr.append("Content-Type: application/octet-stream; charset=" 
					+ "UTF-8" + LINEND); 
			fileInfoStr.append(LINEND);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputStream is = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			int len = -1;
			while ((len = is.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			byte[] fileData = baos.toByteArray();  //文件的内容
			is.close();
			/**
			 * 通过File 封装一个HttpEntity  包含headerData、数据流、endData
			 * 重写getContent方法
			 */
			MultiPartEntity mMultiPartEntity = new MultiPartEntity();
			//头数据
			mMultiPartEntity.addByteArray(headerData, headerData.length);
			//文件数据
			mMultiPartEntity.addByteArray(fileInfoStr.toString().getBytes(), fileInfoStr.toString().getBytes().length);
			mMultiPartEntity.addByteArray(fileData, fileData.length);
			mMultiPartEntity.addByteArray(LINEND.getBytes(), LINEND.getBytes().length);//换行 必须
			//结束标志数据
			mMultiPartEntity.addByteArray(endData, endData.length);
			mHttpEntity = mMultiPartEntity;
		}else if(mFileMap != null){
			final String BOUNDARY = getBoundary();  //数据分隔线
			final String END_LINE = "--" + BOUNDARY + "--\r\n";	//数据结束标志
			final String PREFIX = "--", LINEND = "\r\n";//前缀和结尾
			mHttpPost.setHeader("Content-Type",
					"multipart/form-data; boundary=" + BOUNDARY);
			StringBuffer headerStr = new StringBuffer();
			if(mMultiParams != null){
				for (Map.Entry<String, String> entry : mMultiParams.entrySet()) { 
					headerStr.append(PREFIX);  //前缀
					headerStr.append(BOUNDARY);	//分割线
					headerStr.append(LINEND);	//结尾
					headerStr.append("Content-Disposition: form-data; name=\""	
							+ entry.getKey() + "\"" + LINEND); //请求头的KEY
					//请求头的值
					headerStr.append(LINEND);
					headerStr.append(entry.getValue());
					headerStr.append(LINEND);
				} 
			}
		
			
			byte[] headerData = headerStr.toString().getBytes();
			byte[] endData = END_LINE.getBytes();
			/**
			 * 通过File 封装一个HttpEntity  包含headerData、数据流、endData
			 * 重写getContent方法
			 */
			MultiPartEntity mMultiPartEntity = new MultiPartEntity();
			//头数据
			mMultiPartEntity.addByteArray(headerData, headerData.length);
			if(mFileMap != null){
				//遍历所有文件
				for (Map.Entry<String, File> entry : mFileMap.entrySet()) {
					/**
					 * 文件的信息（文件的名称，文件的编码格式等等）
					 */
					StringBuffer fileInfoStr = new StringBuffer();
					fileInfoStr.append(PREFIX);
					fileInfoStr.append(BOUNDARY);
					fileInfoStr.append(LINEND);
					fileInfoStr.append("Content-Disposition: form-data; name=\"file\"; filename=\""
							+ entry.getKey()  + "\"" + LINEND);
					fileInfoStr.append("Content-Type: application/octet-stream; charset=" 
							+ "UTF-8" + LINEND); 
					fileInfoStr.append(LINEND);
					/**
					 * 文件的内容
					 */
					File file = entry.getValue();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					InputStream is = new FileInputStream(file);
					byte[] buffer = new byte[1024];
					int len = -1;
					while ((len = is.read(buffer)) != -1) {
						baos.write(buffer, 0, len);
					}
					byte[] fileData = baos.toByteArray();  //文件的内容
					is.close();
					//文件数据
					mMultiPartEntity.addByteArray(fileInfoStr.toString().getBytes(), fileInfoStr.toString().getBytes().length);
					mMultiPartEntity.addByteArray(fileData, fileData.length);
					mMultiPartEntity.addByteArray(LINEND.getBytes(), LINEND.getBytes().length);//换行 必须
				}
			}
			//结束标志数据
			mMultiPartEntity.addByteArray(endData, endData.length);
			mHttpEntity = mMultiPartEntity;
		}
		mHttpPost.setEntity(mHttpEntity);
		mHttpResponse = mHttpClient.execute(mHttpPost);
		return mHttpResponse;
	}
	
	
	public int getResponseCode(){
		if(mHttpResponse != null && mHttpResponse.getStatusLine() != null){		
			Log.d(TAG, "ResponseCode:"+mHttpResponse.getStatusLine().getStatusCode());
			return mHttpResponse.getStatusLine().getStatusCode();
		}
		else{
			Log.d(TAG, "mHttpResponse is null");
			return -1;
		}		
	}
	
	private void buildHttpClient() {
		//如果是cmwap接入的话 不设置代理是上不了网的，一般代理IP都用10.0.0.172，端口默认80
//		if (isWap) { 
//		    // used to access cmwap
//			Log.d(TAG, "buildHttpClient isWap");
//		    HttpHost proxy = new HttpHost("10.0.0.172", 80);
//		    mHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//		}else{
			mHttpClient.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
//		}
	}
	
	
	 /**
	  * 检测网络连接是否是cmwap
	  * 如果是的话需要设置代理
	  * @param context
	  * @return
	  */
	 private static boolean isWap(Context context){
		 //ConnectivityManager主要管理和网络连接相关的操作 
		 ConnectivityManager cm =(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		 NetworkInfo nInfo = cm.getActiveNetworkInfo();
		 if(nInfo == null || nInfo.getType() != ConnectivityManager.TYPE_MOBILE)
			 return false;
		 String extraInfo = nInfo.getExtraInfo();
		 if(extraInfo == null || extraInfo.length() < 3)
			 return false;
		 if(extraInfo.toLowerCase().contains("wap"))
			 return true;
		 return false;
	 }



	 //文件支持
	 
	@Override
	public void postMultiPart(String filePath) {
		postMultiPart(filePath, null);
	}



	@Override
	public void postMultiPart(String filePath,
			Hashtable<String, String> multiParams) {
		this.mMultiPartFile = filePath;
		this.mMultiParams = multiParams;
	}
	 
	
	/**
	 * 获取数据分隔线
	 * 由系统当前时间和随机产生的长整型组成 
	 * @return
	 */
	private String getBoundary(){
		return String.valueOf(System.currentTimeMillis()) 
				+ String.valueOf(mBoundaryRandom.nextLong());
	}



	@Override
	public void postMultiFiles(Map<String, File> fileMap,
			Hashtable<String, String> multiParams) {
		this.mFileMap = fileMap;
		this.mMultiParams = multiParams;
	}
	 
	 
	 

}
