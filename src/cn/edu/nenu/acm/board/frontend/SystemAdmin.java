package cn.edu.nenu.acm.board.frontend;

import java.io.IOException;
import static cn.edu.nenu.acm.board.frontend.model.BoardAppListener.*;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import cn.edu.nenu.acm.board.frontend.model.StatusCacheUnion;

import static cn.edu.nenu.acm.board.Board.*;

/**
 * Servlet implementation class SystemAdmin
 */
@WebServlet(description = "系统的一些参数设置，状态查询等。", urlPatterns = { "/SystemAdmin" })
public class SystemAdmin extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SystemAdmin() {
		super();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("utf-8");
		response.setContentType("application/json");
		StatusCacheUnion cacheUnion = StatusCacheUnion.getInstance(false);
		String login=(String) request.getSession().getAttribute("login");
		if (request.getParameter("pushAjaxInterval") != null&&login!=null) {
			try {
				pushAjaxInterval = Integer.parseInt(request
						.getParameter("pushAjaxInterval"));
				statusCacheIntervalCount = Integer.parseInt(request
						.getParameter("statusCacheIntervalCount"));
				statusCacheInterval = Integer.parseInt(request
						.getParameter("statusCacheInterval"));
				maxActivePushAjaxConnection = Integer.parseInt(request
						.getParameter("maxActivePushAjaxConnection"));
				freezeTime = Integer.parseInt(request
						.getParameter("freezeTime"));
			} catch (Exception e) {
			}
		}
		if ("true".equals(request.getParameter("setStop"))) {
			cacheUnion.setStop();
		}
		if ("true".equals(request.getParameter("setRefresh"))) {
			ajaxPageRefreshTimestamp = new Date().getTime();
		}
		try {
			JSONObject result = new JSONObject();
			JSONObject setting = new JSONObject();
			JSONObject pushAjax = new JSONObject();
			JSONObject cache = new JSONObject();

			result.put("code", 0);
			result.put("message", "ok.");

			setting.put("pushAjaxInterval", pushAjaxInterval);
			setting.put("statusCacheIntervalCount", statusCacheIntervalCount);
			setting.put("statusCacheInterval", statusCacheInterval);
			setting.put("maxActivePushAjaxConnection",
					maxActivePushAjaxConnection);
			setting.put("freezeTime", freezeTime);

			pushAjax.put("ajaxPageRefreshTimestamp", ajaxPageRefreshTimestamp);
			pushAjax.put("liveCount", liveCount.get());
			pushAjax.put("cacheRequestCount", GetStatus.cacheRequestCount.get());
			pushAjax.put("dbRequestCount", GetStatus.dbRequestCount.get());

			cache.put("lastTimestamp", cacheUnion.getLastTimestamp());
			cache.put("exitCode", cacheUnion.getExitCode());
			cache.put("exitMessage", cacheUnion.getExitMessage());
			cache.put("oldestTimestamp", cacheUnion.getOldestTimestamp());
			cache.put("isAlive", cacheUnion.isAlive());
			cache.put("interval", cacheUnion.getInterval());

			result.put("setting", setting);
			result.put("pushAjax", pushAjax);
			result.put("cache", cache);

			response.getWriter().print(result.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

}
