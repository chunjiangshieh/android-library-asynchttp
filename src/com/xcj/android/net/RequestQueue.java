package com.xcj.android.net;

import java.util.Vector;

import android.util.Log;


/**
 * 请求的队列
 * 使用Vector模拟先进先出的一个队列,Vector是线程安全的
 * LinkedList类也能实现队列的功能，但是是非线性安全的，必须自己实现访问同步
 * @author chunjiang.shieh
 *
 */
public class RequestQueue {
	
	
	private Vector<Object> mQueue;
	
	
	public RequestQueue(){
		mQueue = new Vector<Object>();
	}
	
	
	/**
	 * 检索并移除此队列的头部，如果此队列不存在任何元素，则一直等待。
	 * @return
	 */
	public Object take()  {
		synchronized (mQueue) {
//			Log.d("RequestQueue", "mQueue size:"+mQueue.size());
			if(mQueue.size() > 0){
				return poll();
			}else{
				try {
					mQueue.wait();	
				} catch (Exception e) {
					// TODO: handle exception
				}
				return poll();	
			}
		}
	}
	
	/**
	 *  检索并移除此队列的头，如果此队列为空，则返回 null。
	 * @return
	 */
	public Object poll(){
		synchronized (mQueue) {
			if(mQueue.size() > 0){
				Object obj = mQueue.elementAt(0);
				mQueue.removeElementAt(0);
				return obj;
			}else{
				return null;
			}
		}
	}
	
	/**
	 * 检索，但是不移除此队列的头，如果此队列为空，则返回 null。
	 * @return
	 */
	public Object peek(){
		synchronized (mQueue) {
			if(mQueue.size() > 0){
				return mQueue.elementAt(0);
			}else{
				return null;
			}
		}
	}

	/**
	 * 将指定的元素添加到队列的尾部.
	 * @param o
	 */
	public void put(Object o) {
		if(o == null) 
			return ;
		synchronized (mQueue) {
			mQueue.addElement(o);
			mQueue.notifyAll();
		}
	}
	
	/**
	 * 移除指定的元素
	 * @param o
	 * @return
	 */
	public boolean remove(Object o){
		synchronized (mQueue) {
			return mQueue.removeElement(o);
		}
	}
	
	
	public int getSize(){
		return mQueue.size();
	}
	
	public void interrupt(){
		synchronized (mQueue) {
			mQueue.notifyAll();	
		}
	}
	
	/**
	 * 指定的元素是否已经包含于队列中
	 * 保证队列请求的唯一性
	 * @param elem
	 * @return
	 */
	public boolean contains(Object elem){
		synchronized (mQueue) {
			return mQueue.contains(elem);	
		}
	}
	
	/**
	 * 清空队列中所有的元素
	 */
	public void clear(){
		synchronized (mQueue) {
			mQueue.removeAllElements();
			mQueue.notifyAll();
		}
	}
	
	
	

}
