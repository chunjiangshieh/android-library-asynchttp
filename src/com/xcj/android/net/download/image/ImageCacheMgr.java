package com.xcj.android.net.download.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.xcj.android.net.HttpCallBack;
import com.xcj.android.net.HttpRequest;
import com.xcj.android.net.HttpThread;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


/**
 * 图片缓存机制. cache查找顺序. 内存缓存- > 文件缓存 -> 网络下载 
 * （可以用图片 url的MD5值做文件缓存的文件名，通过图片url md5做为文件名判断是已存在该文件，这里简单处理了）. 
 * 可以继承该类 定义自己要缓存图片的文件夹位置等
 * @author chunjiang.shieh
 */
public abstract class ImageCacheMgr {

	private static final String TAG = ImageCacheMgr.class.getSimpleName();
	
	private Context mContext;
	
	/**
	 * 内存最大缓存数
	 */
//	private static final int MAX_CACHE_SIZE = 30;
	
	/**
	 * 缓存图片的默认文件夹
	 */
	private static final String FOLDER = "/cache";
	
	private static final String SDCARD_IMAGE_PATH = "/.ImageCache";
	
	
	/**
	 * 强引用缓存区，只能保存一定的数量
	 * 图片-内存缓存
	 * key：图片url
	 * value：图片bitmap
	 */
	private Hashtable<String, Bitmap> mMemoryCache;
	
	
	/**
	 * 图片-文件缓存
	 * key:图片url
	 * value:文件夹名称
	 * 通过url可以得到图片保存的图片路径
	 */
	private Hashtable<String,String> mFolderCache;
	
	/**
	 *  图片下载回调列表  
	 *  key:图片url 
	 *  value:回调列表
	 *  避免相同的请求被发送两次
	 */
	private Hashtable<String, List<ImageCallBack>> mDownloadCallBackMap;
	
	
	/**
	 *  图片URL-内存缓存
	 *  用于记录缓存中最旧的URL
	 */
	private List<String> mMemoryCacheURLList;
	
	
	/**
	 * 请求的ID和对应的URL
	 */
	private Hashtable<Integer, String> mRequestURLMap;
	

	
	/**
	 *  图片下载引擎（线程）
	 */
	private HttpThread mHttpThread;
	
	
	/**
	 * 图片是否需要下载到SDCard的标志位
	 */
	private static final boolean DOWNLOAD_SDCARD = true;

