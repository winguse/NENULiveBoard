package cn.edu.nenu.acm.board.frontend.model;

import static cn.edu.nenu.acm.board.Board.*;
import static cn.edu.nenu.acm.board.frontend.model.DBStatusFetcher.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class StatusCacheUnion extends Thread {

	// private long oldestTimestamp=0;
	private long lastTimestamp = 0;
	private boolean needStop = false;
	private String exitMessage = "";
	private int exitCode = 0;
	private int interval = 0;

	private String result=null; 
	private ArrayList<Integer> cachedIntervalCount = null;
	private ArrayList<Long> cachedIntervalTimestamp = null;
	private ArrayList<ArrayList<Object>> statusCache = null;

	private static StatusCacheUnion me = null;

	private StatusCacheUnion() {
		// 缓存应该单体
		cachedIntervalCount = new ArrayList<Integer>();
		cachedIntervalTimestamp = new ArrayList<Long>();
		statusCache = new ArrayList<ArrayList<Object>>();
		result="";
		exitMessage = "not started..";
		exitCode = -2;//not started 
	}

	/**
	 * 获得缓存线程单体
	 * @param notDeath 如果为真，则保证返回的对象不是死线程
	 * @return
	 */
	public static synchronized StatusCacheUnion getInstance(boolean notDeath) {
		if (me == null){
			me = new StatusCacheUnion();
		}
		if(notDeath&&me.getState()==State.TERMINATED){
			me = new StatusCacheUnion();
		}
		return me;
	}

	@Override
	public void run() {
		System.out.println("cache thread started.");
		// oldestTimestamp=lastTimestamp=new Date().getTime();
		lastTimestamp = 0;//第一次把有屎以来的都获取
		needStop = false;
		exitMessage = "not exit..(running or under exception)";
		exitCode = -1;//running
		interval = 0;
		while (!needStop) {
			while (cachedIntervalCount.size() > statusCacheIntervalCount) {
				for (int i = 0; i < cachedIntervalCount.get(0); i++) {
					statusCache.remove(0);
				}
				cachedIntervalCount.remove(0);
				cachedIntervalTimestamp.remove(0);
			}
			exitMessage=new Date().toLocaleString()+" # intervalCount: "+cachedIntervalCount.size()+
					", statusCache Size: "+statusCache.size();
			// oldestTimestamp=statusCache.get(0).get(index)
			try {
				/*
				 * 2012-10-10 21:00 发现bug了，由于两个服务时间不同步，会造成缓存堆积或者缓存无法换取，
				 * 所以这里面不应该信任数据库服务器时间的时间戳，应该根据数据特性选择。
				 */
				cachedIntervalTimestamp.add(lastTimestamp);
//				long currentTimestamp = new Date().getTime();//考虑数据库操作过程中的并发新插入，宁愿下次重复，也不要漏掉，所以先保存当前时间戳
//				System.out.println("cache interval: fetching from database.");
				ArrayList<ArrayList<Object>> newStatus = getStatusArray(lastTimestamp,false);
//				System.out.println("cache interval: end of fetching from database.");
				cachedIntervalCount.add(newStatus.size());
				if (newStatus.size() > 0) {
					lastTimestamp = getLastUpdateTime();//2012-10-10 21:00 修复时间不同步造成的问题
					System.out.println("Status cache updated, count: "+newStatus.size());
					statusCache.addAll(newStatus);
				}
				//进一步缓存JSONObject
				JSONObject jsonResult=new JSONObject();
				try {
					jsonResult.put("ajaxPageRefreshTimestamp", ajaxPageRefreshTimestamp);
					jsonResult.put("status", statusCache);
					jsonResult.put("statusHeader", getStatusHeader());
					jsonResult.put("code", 0);
					jsonResult.put("message", "Status Fecthed Successfully. (Union Cache) @"+new Date().toLocaleString());
					this.result=jsonResult.toString();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (newStatus.size() > 0) {
					synchronized (this) {
						System.out.println("notify all waiting for cache.");
						notifyAll();
					}
				}
				// 处理重复的，不过应该问题不大，只是数据库用的是闭区间获取，只要依次更新，应该问题不大
			} catch (SQLException e1) {
				e1.printStackTrace();
				exitCode = 1;
				exitMessage = "SQLExcepion: " + e1.getMessage();
				break;
			}
			interval++;
//			System.out.println("Status cache interval: " + interval);
			try {
				sleep(statusCacheInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
				exitCode = 1;
				exitMessage = "InterruptedExcepion: " + e.getMessage();
			}
		}
		exitMessage = "exited normaly..";
		exitCode = 0;//running
		System.out.println("Cache thread stopped.");
	}

	public long getLastTimestamp() {
		return lastTimestamp;
	}

	public void setStop() {
		this.needStop = true;
	}

	public long getOldestTimestamp() {
		if (cachedIntervalTimestamp == null) {
			System.out.println("Oldest Timestamp not avalible.");
			return 0;
		}
		if(cachedIntervalTimestamp.size()==0)return 0;
		return cachedIntervalTimestamp.get(0);
	}

	public ArrayList<ArrayList<Object>> getStatusCache() {
		return statusCache;
	}

	public String getExitMessage() {
		return exitMessage;
	}

	public int getExitCode() {
		return exitCode;
	}

	public int getInterval() {
		return interval;
	}

	public String getJSONCache(){
		return result;
	}
	
}
