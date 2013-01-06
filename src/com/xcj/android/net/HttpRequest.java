package com.xcj.android.net;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;


/**
 * HTTP请求的封装 
 * 包括：
 * 1.请求方式
 * 2.请求数据
 * 3.请求的URL
 * 4.请求的ID 
 * 5.请求的状态
 * 6.请求的头字段
 * 7.请求的回调
 * 8.请求返回数据的类型等等
 * @author chunjiang.shieh
 *
 */
public class HttpRequest {
	
	
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	
	/**
	 * 请求的状态是否取消
	 */
	private boolean isCancel;
	
	/**
	 * 请求的URL
	 */
	private String url;
	
	/**
	 * 请求的方法
	 * GET 或 POST
	 */
	private String method;
	
	/**
	 * 头字段
	 */
	private Hashtable<String, String> mHeaderField;
	
	/**
	 * Post的数据
	 */
	private byte[] mPostByteArray;
	
	/**
	 * 请求的回调
	 */
	private HttpCallBack mHttpCallBack;
	
	/**
	 * 请求的ID
	 */
	private int mRequestId;
	
	/**
	 * 返回的数据是否是文件流或者是图片流
	 */
	private boolean isStream;
	
	/**
	 * 增加文件的支持 2012-09-28 18:07
	 */
	
	private String mMultiPartFile;
	private Hashtable<String, String> mMultiPartParams;
	
	/**
	 * 增加多个文件上传的支持 2012-12-24 09:54
	 */
	private Map<String, File> mFileMap;
	
	
	public HttpRequest(String url){
		this(url,METHOD_GET,-1);
	}
	
	public HttpRequest(String url,int requestId){
		this(url,METHOD_GET,requestId);
	}
	
	public HttpRequest(String url,byte[] postData,int requestId){
		
		this(url,METHOD_POST,requestId);
		mPostByteArray = postData;
	}
	
	
	public HttpRequest(String url,String method,int requestId){
		this.url = url;
		this.method = method;
		if(mRequestId != -1){
			mRequestId = requestId;
		}else{
			mRequestId = HttpThread.getNextRequestID();
		}
		isCancel = false;
	}
	

	public boolean isCancel() {
		return isCancel;
	}

	public void setCancel(boolean isCancel) {
		this.isCancel = isCancel;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Hashtable<String, String> getHeaderField() {
		return mHeaderField;
	}

	public void setHeaderField(Hashtable<String, String> mHeaderField) {
		if(mHeaderField != null){
			this.mHeaderField = mHeaderField;
		}
	}
	
	/**
	 * 添加头字段的key和value
	 * @param key
	 * @param value
	 */
	public void addHeaderField(String key,String value){
		if(mHeaderField == null){
			mHeaderField = new Hashtable<String, String>();
		}
		mHeaderField.put(key, value);
	}

	public byte[] getPostData() {
		return mPostByteArray;
	}

	public void setPostData(byte[] mPostByteArray) {
		this.mPostByteArray = mPostByteArray;
	}

	public HttpCallBack getHttpCallBack() {
		return mHttpCallBack;
	}

	public void setHttpCallBack(HttpCallBack mHttpCallBack) {
		this.mHttpCallBack = mHttpCallBack;
	}

	public int getRequestId() {
		return mRequestId;
	}

	public void setRequestId(int mRequestId) {
		this.mRequestId = mRequestId;
	}

	public boolean isStream() {
		return isStream;
	}

	public void setStream(boolean isStream) {
		this.isStream = isStream;
	}
	
	
	
	public String getMultiPartFile(){
		return mMultiPartFile;
	}
	
	
	
	public Map<String, File> getFileMap() {
		return mFileMap;
	}

	public void setMultiPartParams(String key, String value){
		if(mMultiPartParams == null)
			mMultiPartParams = new Hashtable<String, String>();
		mMultiPartParams.put(key, value);
	}
	public Hashtable<String, String> getMultiPartParams(){
		return mMultiPartParams;
	}
	
	public void postMultiPartFile(String filePath){
		mMultiPartFile = filePath; 
		if(filePath != null){
			method = METHOD_POST;
		}
	}
	
	public void postMultiPartFile(String filePath, byte[] data){
		mMultiPartFile = filePath;
		mPostByteArray = data;
		if(filePath != null){
			method = METHOD_POST;
		}
	}
	
	
	public void postFileMap(Map<String, File> fileMap){
		this.mFileMap = fileMap;
		if(fileMap != null){
			method = METHOD_POST;
		}
	}

}
