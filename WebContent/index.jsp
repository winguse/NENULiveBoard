<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="java.util.Date,cn.edu.nenu.acm.board.frontend.model.*"%><%
final long LOAD_TIME;
session.setAttribute("loadTime",LOAD_TIME=new Long(new Date().getTime()));
session.removeAttribute("since");
//TODO 要不要判断频繁刷新的问题？

%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="author" content="Winguse" />
<title>2012 ACM-ICPC Asia Regional Contest (Changchun Site) On Site Realtime Ranking</title>
<link rel="stylesheet" type="text/css" href="style/board.css" title="default" />
</head>
<body>
<div id="message">
	<br/><h3>o(∩_∩)o</h3><p>One moment, Loading...</p><br/>
</div>
<div id="board">
</div>
<div id="movingTeamInfo">
	<div class="teamName">
		<h3>
			<b class="teamEnglishName">Team English Name</b><br/>
			<small class="teamChineseName">Team Chinese Name</small>
		</h3>
		<p>
		Rank: <!--<span class="fromRank">123</span> ➫ --><span class="toRank">56</span>
		</p>
	</div>
	<div class="teamDetial">
		<dl>
			<dt>School:</dt><dd class="school">School English Name<br/><small>School Chinese Name</small></dd>
			<dt>Coach:</dt><dd class="coach">Coach English Name <small>Coach Chinese Name</small></dd>
			<dt>Team Members:</dt><dd class="teamMembers">
			<ol>
				<li>TeamMember Name <small>TeamMember Name</small></li>
				<li>TeamMember Name <small>TeamMember Name</small></li>
				<li>TeamMember Name <small>TeamMember Name</small></li>
			</ol>
			</dd>
		</dl>
	</div>
	<p align="center">
		<input class="button" type="button" value="Close" onclick="board.closeTeamInfo();"/>
	</p>
</div>
<div id="setttingButton"><small>&copy;2012 by <a href="http://winguse.com">wingus<i title="For舒啸(兔子)">e</i></a>, <a href="http://acm.nenu.edu.cn">nenu acm</a>. good luck to all contestants!</small><span title="Board Settings" onclick="$('#settings').fadeIn(500)">✌</span></div>
<div id="settings">
	<h3>Board Display Settings</h3>
	<p>	
		<label for="animateOn">Board Animate On:</label>
		<input id="animateOn" type="checkbox" onchange="board.animateOn=this.checked"/>
	</p>
	<p>	
		<label for="autoScroll">Auto Scroll On:</label>
		<input id="autoScroll" type="checkbox" onchange="board.autoScroll=this.checked"/>
	</p>
	<p>	
		<label for="scrollFollowing">Scroll Follow Moving Team:</label>
		<input id="scrollFollowing" type="checkbox" onchange="board.scrollFollow=this.checked"/>
	</p>
	<p>	
		<label for="autoToggleDisplayName">Auto Toggle DisplayName:</label>
		<input id="autoToggleDisplayName" type="checkbox" onchange="board.autoToggleDisplayName=this.checked"/>
	</p>
	<p>
		<label for="toggleDisplayName">Toggle Display Name Now:</label>
		<input id="toggleDisplayName" type="button" value="Toggle" onclick="board.toggleDisplayName()"/>
	</p>
	<p>
		<label for="setLongConnection">Enable Long Connection:</label>
		<input id="setLongConnection" type="checkbox" onchange="board.setLongConnection(this.checked)"/>
	</p>
	<small>&copy;2012 By <a href="http://winguse.com">Wingus<span title="iloveshuxiao">e</span></a>, <a href="http://acm.nenu.edu.cn">NENU ACM</a>. Good Luck to All Contestants! Enjoy it.</small>
	<p align="center">
		<input id="settingClose" class="button" type="button" value="Close Settings Dialog" onclick="$('#settings').fadeOut(1000)"/>
	</p>
</div>
<script type="text/javascript" src="js/jquery.min.js"></script>
<script type="text/javascript" src="js/boardinfo.js"></script>
<script type="text/javascript">
var pc2_teams=<%=TeamCache.getInstance().getTeams()%>,pc2_problems=<%=ProblemCache.getInstance().getProblems()%>;
var due_ugly={};
var ugly_pc2_teams={};
</script>
<script type="text/javascript" src="js/board.js"></script>
<script type="text/javascript">
var LOAD_TIME=<%=LOAD_TIME%>;
var board=new Board();
<%
String login=(String) request.getSession().getAttribute("login");
if(login!=null){%>
board.scrollFollow=false;
board.autoScroll=false;
board.autoToggleDisplayName=false;
$team=$("#board>.line");
//$problem=$(".problem");
$team.click(function(){
	var tid=this.id.replace(/team_(\d+)/,"$1");
	//var pid=this.id.replace(/teamProblem_(\d+)_(\d+)/,"$2");//TODO ??
	$.post("ClearPedding",{tid:tid},function(d){
		if(d.code==0){
			
		}else{
			ShowMessage("好像出了点小毛病...",5000);
		}
	},"json");
});
<%}else{%>
$(".cell-big").click(function(){
	var tid=this.id.replace(/name(_\d+)/,"$1");
	board.showTeamInfo(tid);
});
<%}%>
board.update();
</script>
<script type="text/javascript">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-7801267-8']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();
</script>
</body>
</html>
<!-- winguse love shuxiao. -->