package cn.edu.nenu.acm.board.frontend.model;

import static cn.edu.nenu.acm.board.Board.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import cn.edu.nenu.acm.board.Board;

public class DBStatusFetcher {

	private static long rLastUpdateTime = -1;

	private static JSONObject statusHeader = null;

	/**
	 * 返回一个二位数组，表示在since这个timestamp之后更新的status。<br>
	 * 主要为了数据体积，如果每个都是有字段的话，体积膨胀得有点夸张，而且生成都时候O(logN)的复杂度也没有O(1)好。<br>
	 * 表头的序号另外一个调用里面，但是语义化就差了
	 * 
	 * @param since
	 * @oaram detailStatus 是否需要详细，不要可以节约带宽
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<ArrayList<Object>> getStatusArray(long since,
			boolean detailStatus) throws SQLException {
		synchronized (Board.dbMutex) {
			ArrayList<ArrayList<Object>> status = new ArrayList<ArrayList<Object>>();
			Connection conn = null;
			try {
				conn = Board.getDataBaseConnection();
				PreparedStatement pstat = conn
						.prepareStatement("SELECT "
								+ "rId,rTId,rPId,rLanguage,rStatus,rTime,rNumber,rJudgementName,rDescription,rLastUpdateTime "
								// 这里面改成严格大于了，所以，任何两次更新必须大于1毫秒，否则会有一定概率不正确，当然对于榜来说，这样的是严格小概率事件
								+ "FROM Runs WHERE rLastUpdateTime > ? ORDER BY rLastUpdateTime ASC");
				pstat.setLong(1, since);
				pstat.execute();
				ResultSet rs = pstat.getResultSet();
				while (rs.next()) {
					ArrayList<Object> s = new ArrayList<Object>();
					s.add(rs.getInt("rId"));
					s.add(rs.getInt("rTId"));
					s.add(rs.getInt("rPId"));
					if (detailStatus) {
						s.add(rs.getString("rLanguage"));
					} else {
						s.add("");
					}
					if (rs.getLong("rTime") > freezeTime && !"F".equals(rs.getString("rDescription"))){
						if(rs.getInt("rStatus") == RUNSTATUS_PEDDING)
							s.add(RUNSTATUS_PEDDING);
						else
							s.add(RUNSTATUS_PEDDING_JUDGED);
					}else{
						s.add(rs.getInt("rStatus"));
					}
					s.add(rs.getLong("rTime"));
					s.add(rs.getInt("rNumber"));
					if (detailStatus) {
						if (rs.getLong("rTime") > freezeTime)
							s.add("Freezed");
						else
							s.add(rs.getString("rJudgementName"));
					} else {
						s.add("");
					}
					if (detailStatus) {
						s.add(rs.getString("rDescription"));
					} else {
						s.add("");
					}
					s.add(rs.getLong("rLastUpdateTime"));
					if (rLastUpdateTime < rs.getLong("rLastUpdateTime")) {
						rLastUpdateTime = rs.getLong("rLastUpdateTime");
					}
					status.add(s);
				}
				rs.close();
				pstat.close();
			} catch (SQLException e) {
				throw e;
			}
			if (conn != null && !conn.isClosed())
				conn.close();
			return status;
		}
	}

	/**
	 * 返回单体status表头信息
	 * 
	 * @return
	 * @throws JSONException
	 */
	public static synchronized JSONObject getStatusHeader()
			throws JSONException {
		if (statusHeader == null) {
			statusHeader = new JSONObject();
			int idx = 0;
			// ----------
			statusHeader.put("rId", idx++);
			statusHeader.put("rTId", idx++);
			statusHeader.put("rPId", idx++);
			statusHeader.put("rLanguage", idx++);
			statusHeader.put("rStatus", idx++);
			statusHeader.put("rTime", idx++);
			statusHeader.put("rNumber", idx++);
			statusHeader.put("rJudgementName", idx++);
			statusHeader.put("rDescription", idx++);
			statusHeader.put("rLastUpdateTime", idx++);
			// 顺序跟上面插入顺序保持一致，我是用正则表达是生成上面几行代码的：
			// s\.add\(rs\.get.+?\("(.+)"\)\);
			// statusHeader.put\("\1",0\);
		}
		return statusHeader;
	}

	/**
	 * 2012-10-10 21:00 发现bug了，由于两个服务时间不同步，会造成缓存堆积或者缓存无法换取，
	 * 所以这里面不应该信任数据库服务器时间的时间戳，应该根据数据特性选择。
	 * 
	 * @return 数据库中，最近一次访问找到的最大的时间戳
	 */
	public static long getLastUpdateTime() {
		return rLastUpdateTime;
	}

}
