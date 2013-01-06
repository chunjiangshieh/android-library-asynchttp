package com.xcj.android.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.entity.AbstractHttpEntity;

/**
 *	上传文件的请求实体
 * @author chunjiang.shieh
 *
 */
public class MultiPartEntity extends AbstractHttpEntity {
	
	private static final int TYPE_BYTEARRAY = 1;
	private static final int TYPE_INPUTSTREAM = 2;
	
	
	private List<RequestData> mDataList = new ArrayList<MultiPartEntity.RequestData>();

	@Override
	public boolean isRepeatable() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 *  获取数据内容的长度
	 */
	@Override
	public long getContentLength() {
		int length = 0;
		for(int i=0;i<mDataList.size();i++){
			RequestData data = mDataList.get(i);
			length += data.mLength;
		}
		return length;
	}

	/**
	 * 获取数据内容
	 */
	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for(RequestData data:mDataList){
			if(data.mDataType == TYPE_BYTEARRAY){
				byte[] byteArray = (byte[]) data.mContent;
				baos.write(byteArray, 0, (int)data.mLength);
			}else if(data.mDataType == TYPE_INPUTSTREAM){
				InputStream inputStream = (InputStream) data.mContent;
				byte[] buffer = new byte[2048];
				int readed = 0;
				long remaining = data.mLength;
				while (remaining>0) {
					readed = inputStream.read(buffer, 0, (int) Math.min(2048, remaining));
					if(readed == -1){		//读完跳出循环
						break;
					}
					baos.write(buffer, 0, readed);
					remaining -= readed;
				}
				inputStream.close();
			}
		}
		InputStream in = new ByteArrayInputStream(baos.toByteArray());
		return in;
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
		for(RequestData data:mDataList){
			if(data.mDataType == TYPE_BYTEARRAY){
				byte[] byteArray = (byte[]) data.mContent;
				outstream.write(byteArray, 0, (int)data.mLength);
			}else if(data.mDataType == TYPE_INPUTSTREAM){
				InputStream inputStream = (InputStream) data.mContent;
				byte[] buffer = new byte[2048];
				int readed = 0;
				long remaining = data.mLength;
				while (remaining>0) {
					readed = inputStream.read(buffer, 0, (int) Math.min(2048, remaining));
					if(readed == -1){		//读完跳出循环
						break;
					}
					outstream.write(buffer, 0, readed);
					remaining -= readed;
				}
				inputStream.close();
			}
		}
		
		outstream.flush();
	}

	@Override
	public boolean isStreaming() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	/**
	 * 添加字节数组
	 * @param array
	 * @param length
	 */
	public void addByteArray(byte[] array,long length){
		if(array == null)
			return;
		RequestData data = new RequestData(TYPE_BYTEARRAY);
		data.mContent = array;
		data.mLength = length;
		mDataList.add(data);
	}
	
	public void addInputStream(InputStream in,long length){
		if(in == null)
			return;
		RequestData data = new RequestData(TYPE_INPUTSTREAM);
		data.mContent = in;
		data.mLength = length;
		mDataList.add(data);
	}
	
	
	/**
	 * 把不同的数据类型封装成同一个请求数据
	 * @author chunjiang.shieh
	 *
	 */
	public class RequestData{
		
		private int mDataType;	//数据的类型
		private Object mContent;  //数据的内容
		private long mLength;  //数据的长度
		
		public RequestData(int dataType){
			this.mDataType = dataType;
		}
	}
	
	

	
}
