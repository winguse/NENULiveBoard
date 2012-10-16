package cn.edu.nenu.acm.board.backend;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;

import cn.edu.nenu.acm.board.Board;

import edu.csus.ecs.pc2.api.IContest;
import edu.csus.ecs.pc2.api.IProblem;
import edu.csus.ecs.pc2.api.IRun;
import edu.csus.ecs.pc2.api.ITeam;
import edu.csus.ecs.pc2.api.ServerConnection;
import edu.csus.ecs.pc2.api.exceptions.LoginFailureException;
import edu.csus.ecs.pc2.api.exceptions.NotLoggedInException;
import edu.csus.ecs.pc2.api.listener.IRunEventListener;

import static cn.edu.nenu.acm.board.Board.*;

/**
 * 要求，比赛过程中，题目名、队伍displayName都不能被修改，否则数据就会不一致。允许添加新题目，允许添加新队伍。
 * 新题目无所谓，新队伍都话，本程序内部存的可能是错误的与学校对应关系，但是这个 无关紧要，重要都是要在去修正数据库里面队伍的学校外键，这样前端展示的时候就不
 * 会有问题。
 * 
 * 遇到致命错误，可以新terminate，然后重新begin，这样会清空之前的数据，重新全部获取。如果需要，注意备份。
 * 
 * 关于FirstBlood的问题，由于考虑一旦rejudge的话，要更新的面积太大了，然后listener
 * 是没有办法得知是不是开始某个题目rejudge的，如果每次都检查全部的runs，显然性能太差，所以这部分功能扔给前提js搞。精简后端功能，保证稳定性。
 * 
 * @author winguse
 * @version 2012-10-3
 */
public class RunsListener implements IRunEventListener, IRunListenerControler {

	protected boolean begined = false;

	protected String login = "scoreboard1";
	protected String password = "sbacmvic";

	protected PrintStream out = null;
	protected Connection conn = null;

	protected long bvBeginFetchTime = 0;
	protected long bvLastUpdateTime = 0;
	protected String description = "";

	protected ServerConnection serverConnection = null;
	protected IContest contest = null;

	// 通过队伍DisplayName分析来获得在数据库里面队伍对应都学校Id，考虑这个不是没次都变化的
	protected HashMap<String, Integer> problems = null;
	protected HashMap<String, Integer> teams = null;

	public RunsListener() throws SQLException {
		out = System.out;
		conn = Board.getDataBaseConnection();
	}

	public RunsListener(PrintStream out) throws SQLException {
		this.out = out;
		conn = Board.getDataBaseConnection();
	}

	/**
	 * 获得一个题目在数据库里面都ID，如果没有，就新建一个 IMPORTANT：比赛过程中，不允许修改题目都problemName
	 * 
	 * @param problemName
	 *            题目名
	 * @return 题目的ID
	 */
	protected int getProblemId(String problemName) {
		int problemId = 0;
		if (problems.containsKey(problemName)) {
			problemId = problems.get(problemName);
		} else {
			try {
				PreparedStatement pstat = null;
				pstat = conn.prepareStatement(
						"INSERT INTO Problems(pName) VALUES(?)",
						Statement.RETURN_GENERATED_KEYS);
				pstat.setString(1, problemName);
				pstat.execute();
				ResultSet rs = pstat.getGeneratedKeys();
				if (rs.next()) {
					problems.put(problemName, problemId = rs.getInt(1));
				} else {
					throw new Exception("奇迹啊！怎么可能插入了没有新ID呢？@problemId");
				}
				pstat.close();
			} catch (SQLException e) {
				e.printStackTrace();
				out.println("数据库错误：" + e.getMessage());
			} catch (Exception e) {
				out.println("其他错误：" + e.getMessage());
				e.printStackTrace();
			}
		}
		return problemId;
	}

