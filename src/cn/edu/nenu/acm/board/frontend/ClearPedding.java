package cn.edu.nenu.acm.board.frontend;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.edu.nenu.acm.board.Board;
import cn.edu.nenu.acm.board.frontend.model.DBStatusFetcher;

/**
 * Servlet implementation class ClearPedding
 */
@WebServlet(
		urlPatterns = { "/ClearPedding" }, 
		initParams = { 
				@WebInitParam(name = "tid", value = "", description = "Team ID In Database")
		})
public class ClearPedding extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClearPedding() {
        super();
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		response.setCharacterEncoding("utf-8");
		response.setContentType("application/json");
		String result="{\"code\":1}";
		String login=(String) request.getSession().getAttribute("login");
		String rTId=request.getParameter("tid"); 
		String rPId=request.getParameter("pid"); 
		String reload=request.getParameter("reload"); 
		if(login==null){
			result="{\"code\":2,\"message\":\"您还没登录……囧。\"}";
		}else if("true".equals(reload)){
			try {
				Connection conn=Board.getDataBaseConnection();
				PreparedStatement pstm;
				pstm = conn.prepareStatement("UPDATE Runs SET rDescription='',rLastUpdateTime=? WHERE rDescription='F'");
				pstm.setLong(1, DBStatusFetcher.getLastUpdateTime()+Board.statusCacheInterval*10);
				pstm.execute();
				pstm.close();
				conn.close();
				result="{\"code\":0}";
			} catch (SQLException e) {
				result="{\"code\":4,\"message\":\"SQL 错误。\"}";
				e.printStackTrace();
			}
		}else if(rTId==null&&rPId==null){
			result="{\"code\":3,\"message\":\"你提交了个错数据："+rTId+"\"}";
		}else if(rPId==null){
			int tid=0;
			try{
				tid=Integer.parseInt(rTId);
			}catch(Exception e){
				
			}
			Connection conn;
			try {
				conn = Board.getDataBaseConnection();
				PreparedStatement pstm=conn.prepareStatement(
						"SELECT rId,rStatus,rDescription,rLastUpdateTime,rPId FROM Runs WHERE rTId=? AND rTime>=? AND rDescription!=? ORDER BY rPId ASC",
						ResultSet.TYPE_SCROLL_SENSITIVE,  
                        ResultSet.CONCUR_UPDATABLE);
				pstm.setInt(1, tid);
				pstm.setInt(2, Board.freezeTime);
				pstm.setString(3, "F");
				pstm.execute();
				ResultSet rs=pstm.getResultSet();
				long updateTime=DBStatusFetcher.getLastUpdateTime()+Board.statusCacheInterval*10;
				while(rs.next()){
					int status=rs.getInt("rStatus");
					rs.updateString("rDescription", "F");
					rs.updateLong("rLastUpdateTime",updateTime);
					rs.updateRow();
					if(status==Board.RUNSTATUS_YES){
						int pid=rs.getInt("rPId");
						while(rs.next()){
							if(pid!=rs.getInt("rPId"))break;
							rs.updateString("rDescription", "F");
							rs.updateLong("rLastUpdateTime",updateTime);
							rs.updateRow();
						}
						break;
					}
				}
				rs.close();
				pstm.close();
				conn.close();
				result="{\"code\":0}";
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}else{
			try {
				int tid=0,pid=0;
				try{
					tid=Integer.parseInt(rTId);
					pid=Integer.parseInt(rPId);//TODO
				}catch(Exception e){}
				Connection conn=Board.getDataBaseConnection();
				PreparedStatement pstm=conn.prepareStatement("UPDATE Runs SET rDescription=?,rLastUpdateTime=? WHERE rTId=? AND rPId=? AND rTime>=?");
				pstm.setString(1, "F");//标记为Final，跳过Pedding缓存处理
				pstm.setLong(2, DBStatusFetcher.getLastUpdateTime()+Board.statusCacheInterval*10);//标记更新时间戳，随便制定一个10倍之后的。实际上，现在系统已经和时钟没关系了，所以随便就可以。
				pstm.setInt(3, tid);
				pstm.setInt(4, pid);
				pstm.setInt(5, Board.freezeTime);
				pstm.execute();
				pstm.close();
				conn.close();
				result="{\"code\":0}";
			} catch (SQLException e) {
				e.printStackTrace();
				result="{\"code\":4,\"message\":\"SQL 错误。\"}";
			} catch(Exception e){
				result="{\"code\":3,\"message\":\"你提交了个错数据："+rTId+"\"}";
			}
		}
		response.getWriter().print(result);
	}

}
