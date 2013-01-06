package com.xcj.android.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.apache.http.client.HttpClient;


import android.content.Context;
import android.util.Log;

/**
 * HTTP的线程类
 * @author chunjiang.shieh
 * 
 */
public class HttpThread implements Runnable {
	
	
	
//	private static final String TAG = HttpThread.class.getName();
	
	private static final int NET_CODE = 6000;
	
	// 网络无法链接
	public static final int ERR_NETWORK_DONT_CONNECT = NET_CODE + 1;
	// 网络请求取消
	public static final int ERR_NETWORK_CANCEL = NET_CODE + 2;
	
	
	//最大的重连次数
	private static final int MAX_RETRY_COUNT = 3;
	
	//用来作为请求的ID
	private static int mReqeustNum = 10000;
	
	private Thread mThread = null;
	//线程是否停止的标志位
	private boolean mStop = false;
	
	private Context mContext;
	private RequestQueue mRequestQueue;
	
	private HttpClient mHttpClient;
	
	public HttpThread(Context context){
		mContext = context;
		mRequestQueue = new RequestQueue();
	}
	
	
	public void addRequest(HttpRequest request){
		//将http请求添加到请求队列中
		mRequestQueue.put(request);
		if(mThread == null){
			mThread = new Thread(this);
			mThread.start();		//启动Http的线程
		}
	}
	
	/**
	 * 关闭HTTP线程
	 */
	public void shutdown() {
		if (mThread != null) {
			mStop = true;
			if(mHttpClient != null){
				mHttpClient.getConnectionManager().shutdown();
				mHttpClient = null;
			}
			mRequestQueue.interrupt();
			mThread.interrupt();
			mThread = null;
		}
	}
	

