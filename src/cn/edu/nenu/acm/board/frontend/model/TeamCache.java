package cn.edu.nenu.acm.board.frontend.model;

import static cn.edu.nenu.acm.board.Board.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class TeamCache {

	private static TeamCache me = null;

	private long cacheTimestamp = 0;
	private JSONObject jsonTeams = null;
	private String teams = null;

	private TeamCache() {
		jsonTeams = new JSONObject();
		teams = "{}";
	}

	public static TeamCache getInstance() {
		if (me == null)
			me = new TeamCache();
		return me;
	}

	private void refresh() {
		if (new Date().getTime()-cacheTimestamp>pushAjaxInterval*10|| cacheTimestamp < ajaxPageRefreshTimestamp) {
			try {
				Connection conn = getDataBaseConnection();
				try {
					PreparedStatement pstm = conn
							.prepareStatement("SELECT tId,tDisplayName FROM Teams");
					pstm.execute();
					ResultSet rs = pstm.getResultSet();
					JSONObject jsonTeamsMain=new JSONObject();
					while (rs.next()) {
						jsonTeamsMain.put("_" + rs.getInt("tId"),
								rs.getString("tDisplayName"));
					}
					jsonTeams.put("teams", jsonTeamsMain);
					jsonTeams.put("code", 0);
					jsonTeams.put("message", "Teams' information cached.");
				} catch (SQLException e) {
					jsonTeams.put("code", 1);
					jsonTeams.put("message", "SQLException: "+e.getMessage());
					e.printStackTrace();
				}
				cacheTimestamp=new Date().getTime();
				teams=jsonTeams.toString();
				if (conn != null && !conn.isClosed())
					conn.close();
			} catch (JSONException e) {
				teams = "{\"code\":-1}";
				e.printStackTrace();
			} catch (SQLException e) {
				teams = "{\"code\":-2}";
				e.printStackTrace();
			}
		}
	}

	public JSONObject getJsonTeams() {
		refresh();
		return jsonTeams;
	}

	public String getTeams() {
		refresh();
		return teams;
	}

}
