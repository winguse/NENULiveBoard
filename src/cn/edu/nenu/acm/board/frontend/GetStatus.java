package cn.edu.nenu.acm.board.frontend;

import java.io.IOException;
import java.lang.Thread.State;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

import com.winguse.util.Counter;

import cn.edu.nenu.acm.board.frontend.model.StatusCacheUnion;

import static cn.edu.nenu.acm.board.frontend.model.DBStatusFetcher.*;
import static cn.edu.nenu.acm.board.Board.*;
import static cn.edu.nenu.acm.board.frontend.model.BoardAppListener.*;

/**
 * Servlet implementation class GetStatus
 */
@WebServlet(urlPatterns = { "/GetStatus" }, initParams = { @WebInitParam(name = "since", value = "0", description = "the timestamp of when runs are updated or inserted") })
public class GetStatus extends HttpServlet {
	private static final long serialVersionUID = 1L;


	public static Counter cacheRequestCount = new Counter();
	public static Counter dbRequestCount = new Counter();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetStatus() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("utf-8");
		response.setContentType("application/json");
		HttpSession session = request.getSession();
		Long lSince = (Long) session.getAttribute("since");//这个是数据中的最大时间戳，数据中的和服务器的不一定是同步的
		Long lastGetStatus = (Long) session.getAttribute("lastGetStatus");//这个是上次getStatus的时间戳
		Long loadTime = (Long) session.getAttribute("loadTime");
		Boolean loading = (Boolean) session.getAttribute("loading");
		String login=(String) request.getSession().getAttribute("login");
		boolean longConnection="true".equals(request.getParameter("longConnection"));
		if(lastGetStatus==null||new Date().getTime()-lastGetStatus>pushAjaxInterval){
			loading=false;
		}
		if (loading != null && loading&&login==null&&liveCount.get()>maxActivePushAjaxConnection/2) {
			response.getWriter().print(
					"{\"code\":6,\"message\":\"已经有活跃的连接，当前连接被拒绝，请稍后 "
							+ pushAjaxInterval + "ms 再试。\"}");
			return;
		}
		session.setAttribute("loading", loading = true);
		System.out.println("add:" + liveCount.get());
		try {
			if (loadTime == null || loadTime < ajaxPageRefreshTimestamp) {
				response.getWriter()
						.print("{\"ajaxPageRefreshTimestamp\":"
								+ ajaxPageRefreshTimestamp
								+ ",\"code\":5,\"message\":\"无法检测到正确的客户端信息，请刷新，并确保打开了Cookie。\"}");
				return;
			}
			// TODO !!! 拦截非法访问
			if (liveCount.get() > maxActivePushAjaxConnection * 1.3
					|| (liveCount.get() > maxActivePushAjaxConnection && Math
							.random() > 0.3)) {
				// 我想，如果负载过高的时候，不要全部拒绝会挺好都，随机让一些通过，这样能够保证可用度，所以超过负载的时候，仍然有30%的概率通过
				// 但是不能超过1.3倍
				response.getWriter()
						.print("{\"code\":4,\"message\":\"服务器已经达到连接上限，为了服务稳定，不再接受更多链接，请稍后再试。\"}");
				return;
			}
			long since = 1;
			if (loadTime != null
					&& Math.abs(loadTime - new Date().getTime()) < 1000) {
				lSince = 1L;// 1s内刷新的页面，应该让它从新换得全部的数据 
			}
			if ("token".equals(request.getParameter("token"))) {// TODO
																// 只有符合规定的情况下，才信任get来都since
				try {
					String str = request.getParameter("since");
					if (str != null && !"".equals(str))
						since = Long.parseLong(str);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (lSince != null) {
				since = lSince;
			}
			JSONObject jsonResult = new JSONObject();
			String result = "";
			try {
				StatusCacheUnion cache = StatusCacheUnion.getInstance(true);
				synchronized (cache) {
					if (cache.getState() == State.NEW) {
						System.out.println("start cache thread.");
						cache.start();
						// System.out.println("cache thread started. sleep 2s wait for cache.");
						// try {
						// Thread.sleep(2000);//因为缓存刚刚启动，所以呢，等缓存加载完成后才继续，以保证性能。
						// } catch (InterruptedException e) {
						// e.printStackTrace();
						// }
						// System.out.println("weak up from sleep 2s.");
					}
				}
				// 若请求时间太长，超越缓存，则从数据库里面完整读取，希望这样都事情尽可能少
				jsonResult.put("ajaxPageRefreshTimestamp",
						ajaxPageRefreshTimestamp);
				jsonResult.put("code", 1);
				jsonResult.put("message", "System error, empty.");
				if (since < cache.getOldestTimestamp()) {// ||true) {since=0;
					// 从数据库裸查询
					dbRequestCount.add();
					try {
						jsonResult.put("status", getStatusArray(since,false));
						jsonResult.put("statusHeader", getStatusHeader());
						jsonResult.put("code", 0);
						jsonResult.put("message",
								"Status Fecthed Successfully (db).");
						lSince=getLastUpdateTime();
						session.setAttribute("since", lSince);
						lastGetStatus = new Date().getTime();
						session.setAttribute("lastGetStatus", lastGetStatus);
					} catch (SQLException e) {
						jsonResult.put("code", 2);
						jsonResult.put("message",
								"SQLException:" + e.getMessage());
						e.printStackTrace();
					}
				} else {
					// 从缓存里面查
					cacheRequestCount.add();
					try {
						System.out.println("fetch or wait for cache");
						int orgInterval = cache.getInterval();
						int maxCacheWaitingTime = pushAjaxInterval;
						if (since == 1) {// 走到这里，必然时间戳时间在缓存中，如果是1，那么还是早早地返回比较好，否则等20s不太好。
							maxCacheWaitingTime = 500;
						}
						if(longConnection)
						synchronized (cache) {
							while (cache.getInterval() == orgInterval)
								cache.wait(maxCacheWaitingTime);// 等待通知，两种情况退出：1.时间到；2.有新通知
						}
						System.out.println("cache waiting ended.");
						lSince=cache.getLastTimestamp();
						session.setAttribute("since", lSince);
						lastGetStatus = new Date().getTime();
						session.setAttribute("lastGetStatus", lastGetStatus);
						result = cache.getJSONCache();
						if (result.length() <= 50) {
							System.out
									.println("json cache is empty, wait 1500ms.");
							Thread.sleep(1500);// 不知道为什么，上面的cache.wait在第一次的时候会自动跳过，
							// 原因见java doc:A thread can also wake up without
							// being notified, interrupted, or timing out, a
							// so-called spurious wakeup.
							// 已经修正了，我想这个问题应该不会出现了，这段代码应该变成死代码，不过记录我的学习历程，留下。
							System.out.println("weakup from 1500ms sleeping.");
							jsonResult.put("status", cache.getStatusCache());// 这个过程也是挺费时间的，而且都是一样的，我想缓存它
							jsonResult.put("statusHeader", getStatusHeader());
							jsonResult.put("code", 0);
							jsonResult
									.put("message",
											"Status Fecthed Successfully. (no json cache) ");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						jsonResult.put("code", 3);
						jsonResult.put("message",
								"InterruptedException:" + e.getMessage());
					}
				}
				if (result.length() <= 100) {
					response.getWriter().print(jsonResult.toString());
				} else {
					response.getWriter().print(result);
				}
			} catch (JSONException e1) {
				e1.printStackTrace();
				response.getWriter().print("{\"code\":-1}");// 致命JSON挂掉了错误
			}
		}catch(Exception e){
		} finally {
			session.setAttribute("loading", loading = false);
			System.out.println("dec:" + liveCount.get());
		}
	}
}