	/**
	 *执行请求通讯的线程
	 */
	@Override
	public void run() {
//		Log.d(TAG, "HttpThread is Running");
		while (!mStop) {
			Object o = mRequestQueue.take();
			if(o != null){
				HttpRequest request = (HttpRequest) o;
				int times = 0;
				//当同时满足这3个条件的时候才执行下
				while (!mStop && times < MAX_RETRY_COUNT && !request.isCancel()) {
					try {
						int code = doHttpRequest(request);
//						Log.d(TAG, "doHttpRequest code:"+code);
						if(code != -1){		//不返回-1的时候跳出循环，否则重试
							break;
						}
						
					} catch (IOException e) {
						e.printStackTrace();
						notifyError(request, ERR_NETWORK_DONT_CONNECT, null);
						break;
					}
					times++;
				}
				if (isCancel(request)){ //取消
					notifyError(request, ERR_NETWORK_CANCEL, null);
				} else if (times >= MAX_RETRY_COUNT) { // 重试失败
					notifyError(request, ERR_NETWORK_DONT_CONNECT, null);
				}
			}
			o = null;
		}
//		Log.d(TAG, "HttpThread is end");
	}
	
	
	/**
	 * 请求的执行方法
	 * @param request
	 * @return
	 * @throws IOException
	 */
	private int doHttpRequest(HttpRequest request) throws IOException {
		
		int responseCode;
		IHttp mHttp = null;
//		try {
			mHttp = getAndroidHttp(request);
			/**
			 * 给请求添加头字段
			 */
			Hashtable<String, String> header = request.getHeaderField();
			if(header != null && header.size() > 0){
				Enumeration<String> enu = header.keys();
				while (enu.hasMoreElements()) {
					String key = enu.nextElement();
					String value = header.get(key);
					mHttp.setRequestHeaderField(key, value);
				}
			}
			/**
			 * 给Post请求添加post数据
			 */
			if(request.getMethod().equals(HttpRequest.METHOD_POST)){
				if(request.getPostData() != null){
					mHttp.postByteArray(request.getPostData());
				}

				if (request.getMultiPartFile() != null) {
					String filePath = request.getMultiPartFile();
//					Log.d(TAG, "------->doHttpRequest filePath: "+filePath);
					Hashtable<String, String> multiParams = request.getMultiPartParams();
					mHttp.postMultiPart(filePath, multiParams);
				}
				
				if(request.getFileMap() != null){
					Map<String, File> fileMap = request.getFileMap();
					Hashtable<String, String> multiParams = request.getMultiPartParams();
					mHttp.postMultiFiles(fileMap, multiParams);
				}
			}
			/**
			 * 准备工作做完了 开始执行
			 */
			responseCode = mHttp.execute();
//			Log.d(TAG, "----------->ResponseCode:"+responseCode);
			if(isCancel(request)){
//				Log.d(TAG, "----------->isCancel ResponseCode:"+responseCode);
				return responseCode;
			}else{
				String contentType = mHttp.getResponseHeaderField("Content-Type");
				if(contentType != null && contentType.indexOf("vnd.wap.wml")>=0){
					return -1;  //重试
				}else{
					if(responseCode == 200){
						if(request.isStream()){		//如果是文件流或图片流
							InputStream stream = null;
							try {
								stream = mHttp.openInputStream();
								notifyReceived(request, stream, mHttp.getContentLength());
							} finally{
								if(stream != null){
									stream.close();
									stream = null;
								}
							}
						}else{
							byte[] data = mHttp.openByteArray();
							notifyReceived(request, data);
						}
					}else{
						InputStream in = mHttp.openInputStream();
						if(in != null){
							in.close();
							in = null;
						}
						notifyError(request, responseCode, null);
					}
				}
				
			}
//		} finally{
//			if(mHttp != null){
//				mHttp.close();
//				mHttp = null;
//			}
//		}
		return responseCode;
	}
	
	
	/**
	 * 获取Android的HTTP封装类
	 * @param request
	 * @return
	 */
	private IHttp getAndroidHttp(HttpRequest request){
		if(mHttpClient == null){
			mHttpClient = CustomHttpClient.getHttpClient();
		}
		String url = request.getUrl();
		String method = request.getMethod();
		if(!url.startsWith("http://") && !url.startsWith("https://")){
			url = "http://" + url;
		}
		IHttp mIHttp = AndroidHttp.createAndroidHttp(mContext,
				url, method, mHttpClient);
		return mIHttp;
		
	}
	
	/**
	 * 请求是否已经取消或者线程是否已经停止
	 * @param request
	 * @return
	 */
	private boolean isCancel(HttpRequest request){
		return mStop || request.isCancel();
	}
	
	
	/**
	 * HTTP请求成功
	 * 功能描述：把成功返回的字节数组回调给请求者
	 * @param request
	 * @param data
	 */
	private void notifyReceived(HttpRequest request,byte[] data){
		if(request.getHttpCallBack() != null){
			request.getHttpCallBack().onReceived(request.getRequestId(), data);
		}
	}
	
	/**
	 * HTTP请求成功
	 * 功能描述：把成功返回的文件流或图片流回调给请求者
	 * @param request
	 * @param stream
	 * @param contentLength
	 */
	private void notifyReceived(HttpRequest request,InputStream stream,long contentLength){
		if(request.getHttpCallBack() != null){
			request.getHttpCallBack().onReceived(request.getRequestId(),
					stream, contentLength);
		}
	}
	
	/**
	 * HTTP请求出错
	 * 功能描述：把出错信息回调给请求者
	 * @param request
	 * @param errorCode
	 * @param data
	 */
	private void notifyError(HttpRequest request,int errorCode,byte[] errorData){
		if(request.getHttpCallBack() != null){
			request.getHttpCallBack().onError(request.getRequestId(),
					errorCode, errorData);
		}
	}
	

	public static synchronized int getNextRequestID() {
		if(mReqeustNum >= Integer.MAX_VALUE){
			mReqeustNum = 10000;
			return mReqeustNum;
		}else{
			return mReqeustNum++;	
		}
	}
}
