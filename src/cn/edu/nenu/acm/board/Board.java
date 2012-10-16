package cn.edu.nenu.acm.board;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;

/**
 * 目标：<br>
 * 实时更新、减少服务器负载，减少网络负载。<br>
 * 设计：<br>
 * js向server请求一个timestamp之后更新的status。 server返回： <br>
 * 1. 若该 timestamp 小于 server 端 status 缓存的下界（例如20s）之外，则执行裸查询，返回结果。 <br>
 * 2. 若该 timestamp 在 server 缓存之内，则等待（wait(20s)） server
 * 下一个更新周期（例如1s），若存在新结果，则开始唤醒等待（notifyAll），否则不唤醒；若长时间没有唤醒，则自动唤醒，断开连接重来。
 * 
 * @author winguse
 * */
public class Board {

	public final static int RUNSTATUS_YES = 0;
	//public final static int RUNSTATUS_FIRST_BLOOD = 1;
	//2012-09-26 决定不在数据库里面存是不是FIRST BLOOD，原因是，如果发生Rejudge都话，FB得完全重新判断，而PC^2是从来不会告诉你什么时候开始Rejudge的。也就是说，实现都时候，我可能需要每次都判断全部数据，最简单和复杂度可以接受都，都要写个堆来维护，所以，这部分运算了扔给Javascript。
	public final static int RUNSTATUS_PEDDING = 2;
	public final static int RUNSTATUS_NO = 3;
	public final static int RUNSTATUS_DELETED = -1;
	public final static int RUNSTATUS_UNDEFINE = -2;
	/**
	 * 数据库链接，使用连接池
	 */
	private static BasicDataSource dataSource = null;
	/**
	 * 强制刷新时间，如果javascript检查到这个值更新里面，那么整个页面重新载入。 用途是方便js代码更新，因为后端功能基本不变，变化的都是前端。
	 */
	public static long ajaxPageRefreshTimestamp = 0;
	/**
	 * push ajax HTTP链接生存长度，超过这个长度都HTTP会话会被主动断开，js会自动重新建立新的连接。
	 * 具体时间长度待定，看性能如何。考虑两个方便：连接开销和数据开销的问题。
	 */
	public static int pushAjaxInterval = 20000;
	/**
	 * 缓存的(周期)大小<br>
	 * 考虑空间和时间，按那个存比较好呢？——事实上，空间也应该算上，应该有个上界，否则带宽消耗很大。2012.10.12<br>
	 * 时间比较合适，因为空间的缓存不能保证效果，因为提交的密集程度是不均匀的：<br>
	 * 如果一旦密集提交，那么缓存就可能失去意义；<br>
	 * 如果提交很稀疏，网络负载也会变大。
	 */
	public static int statusCacheIntervalCount=30;
	/**
	 * 缓存刷新周期，每隔这样一个时间就更新一次cache，并且，如果确实获得新数据了，就通知等待的ajax
	 */
	public static int statusCacheInterval = 1000;
	/**
	 * 最大同时在线的push ajax数量，因为push的时候，连接是保持的，先定个上界，保证不会503
	 */
	public static int maxActivePushAjaxConnection=1000;
	/**
	 * 封榜时间（分钟） 
	 */
	public static int freezeTime=60*4;
	
	/**
	 * 初始化数据库连接池
	 */
	public static void dataBaseInit() {
		if (dataSource != null) {
			try {
				dataSource.close();
			} catch (Exception e) {
				//
			}
			dataSource = null;
		}
		try {
			Properties p = new Properties();
			p.setProperty("driverClassName", "com.mysql.jdbc.Driver");
			p.setProperty("url", "jdbc:mysql://localhost:3306/ContestBoard");
			p.setProperty("password", "i love shuxiao");
			p.setProperty("username", "ContestBoard");
			p.setProperty("maxActive", "2");
			p.setProperty("maxIdle", "2");
			p.setProperty("maxWait", "1000");
			p.setProperty("removeAbandoned", "true");
			p.setProperty("removeAbandonedTimeout", "120");
			p.setProperty("testOnBorrow", "true");
			p.setProperty("logAbandoned", "true");
			p.setProperty("autoReconnect", "true");
			dataSource = (BasicDataSource) BasicDataSourceFactory
					.createDataSource(p);
		} catch (Exception e) {
			//
		}
	}

	/**
	 * 从数据连接池里获得数据库连接
	 * @return
	 * @throws SQLException
	 */
	public static synchronized Connection getDataBaseConnection()
			throws SQLException {
		if (dataSource == null) {
			dataBaseInit();
		}
		Connection conn = null;
		if (dataSource != null) {
			conn = dataSource.getConnection();
		}
		return conn;
	}

}
/*winguse love shuxiao*/