	/**
	 * 获得一个队伍，在数据库里面都ID，没有的话，就插入一个 IMPORTANT：比赛过程中，不允许修改队伍的displayname
	 * 
	 * @param t
	 *            ITeam对象，队伍的信息
	 * @return 队伍都ID
	 */
	protected int getTeamId(ITeam t) {
		int teamId = 0;
		String displayName = t.getDisplayName();
		if (teams.containsKey(displayName)) {
			teamId = teams.get(displayName);
		} else {
			try {
				PreparedStatement pstat = null;
				pstat = conn
						.prepareStatement(
								"INSERT INTO Teams(tAccountNumber,tDisplayName,"
										+ "tLoginName,tSiteNumber,tIsDisplayableOnScoreboard,tDescription)"
										+ "VALUES(?,?,?,?,?,?)",
								Statement.RETURN_GENERATED_KEYS);
				pstat.setInt(1, t.getAccountNumber());
				pstat.setString(2, t.getDisplayName());
				pstat.setString(3, t.getLoginName());
				pstat.setInt(4, t.getSiteNumber());
				pstat.setBoolean(5, t.isDisplayableOnScoreboard());
				pstat.setString(6, "");// 描述信息
				pstat.execute();
				ResultSet rs = pstat.getGeneratedKeys();
				if (rs.next()) {
					teams.put(displayName, teamId = rs.getInt(1));
				} else {
					throw new Exception("奇迹啊！怎么可能插入了没有新ID呢？@teamId");
				}
				pstat.close();
			} catch (SQLException e) {
				e.printStackTrace();
				out.println("数据库错误：" + e.getMessage());
			} catch (Exception e) {
				out.println("其他错误：" + e.getMessage());
				e.printStackTrace();
			}
		}
		return teamId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.edu.nenu.acm.board.IRunListenerControler#begin()
	 */
	@Override
	public synchronized void begin() {
		if (begined)
			return;
		begined = true;
		problems = new HashMap<String, Integer>();
		teams = new HashMap<String, Integer>();
		try {
			serverConnection = new ServerConnection();
			contest = serverConnection.login(login, password);
			bvBeginFetchTime = bvLastUpdateTime = new Date().getTime();
			Statement stat = conn.createStatement();
			// 重置数据
			stat.execute("DELETE FROM Runs");
			stat.execute("DELETE FROM Teams");
			stat.execute("DELETE FROM Problems");
			stat.close();
			// 拉取题目信息
			for (IProblem p : contest.getProblems()) {
				getProblemId(p.getName());
			}
			// 拉取队伍信息
			for (ITeam t : contest.getTeams()) {
				getTeamId(t);
			}
			// 拉取已有的Run
			for (IRun r : contest.getRuns()) {
				updateRun(r);
			}
			contest.addRunListener(this);
			out.println("Board is listening...");
			// 上面执行了这么多，有点担心数据同步的问题（例如我还没把队伍信息插入完，但是这个时候队伍提交了题目，而我现在又没有把listener绑定）
			// 上述情况，在比赛中时，使用这个可能会出现，如果一开始就监听的话，不存在，不过考虑不稳定因素，这个的确很难说。
		} catch (LoginFailureException e) {
			out.println("无法登陆PC^2：\n" + e.getMessage());
			begined = false;
		} catch (SQLException e) {
			out.println("数据库错误：" + e.getMessage());
			begined = false;
			e.printStackTrace();
		} catch (Exception e) {
			out.println("捕获其他错误：" + e.getMessage());
			begined = false;
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.edu.nenu.acm.board.IRunListenerControler#terminate()
	 */
	@Override
	public synchronized void terminate() {
		out.println("Trying to terminate...");
		try {
			begined = false;
			if (conn != null && !conn.isClosed())
				conn.close();
			if (contest != null)
				contest.removeRunListener(this);
			if (serverConnection != null && serverConnection.isLoggedIn())
				serverConnection.logoff();
			problems = null;
			teams = null;
			contest = null;
			serverConnection = null;
			conn = null;
			out.println("Terminated.");
		} catch (NotLoggedInException e) {
			out.println("结束：尚未登录！");
			e.printStackTrace();
		} catch (SQLException e) {
			out.println("捕获其他错误：" + e.getMessage());
			e.printStackTrace();
		}
	}

	protected synchronized void updateRun(IRun r) {
		//其实这里面生成的rId是多余的，本来IRun里面的number就已经可以作为ID用了。
		if (!begined) {
			out.println("Not Begined, but runs arrived.");
			return;
		}
		try {
			Date now = new Date();
			bvLastUpdateTime = now.getTime();
			out.print(now + "\t");
			int teamId = getTeamId(r.getTeam()), problemId = getProblemId(r
					.getProblem().getName())/* , firstBloodRunNumber */, oldStatus = RUNSTATUS_UNDEFINE;
			PreparedStatement pstat = null;
			String sql = null;
			pstat = conn
					.prepareStatement("SELECT rNumber,rStatus FROM Runs WHERE rTId=? AND rPId=? AND rNumber=?");
			pstat.setInt(1, teamId);
			pstat.setInt(2, problemId);
			pstat.setInt(3, r.getNumber());
			pstat.execute();
			ResultSet rs = pstat.getResultSet();
			if (rs.next()) {
				oldStatus = rs.getInt("rStatus");
				out.print("UPDATE:\t");
				sql = "UPDATE Runs SET rLanguage=?,rStatus=?,rTime=?,rJudgementName=?,rDescription=?,rLastUpdateTime=? WHERE rTId=? AND rPId=? AND rNumber=?";
			} else {
				out.print("   New:\t");
				sql = "INSERT INTO Runs(rLanguage,rStatus,rTime,rJudgementName,rDescription,rLastUpdateTime,rTId,rPId,rNumber) VALUES(?,?,?,?,?,?,?,?,?)";
			}
			rs.close();
			pstat = conn.prepareStatement(sql);
			pstat.setString(1, r.getLanguage().getName());
			out.print(r.getNumber() + "\t" + r.getLanguage().getName() + "\t");
			// if(oldStatus==RUNSTATUS_FIRST_BLOOD){//要更新都对象竟然是FB，那么我应该先吧FB都那个标记量删掉。这种情况rejudged的情况
			// firstBloodRunNumbers.put(problemId, Integer.MAX_VALUE);
			// }
			if (r.isDeleted()) {
				pstat.setInt(2, RUNSTATUS_DELETED);
				out.print("DELETE\t");
			} else if (r.isFinalJudged()) {
				if (r.isSolved()) {
					// firstBloodRunNumber =
					// firstBloodRunNumbers.get(problemId);
					// if (r.getNumber() <= firstBloodRunNumber) {//
					// RUNSTATUS_FIRST_BLOOD
					// firstBloodRunNumbers.put(problemId, r.getNumber());
					// pstat.setInt(2, RUNSTATUS_FIRST_BLOOD);
					// out.print("FIRST_BLOOD\t");
					// conn.createStatement().execute(
					// "UPDATE Runs SET rStatus=" + RUNSTATUS_YES
					// + " WHERE rNumber="
					// + firstBloodRunNumber);
					// } else {
					out.print("YES\t");
					pstat.setInt(2, RUNSTATUS_YES);
					// }
				} else {
					out.print("NO\t");
					pstat.setInt(2, RUNSTATUS_NO);
				}
			} else {
				out.print("PEDDING\t");
				pstat.setInt(2, RUNSTATUS_PEDDING);
			}
			out.print(r.getSubmissionTime() + "\t" + r.getJudgementName()
					+ "\t" + r.getTeam().getDisplayName() + "[" + teamId
					+ "]\t" + r.getProblem().getName() + "[" + problemId
					+ "]\n");
			pstat.setLong(3, r.getSubmissionTime());
			pstat.setString(4, r.getJudgementName());
			pstat.setString(5, "");
			pstat.setLong(6, bvLastUpdateTime);
			pstat.setInt(7, teamId);
			pstat.setInt(8, problemId);
			pstat.setInt(9, r.getNumber());
			pstat.execute();
			pstat.close();
		} catch (SQLException e) {
			out.println("数据库错误。" + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Invoked when a run has been checked by a judge.
	 * 
	 * @param run
	 *            -
	 * @param isFinal
	 *            - true if this is a action for a final Judgement.
	 * */
	@Override
	public void runCheckedOut(IRun arg0, boolean arg1) {
		// do nothing
	}

	@Override
	public void runCompiling(IRun arg0, boolean arg1) {
		// do nothing
	}

	@Override
	public void runDeleted(IRun arg0) {
		// do nothing
	}

	@Override
	public void runExecuting(IRun arg0, boolean arg1) {
		// do nothing
	}

	/**
	 * Invoked when an existing run has been judged; that is, has had a
	 * Judgement applied to it. Typically this happens when a judge assigns a
	 * Judgement, but it can also occur as a result of an "automated judgement"
	 * (also known as a "validator") applying a judgement automatically.
	 * 
	 * @param run
	 *            - the judged IRun
	 * @param isFinal
	 *            - true if this is a action for a final Judgement.
	 * */
	@Override
	public void runJudged(IRun arg0, boolean arg1) {
		updateRun(arg0);
	}

	@Override
	public void runJudgingCanceled(IRun arg0, boolean arg1) {
		// do nothing
	}

	/**
	 * Invoked when a new run has been added to the contest. Typically this
	 * means that a run has been submitted by a team; it may also may be caused
	 * by a remote server in a multi-site contest sending its run(s) to the
	 * local server. The added run may have been originally entered into the
	 * contest on either the local server (the server to which this client is
	 * connected) or on a remote server.
	 * 
	 * @see edu.csus.ecs.pc2.api.listener.IRunEventListener#runSubmitted(edu.csus.ecs.pc2.api.IRun)
	 * @param run
	 *            - the IRun that has been added to the contest
	 */
	@Override
	public void runSubmitted(IRun arg0) {
		updateRun(arg0);
	}

	@Override
	public void runUpdated(IRun arg0, boolean arg1) {
		updateRun(arg0);
	}

	@Override
	public void runValidating(IRun arg0, boolean arg1) {
		// do nothing
	}

	@Override
	public long getBvBeginFetchTime() {
		return bvBeginFetchTime;
	}

	@Override
	public long getBvLastUpdateTime() {
		return bvLastUpdateTime;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean isBegined() {
		return begined;
	}

}
