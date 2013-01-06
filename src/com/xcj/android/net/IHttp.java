package com.xcj.android.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
/**
 * HTTP操作的抽象接口，适用于多平台
 * @author chunjiang.shieh
 *
 */
public interface IHttp {
	
	
	
	
	/**
	 * 设置post数据
	 * @param byteArray
	 */
	public void postByteArray(byte[] byteArray);
	
	/**
	 * 根据文件路径上传文件
	 * @param filePath
	 */
	public void postMultiPart(String filePath);
	
	/**
	 * 根据文件路径上传文件 并且设置请求头
	 * @param filePath
	 * @param multiParams
	 */
	public void postMultiPart(String filePath,Hashtable<String, String> multiParams);
	
	/**
	 * 上传多个文件
	 * @param fileMap
	 * @param multiParams
	 */
	public void postMultiFiles(Map<String, File> fileMap,
			Hashtable<String, String> multiParams);
	
	/**
	 * 关闭Http资源
	 * @throws IOException
	 */
	public void close() throws IOException;
	
	/**
	 * 执行Http的请求
	 * 执行完成后返回HTTP响应的状态码
	 * @return
	 * @throws IOException
	 */
	public int execute() throws IOException;
	
	
	/**
	 * 设置通用请求属性
	 * @param key
	 * @param value
	 */
	public void setRequestHeaderField(String key, String value);
	
	
	
	/**
	 * 返回响应头属性的值
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public String getResponseHeaderField(String key) throws IOException;
	
	/**
	 * 返回Http响应的输入流
	 * @return
	 * @throws IOException
	 */
	public InputStream openInputStream()  throws IOException;
	
	
	/**
	 * 返回Http响应的字节数组
	 * @return
	 * @throws IOException
	 */
	public byte[] openByteArray() throws IOException;
	
	
	/**
	 * 返回响应数据的长度
	 * @return
	 */
	public long getContentLength();


}