	public ImageCacheMgr(Context context) {
		mContext = context;
		mMemoryCache = new Hashtable<String, Bitmap>();
		mMemoryCacheURLList = new Vector<String>();
		mDownloadCallBackMap = new Hashtable<String, List<ImageCallBack>>();
	}




	
	public Bitmap getImage(String url){
		return getImage(url,FOLDER);
	}
	
	
	/**
	 * 根据url，返回缓存中的图片
	 * 
	 * @param url
	 * @return
	 */
	public Bitmap getImage(String url,String folder) {
		if (!url.startsWith("http://")) {
			url = "http://" + url;
		}
		synchronized (mMemoryCache) {
			if (mMemoryCache.containsKey(url)) {
				return mMemoryCache.get(url);
			} else {
				Bitmap fileCache = getImageFormFile(url,folder);
				if (fileCache != null) {
					if(getMaxCacheSize()>0){
						//找到文件缓存后先存到内存缓存，以供下次直接从内存缓存中读取
						fileCache = addImageToCache(url, fileCache);
					}
					return fileCache;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * 根据url，查找本地文件的图片
	 * 
	 * @param url
	 * @return
	 */
	private Bitmap getImageFormFile(String url,String folder) {
		int endSpritIndex = url.lastIndexOf("/") + 1;
		String filename = url.substring(endSpritIndex);
//		Log.d(TAG, "getImageFormFile fileName："+filename);
		File file = getCacheDir(folder);
//		Log.d(TAG, "getImageFormFile fileDir："+file.getPath());
		if (file != null) {
			file = new File(file, filename);
			if (file.exists()) {
//				Log.d(TAG, "getImageFormFile file.getPath()："+file.getPath());
				return BitmapFactory.decodeFile(file.getPath());
//				BitmapFactory.Options options = new BitmapFactory.Options();
//				options.inSampleSize = 2 ; //图片缩小到原来的1/2
//				return BitmapFactory.decodeFile(file.getPath(),options);  //更省内存 
			}
		}
		return null;
	}
	
	/**
	 * 添加图片到内存缓存.
	 * 
	 * @param url
	 * @param bitmap
	 */
	protected Bitmap addImageToCache(String url, Bitmap bitmap) {
		Hashtable<String, Bitmap> table = getCacheTable();
		List<String> urlList = getCacheURLList();
		synchronized (table) {
			if (table.size() > getMaxCacheSize()) { // 大于缓存数. 腾空一个位置
				String oldURL = urlList.get(0);
				//注释的代码可能还有点问题，在找更好的解决办法
//				Bitmap oldBitmap = table.remove(oldURL);
//				oldBitmap.recycle();
				
				table.remove(oldURL);
				urlList.remove(oldURL);

			}
			table.put(url, bitmap);
			urlList.add(url);
			return bitmap;
		}
	}

	
	/**
	 * 请求下载图片
	 * @param url
	 * @param saveFolder
	 * @param callback
	 */
	protected void doDownloadImage(String url, ImageCallBack callback) {
		doDownloadImage(url,FOLDER,callback);
	}
	
	/**
	 * 请求下载图片.
	 * 
	 * @param url
	 * @param callback
	 */
	protected void doDownloadImage(String url,String saveFolder, ImageCallBack callback) {

		if (!url.startsWith("http://")) {
			url = "http://" + url;
		}

		if (mHttpThread == null) {
			mHttpThread = new HttpThread(mContext);
		}
		if (mRequestURLMap == null) {
			mRequestURLMap = new Hashtable<Integer, String>();
		}
		if(mFolderCache == null){
			mFolderCache = new Hashtable<String, String>();
		}

		synchronized (mDownloadCallBackMap) {
			// 如果同一个地址已经在下载列表中，就不把该请求加到请求队列中了，以免同一个请求发送两次
			if (mDownloadCallBackMap.containsKey(url)) {
				List<ImageCallBack> callbackList = mDownloadCallBackMap
						.get(url);
				if ( callback != null && !callbackList.contains(callback)) {
					callbackList.add(callback);
				}
			} else {
				Vector<ImageCallBack> callbackList = new Vector<ImageCallBack>();
				mDownloadCallBackMap.put(url, callbackList);
				HttpRequest download = new HttpRequest(url,HttpThread.getNextRequestID());
				download.setHttpCallBack(mDownloadCallback);
				download.setStream(true);
				if(callback != null){
					callbackList.add(callback);	
				}
				mHttpThread.addRequest(download);
				mRequestURLMap.put(download.getRequestId(), url);
				if(saveFolder != null){
					mFolderCache.put(url, saveFolder);	
				}
			}
		}
	}



	/**
	 * 获得Cache 内存table
	 * 
	 * @return
	 */
	protected Hashtable<String, Bitmap> getCacheTable() {
		return mMemoryCache;
	}

	/**
	 * 获得Cache 中的URL 列表
	 * 唯一的作用是用于记录缓存中最旧的URL
	 * @return
	 */
	protected List<String> getCacheURLList() {
		return mMemoryCacheURLList;
	}

	/**
	 * 获得应用扩展卡存储路径
	 * 
	 * @param context
	 * @return
	 */
	protected File getCacheDir(String folderStr) {
		File folder = null;
		if(DOWNLOAD_SDCARD){
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				File storage = Environment.getExternalStorageDirectory();
				String sdcardImagePath = (getSdcardImagePath() ==null
						|| getSdcardImagePath().equals(""))?SDCARD_IMAGE_PATH:getSdcardImagePath();
				folder = new File(storage,sdcardImagePath);
			}else{
				folder = mContext.getCacheDir();
			}
		}else{
			folder = mContext.getCacheDir();
		}
		if(folderStr == null || folderStr.length() <= 0){
			return null;
		}else{
			folder = new File(folder,folderStr);
			if(!folder.exists()){
				folder.mkdirs();
			}
		}
		return folder;
	}

	/**
	 * 图片下载完成，通知回调接口
	 */
	private Handler mDownloadHandler ;
	
	// 下载成功
	static final int DOWNLOAD_SUCCEED = 0x0;
	// 下载失败
	static final int DOWNLOAD_FAIL = 0x1;
	//下载进度
	static final int UPDATE_PROGRESS = 0x2;
	
	private Handler getDownloadHandler(){
		if(mDownloadHandler == null){
			mDownloadHandler = new Handler(Looper.getMainLooper()){
				
				public void handleMessage(Message msg) {
					int code = msg.what;
					List<ImageCallBack> callbacks = null;
					String url = (String) msg.obj;
					synchronized (mDownloadCallBackMap) {
						callbacks = mDownloadCallBackMap.get(url);
						//请求完成之后，将该url的回调列表从缓存中移除
						if(code == DOWNLOAD_SUCCEED
								|| code ==DOWNLOAD_FAIL){
							mDownloadCallBackMap.remove(url);
						}
					}
				
					if (callbacks != null && callbacks.size() > 0) {
						if (code == DOWNLOAD_SUCCEED) {
							Bitmap bitmap = getCacheTable().get(url);
							for (int i = 0; i < callbacks.size(); i++) {
								ImageCallBack cb = callbacks.get(i);
								if (cb != null) {
									cb.onGetImage(bitmap, url);
								}
							}
						} else if(code == DOWNLOAD_FAIL){
							for (int i = 0; i < callbacks.size(); i++) {
								ImageCallBack cb = callbacks.get(i);
								cb.onGetError(url);
							}
						}else if(code == UPDATE_PROGRESS){   //更新进度
							int progress =  msg.arg1;
							for (int i = 0; i < callbacks.size(); i++) {
								ImageCallBack cb = callbacks.get(i);
								cb.onUpdateProgress(url,progress);
							}
						}
					}
				}
			};
		}
		return mDownloadHandler;
	}
	
	/**
	 * 下载请求，http回调接口
	 */
	private HttpCallBack mDownloadCallback = new HttpCallBack() {

		@Override
		public void onReceived(int requestId, InputStream stream,
				long contentLength) {

			String url = mRequestURLMap.get(requestId);
			if (url != null) {
				String folder = mFolderCache.get(url);
				mRequestURLMap.remove(requestId);
//				String filename = url.substring(url.lastIndexOf("/")+1, url.lastIndexOf("."));
				String filename = url.substring(url.lastIndexOf("/")+1);
			
				File dir = getCacheDir(folder);
//				Log.d(TAG, "------->filename:"+filename 
//						+" folder:"+folder
//						+" dir: "+dir.getAbsolutePath());
			//	File dir = new File(mStoreDir);
				File tmpFile = new File(dir, filename + ".tmp");
				if (tmpFile.exists()) {
					tmpFile.delete();
				}

				try {
					FileOutputStream fos = new FileOutputStream(tmpFile);
					byte[] buf = new byte[2048];
					int num = -1;
					int count = 0;
					while ((num = stream.read(buf)) != -1) {
						count += num;
						int progress =(int)(((float)count / contentLength) * 100);  
//						Log.d(TAG, "------------------------->progress:"+progress);
						Handler handler = getDownloadHandler();
						Message msg = handler.obtainMessage(UPDATE_PROGRESS, 
								progress, 0, url);
						handler.sendMessage(msg);
						fos.write(buf, 0, num);
					}
					fos.flush();
					fos.close();

					File file = new File(dir, filename);
					if (file.exists()) {
						file.delete();
					}
					tmpFile.renameTo(file);
				
					Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
					
//					BitmapFactory.Options options = new BitmapFactory.Options();
//					options.inSampleSize = 2 ; //图片缩小到原来的1/2
//					Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(),options);  //更省内存 
					/**
					 * 添加图片到缓存
					 */
					addImageToCache(url, bitmap);
					Handler handler = getDownloadHandler();
					Message msg = handler.obtainMessage(DOWNLOAD_SUCCEED, url);
					handler.sendMessage(msg);
				} catch (IOException e) {
					// TODO: handle exception
					e.printStackTrace();
					Handler handler = getDownloadHandler();
					Message msg = handler.obtainMessage(DOWNLOAD_FAIL,url);
					handler.sendMessage(msg);
				}
			}
		}

		@Override
		public void onReceived(int requestId, byte[] data) {
			
		}

		@Override
		public void onError(int requestId, int errCode, byte[] errStr) {
			String url = mRequestURLMap.get(requestId);
			if (url != null) {
				mRequestURLMap.remove(requestId);
				Handler handler = getDownloadHandler();
				Message msg = handler.obtainMessage(DOWNLOAD_FAIL, url);
				handler.sendMessage(msg);
			}
		}
	};


	/**
	 * 释放资源
	 */
	public void shutdown(){
		if(mHttpThread != null){
			mHttpThread.shutdown();
			mHttpThread = null;
		}
		if(mMemoryCache != null){
			mMemoryCache.clear();
			mMemoryCache = null;
		}
		if(mDownloadCallBackMap != null){
			mDownloadCallBackMap.clear();	
			mDownloadCallBackMap = null;
		}
		if(mMemoryCacheURLList != null){
			mMemoryCacheURLList.clear();
			mMemoryCacheURLList = null;
		}
	}
	
	
	
	
	
	/**
	 * 图片下载回调接口. 接口方件在UI Handler处理线程被调用.
	 * 
	 * 
	 */
	public interface ImageCallBack {

		/**
		 * 图片下载成功
		 * 
		 * @param bitmap
		 * @param localPath
		 */
		public void onGetImage(Bitmap bitmap, String url);

		/**
		 * 图片下载失败
		 * 
		 * @param localPath
		 */
		public void onGetError(String url);
		
		/**
		 * 更新下载进度
		 */
		public void onUpdateProgress(String url,int progress);
	} 
	
	/**
	 * return 0 表示不需要缓存
	 * @return
	 */
	public abstract int getMaxCacheSize();
	/**
	 * 保存图片到sdcard的路径
	 * @return
	 */
	public abstract String getSdcardImagePath();
}
