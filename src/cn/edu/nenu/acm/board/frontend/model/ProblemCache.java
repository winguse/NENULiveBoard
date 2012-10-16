package cn.edu.nenu.acm.board.frontend.model;

import static cn.edu.nenu.acm.board.Board.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class ProblemCache {

	private static ProblemCache me = null;

	private long cacheTimestamp = 0;
	private JSONObject jsonPrlblems = null;
	private String problems = null;

	private ProblemCache() {
		jsonPrlblems = new JSONObject();
		problems = "{}";
	}

	public static ProblemCache getInstance() {
		if (me == null)
			me = new ProblemCache();
		return me;
	}

	private void refresh() {
		if (new Date().getTime()-cacheTimestamp>pushAjaxInterval*10 || cacheTimestamp < ajaxPageRefreshTimestamp) {
			try {
				Connection conn = getDataBaseConnection();
				try {
					PreparedStatement pstm = conn
							.prepareStatement("SELECT pId,pName FROM Problems");
					pstm.execute();
					ResultSet rs = pstm.getResultSet();
					JSONObject jsonProblemsMain=new JSONObject();
					while (rs.next()) {
						jsonProblemsMain.put("_" + rs.getInt("pId"),
								rs.getString("pName"));
					}
					jsonPrlblems.put("problems", jsonProblemsMain);
					jsonPrlblems.put("code", 0);
					jsonPrlblems.put("message", "Problems' information cached.");
				} catch (SQLException e) {
					jsonPrlblems.put("code", 1);
					jsonPrlblems.put("message", "SQLException: "+e.getMessage());
					e.printStackTrace();
				}
				cacheTimestamp=new Date().getTime();
				problems=jsonPrlblems.toString();
				if (conn != null && !conn.isClosed())
					conn.close();
			} catch (JSONException e) {
				problems = "{\"code\":-1}";
				e.printStackTrace();
			} catch (SQLException e) {
				problems = "{\"code\":-2}";
				e.printStackTrace();
			}
		}
	}

	public JSONObject getJsonPrlblems() {
		refresh();
		return jsonPrlblems;
	}

	public String getProblems() {
		refresh();
		return problems;
	}

}
