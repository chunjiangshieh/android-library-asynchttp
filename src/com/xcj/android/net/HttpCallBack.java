package com.xcj.android.net;

import java.io.InputStream;

/**
 * 请求的回调接口
 * @author chunjiang.shieh
 *
 */
public interface HttpCallBack{
	
	public void onError(int requestId, int errCode, byte[] errStr);
	
	public void onReceived(int requestId, byte[] data);
	
	public void onReceived(int requestId, InputStream stream, long contentLength);	
}
