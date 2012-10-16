/**
 * 我写习惯的js对象风格是构造函数式的，相对来说，是相当费内存的。
 * 不过Prototype的方式，对私有变量的控制不好，我不太习惯——并且写起来也不优美。
 * ©2012 Winguse, NENU ACM. All Rights Reserved.
 * @author Winguse
 */
"use strict";
var PER_PENALTY = 20,ANIMATE_TIME=5000,MAX_ANIMATE_QUE=15,GOLD=17,SLIVER=52,BRONZE=103;
/**
 * Status
 * 状态宏定义，话说到现在，我都不知道怎样在js里面写静态变量，有同学告诉我么？答案：写到prototype里面，虽然不是常量，但是是静态，但是是全局的。
 */
var RUNSTATUS_YES = 0, RUNSTATUS_FIRST_BLOOD = 1, RUNSTATUS_PEDDING = 2, RUNSTATUS_NO = 3, RUNSTATUS_DELETED = -1, RUNSTATUS_UNDEFINE = -2;
/**
 * Status 对象
 * 
 * @param arrData
 *            服务器传来的status数组
 * @param statusHeader
 *            服务器传来的数组下标
 */
function Status(arrData, statusHeader) {
	this.rid = arrData[statusHeader.rNumber];// 由于后台自动生成了一个rId，这个东西实质上是数据库上的ID，用pc2本身的就够了，后台多此一举，懒得改了。
	this.tid = "_"+arrData[statusHeader.rTId];
	this.time = arrData[statusHeader.rTime];
	this.pid = "_"+arrData[statusHeader.rPId];
	this.status = arrData[statusHeader.rStatus];
	this.judgement = arrData[statusHeader.rJudgementName];
	this.language = arrData[statusHeader.rLanguage];
	this.description = arrData[statusHeader.rDescription];
	this.lastUpdateTime = arrData[statusHeader.rLastUpdateTime];
}

/**
 * Problem构造函数，两个用途，一个是作为一个队伍而言，判断这个队伍过了多少，<br>
 * 另外一个嘛，全场统计是不是firstBlood之类的（但是显然，有些功能就不是很好了，好像变成了整场一个队伍一样）。
 * 
 * @param pname
 *            题目名称，期待是A，B，C，D...
 * @param pid
 *            题目的ID，数据库里面的
 * @param tid
 *            TeamID，队伍的ID，数据库里面的
 */
function Problem(name, pid, tid) {
	this.arrStatus = new Array();
	this.penalty = 0;
	this.acceptedTime = -1;
	this.acceptedRid = -1;
	this.acceptedTid="";//只在全局里面用，用来标记一下到底是那个队伍FB了
	this.yes = 0;
	this.no = 0;
	this.all = 0;
	this.name = name;
	this.pid = pid;
	this.tid = tid;
}
/**
 * 这个是题目ID到根据题目displayname（通常期待是A，B，C，D...）的快速映射，在第一次获得ProblemList的时候更新，全局通用。
 */
Problem.prototype.id2pos = null;

/**
 * 全局题目列表，负责管理统计信息，判断FirstBlood之类
 */
Problem.prototype.staticList=getProblemList();
/**
 * 更新一个状态，例如从服务器传来了新的status
 * 
 * @param status
 *            Status对象，注意服务器为了压缩传来的数组要先处理一下
 */
Problem.prototype.updateStatus = function(status) {// ??这里是不是要弄个防多线程数据一致性的问题？——写后台多线程时写疯了。
//	assert(status instanceof Status);
//	assert(this.pid == status.pid);// 发现非法
	if (status.status == RUNSTATUS_YES) {
		this.yes++;
	} else if (status.status == RUNSTATUS_NO) {
		this.no++;
	}
	this.all++;
	var updated = true, found = false;
	for ( var i = 0; i < this.arrStatus.length; i++) {
		if (this.arrStatus[i].rid == status.rid) {// 找到了
			found = true;
			if (this.arrStatus[i].lastUpdateTime < status.lastUpdateTime) {
				if (this.arrStatus[i].status == RUNSTATUS_YES) {
					this.yes--;
				} else if (this.arrStatus[i].status == RUNSTATUS_NO) {
					this.no--;
				}
				this.all--;
				this.arrStatus[i] = status;// 有更新，替换之。
			} else {
				updated = false;// 没有更新，不需要更新这个题目的信息。
			}
			break;
		}
	}
	if (!found) {// 没找到，插入之
		this.arrStatus.push(status);
	}
	if (updated) {
		this.arrStatus.sort(function(a, b) {// 很暴力，直接全部重新排序，我不信你一个队伍交了好几个数量级，一般来说现场赛status不能超过3k的，不过这次长春站180队啊……不过还是复杂度强迫症。
			if(a.status==RUNSTATUS_DELETED)return 1;//把删除的排到最后
			if(b.status==RUNSTATUS_DELETED)return -1;//把删除的排到最后
			return a.rid - b.rid;//不能用time，应该用runID
		});
		while(this.arrStatus.length>0&&this.arrStatus[this.arrStatus.length-1].status==RUNSTATUS_DELETED){
			this.arrStatus.pop();
			this.all--;
		}
		this.penalty = 0;
		this.acceptedTime = -1;// 看上去似乎这个没必要赋初值，事实上，很有可能rejudge的，所以，搞起吧。
		var triedCount=0,s = {};// 显然eclipse又卖萌了，说这个没有初始化，手贱多写写呗。
		for ( var i = 0; i < this.arrStatus.length; i++) {
			s = this.arrStatus[i];
			triedCount++;
			if (
					this.tid == null//当前是全局problemlist量
				&&	!board.teams[s.tid].offical//要更新的那货却来自酱油队
				)continue;//酱油队放开那个first blood之外，但是你也得帮着统计数据。
			//TODO 注意，board的是全局变量，囧爆了。我想念Java的关系依赖注入了。
			if (s.status == RUNSTATUS_YES) {
				this.acceptedTime = s.time;
				this.acceptedRid = s.rid;
				if(i != this.arrStatus.length){
//					ShowMessage(pc2_teams.teams[this.tid]+" 调戏裁判！",10000);
				}
				break;// 看上去似乎也不必要，但是有一个奇葩的情况——那个队伍明明已经过了，还继续交，小朋友，你说调戏裁判这种事情，给他们罚时好不好？呵呵。
			} else if (s.status == RUNSTATUS_NO) {
				this.penalty += PER_PENALTY;
			}
		}
		if (this.tid == null) {// 更新全局统计信息
			if(
					this.acceptedTid != s.tid&&s.status == RUNSTATUS_YES
				&&	board.teams[s.tid].offical	//TODO 访问了全局变量 同上，酱油队你要放开firstblood但是要统计数据！
				){//FirstBood 发生改变了
				$("#teamProblem"+this.acceptedTid+this.pid).removeClass("firstblood");//原来的去掉这个属性
				this.acceptedTid = s.tid;//First Blood处理部分
				$("#teamProblem"+this.acceptedTid+this.pid).addClass("firstblood");//增加这个属性
			}
			var $problemStatistic=$("#problemStatistic"+this.pid);
			$problemStatistic.html("<span class='yescount'>"+this.yes+"</span><span style='color:#fff'>/</span><span class='allcount'>"+this.all+"</span>");
			$problemStatistic.attr("title",
					"Yes:"+this.yes+", No:"+this.no+", All:"+this.all+
					", Pedding:"+(this.all-this.no-this.yes)+
					"\nFirst Blood Time:"+this.acceptedTime+
					"\nRate: "+Math.round(this.yes/this.all*100)+"%."
			);
			var color=128;
			if(this.all>0)color=128-128*this.yes/this.all;
			$problemStatistic.css("background-color","rgb("+color+","+color+","+color+")");
		} else {// 更新 TODO 处理刚刚初始化，HTML没有生成的时候的情况。
			var $teamProblem=$("#teamProblem"+this.tid+this.pid),descriptionString=" (Tried / AC Time)";
			$teamProblem.removeClass("pedding yes no");
			if (s.status == RUNSTATUS_YES) {
				$teamProblem.addClass("yes");
				$teamProblem.attr("title","Yes"+descriptionString);
				$teamProblem.text(triedCount+"/"+this.acceptedTime);
			} else if (s.status == RUNSTATUS_NO) {
				$teamProblem.addClass("no");
				$teamProblem.attr("title","No"+descriptionString);
				$teamProblem.text(triedCount+"/--");
			} else if (s.status == RUNSTATUS_PEDDING) {
				$teamProblem.addClass("pedding");
				$teamProblem.attr("title","Pedding"+descriptionString);
				$teamProblem.text(triedCount+"/--");
			}else{//如果是delete的话，s是删掉的，没办法被读取出来的，如果有别的状态，那么算别的，否则就是空的
				$teamProblem.attr("title","");
				$teamProblem.text("");
			}
		}
	}
};
/**
 * 返回罚时信息
 */
Problem.prototype.getPenalty = function() {
	return this.acceptedTime == -1 ? 0 : this.penalty;
};
/**
 * 返回是否通过
 */
Problem.prototype.isAccepted = function() {
	return this.acceptedTime != -1;
};

/**
 * 题目列表函数，这里面，性能问题没考虑，实际上，不应该没次都读取和排序的，直接复制就行，唉，复杂度强迫症。
 * 
 * @param tid
 *            队伍ID
 */
function getProblemList(tid) {
	var problemList = new Array();
	for ( var pid in pc2_problems.problems) {// 引用全局了
		problemList.push(new Problem(pc2_problems.problems[pid], pid, tid));
	}
	problemList.sort(function(a, b) {
		return a.name.localeCompare(b.name);
	});
	if (Problem.prototype.id2pos == null) {
		Problem.prototype.id2pos = {};
		for ( var i = 0; i < problemList.length; i++) {
			Problem.prototype.id2pos[problemList[i].pid] = i;
		}
	}
	return problemList;
}
/**
 * 队伍信息类
 * 
 * @param team
 *            服务器传来的Contest Service的team信息
 * @param tid
 *            队伍在服务器的ID，用来识别status、网页分析等。
 * @param school
 *            学校信息，Contest Service的school信息
 */
function Team(team, tid, school) {
	this.nextTeam = null;// 双向链表存队伍相对排名
	this.preTeam = null;
	this.tid = tid;
	this.accepted = 0;
	this.penalty = 0;
	this.lastAccptedTime = -1;
	this.submited = false;
	this.girlsTeam = false;
	if (team == null){
		this.teamHTML='<div id="team'+this.tid+'"></div>';
		this.cn=this.tid;
		return;
	}
	this.problemList = getProblemList(this.tid);
	this.school = school;// 学校信息仅仅是引用
	for ( var attribute in team) {
		this[attribute] = team[attribute];
	}
	this.lastRank="";
	this.teamRank = "No Rank";
	this.schoolRank = "";
	var problemHTML = "";
	for ( var i = 0; i < this.problemList.length; i++) {
		var problem = this.problemList[i];
		this.problemCount++;
		problemHTML += '<div class="cell cell-small problem" id="teamProblem'
				+ this.tid + problem.pid + '"></div>';
	}
	var girlsTeam="",girlsTeamTitle="",noneOffical="";
	if(this.girlsTeam){girlsTeam=" girlsTeam";girlsTeamTitle="Girls Team!! [ ❤ MM ❤！ ]\n \n";}
	if(!this.offical)noneOffical="*";
	this.teamHTML =
	/*		*/'<div class="line'+girlsTeam+'" id="team'+this.tid+'">' +
	/*			*/'<div class="cell cell-small" id="rank' + this.tid + '">' +
	/*				*/'<div class="sub-cell" id="teamRank' + this.tid + '">'+noneOffical+'</div>' +
	/*				*/'<div class="sub-cell" id="schoolRank' + this.tid + '">'+noneOffical+'</div>' +
	/*			*/'</div>' +
	/*			*/'<div class="cell cell-big" id="name' + this.tid + '" title="'+ girlsTeamTitle + this.cn.xss() + '\n' + this.en.xss() + '\n		o(∩_∩)o~\n' + this.school.cn.xss() + '\n' + this.school.en.xss() + '">' +
	/*				*/'<div class="sub-cell" id="team' + this.tid + '">' + this.en.xss() + '</div>' +
	/*				*/'<div class="sub-cell" id="school' + this.tid+ '">'+ this.school.en.xss() + '</div>' +
	/*			*/'</div>' +
	/*			*/'<div class="cell cell-small" id="accepted' + this.tid+ '">'+this.accepted+'</div>' +
	/*			*/'<div class="cell cell-small" id="penalty' + this.tid+ '">'+this.penalty+'</div>' +
	/*			*/problemHTML +
	/*		*/'</div>';
}

/**
 * 更新队伍的Status信息
 * 
 * @param status
 *            Status对象，注意服务器为了压缩传来的数组要先处理一下
 */
Team.prototype.updateStatus = function(status) {
//	assert(status.tid == this.tid);
	this.submited = true;// 表示有过提交
	this.problemList[Problem.prototype.id2pos[status.pid]].updateStatus(status);
	// 每一次更新status都要对整个队伍的信息进行更新
	this.accepted = 0;
	this.penalty = 0;
	this.lastAccptedTime = -1;
	for ( var i = 0; i < this.problemList.length; i++) {
		var problem = this.problemList[i];
		if (problem.isAccepted()) {
			this.accepted++;
			if (this.lastAccptedTime < problem.acceptedTime)
				this.lastAccptedTime = problem.acceptedTime;
			this.penalty += problem.getPenalty()+problem.acceptedTime;
		}
	}
	$("#accepted"+this.tid).text(this.accepted);
	$("#penalty"+this.tid).text(this.penalty);
	//this.updatePos();
};
/**
 * 更新单个的Ranking，考虑Rejudge的情况，决定整个榜地扫描。<br>
 * 排序的前提是，除了当前排序的元素之外，其他部分必须已经有序。<br>
 * 当然，这个版本刚刚开始就对名字进行排序，问题不会很大，因为大家的主要排序码，积分的，都还是0.
 */
Team.prototype.updatePos =function(){return;
	var pointer=this.preTeam,updated=true,moveBack=false;
	while(TeamCompare(pointer,this)>0){//TODO COMP
		pointer=pointer.preTeam;
	}
	if(pointer==this.preTeam){//没有长进，检查是不是有退步
		pointer=this.nextTeam;
		while(TeamCompare(pointer,this)<0){
			pointer=pointer.nextTeam;
		}
		if(pointer==this.nextTeam){
			//没有退步，什么也不做
			updated=false;
		}else{
			moveBack=true;
		}//退步了，现在pointer指向的是那个刚刚好比this劣的，将this插到pointer前面，PS退步的情况就是Rejudge
	}else{
		//进步了，插入到pointer后面
		pointer=pointer.nextTeam;//pointer向后移动一个，就插前面去了，跟上面一样
	}
	if(!updated)return null;
	//将this插到pointer前面
//	//	log(this.cn+"  -->  "+pointer.cn);
//	//	log(this.preTeam.cn+" # "+this.nextTeam.cn);
//	//	log(pointer.preTeam.cn+" # "+pointer.nextTeam);
	this.preTeam.nextTeam=this.nextTeam;
	this.nextTeam.preTeam=this.preTeam;
	this.preTeam=pointer.preTeam;
	this.nextTeam=pointer;
	this.preTeam.nextTeam=this;
	pointer.preTeam=this;
	return {fromTid:this.tid,toTid:pointer.tid,moveBack:moveBack};//TODO
};
Board.prototype.showTeamInfo=function(tid){
	var $movingTeamInfo=$("#movingTeamInfo");
	var team=this.teams[tid];
	$movingTeamInfo.fadeIn(500);
	$movingTeamInfo.find(".teamEnglishName").text(team.en);
	$movingTeamInfo.find(".teamChineseName").text(team.cn);
	$movingTeamInfo.find(".fromRank").text(team.lastRank);
	$movingTeamInfo.find(".toRank").text(team.accepted>0?team.teamRank:" -- ");
	$movingTeamInfo.find(".school").html(team.school.en.xss()+"<br/><small>"+team.school.cn.xss()+"</small>");
	$movingTeamInfo.find(".coach").html(getPersonHtml(team.coach));//TODO
	var teamMembersHtml="";
	for(var i in team.teamMembers){
		teamMembersHtml+="<li>"+getPersonHtml(team.teamMembers[i])+"</li>";
	}
	$movingTeamInfo.find(".teamMembers>ol").html(teamMembersHtml);
};
Board.prototype.closeTeamInfo=function(){
	$("#movingTeamInfo").fadeOut(500);
};
/**
 * 移动两个队伍在网页上面的位置
 * @param moveInfo
 * @param noAnimate 没动画
 */
Board.prototype.moveTeam=function (moveInfo,noAnimate){
	var I=this;
	var fromTid=moveInfo.fromTid,toTid=moveInfo.toTid,moveBack=moveInfo.moveBack;
	var $from=$("#team"+fromTid),$to=$("#team"+toTid);
	if(noAnimate){
		$to.before($from);
		I.moveFinshed=true;
	}else{
	//animation
		var fromOffset=$from.offset();
		if(this.scrollFollow&&this.animateOn)
			this.showTeamInfo(fromTid);
		$from.before("<div id='tmp"+fromTid+toTid+"'></div>");
		$to.before("<div id='tmp"+toTid+fromTid+"'></div>");
		var $fromTmp=$("#tmp"+fromTid+toTid),$toTmp=$("#tmp"+toTid+fromTid),height=$to.height();
		var $scroll,delta=$(window).height()/4;
		if($.browser.mozilla||$.browser.msie)
			$scroll=$("html");
		else
			$scroll=$("body");
		$fromTmp.css({height:height+"px"});
		$toTmp.css({height:"0px"});
		var toTop=$toTmp.offset().top;
		if(moveBack)toTop-=height*2;
		$from.addClass("lineonmove");
		$to.before($from);//提前插入也可以了，可以很好解决并发移动的问题
		if(this.scrollFollow){
			$scroll.scrollTop(fromOffset.top-delta);
			if($scroll.scrollTop()>30){
				var oldOffsetTop=fromOffset.top;
				fromOffset.top=$scroll.scrollTop()+delta;
				if(fromOffset.top<0)
					fromOffset.top=oldOffsetTop;
				$scroll.animate({scrollTop:toTop-delta},ANIMATE_TIME);
			}
		}
		$from.offset(fromOffset);
		$fromTmp.animate({height:0},ANIMATE_TIME);
		$from.animate({top:toTop},ANIMATE_TIME,"swing",function(){
			$fromTmp.remove();
			$toTmp.remove();
			$from.removeClass("lineonmove");
			$from.css("top","");
			$from.css("left","");
			setTimeout(function(){
				I.closeTeamInfo();
			},2500);
			setTimeout(function(){
				I.moveFinshed=true;
			},3000);
		});
		$toTmp.animate({height:height},ANIMATE_TIME);
	}
};
/**
 * 队伍排位优先函数
 * 
 * @param a
 *            Team对象
 * @param b
 *            Team对象
 * @returns 负数a优于b，正数b优于a，0则相等——无耻地没让它相等，最后有个队名排序。
 */
function TeamCompare(a, b) {
//	assert(a instanceof Team);
//	assert(b instanceof Team);
	if (a.accepted != b.accepted)// 第一排序关键字：过题数目多的优先
		return a.accepted>b.accepted?-1:1;
	if (a.penalty != b.penalty)// 第二关键字：罚时少的优先
		return a.penalty < b.penalty?-1:1;
	if (a.lastAccptedTime != b.lastAccptedTime)// 第三关键字：最后通过时间早的优先
		return a.lastAccptedTime < b.lastAccptedTime?-1:1;
	if (a.submited != b.submited)// 第四关键字：有提交的优先
		return a.submited ? -1 : 1;
	if(a.girlsTeam!=b.girlsTeam)//第五关键字，女士优先
		return a.girlsTeam?-1:1;
	// return 0;
	return a.en.localeCompare(b.en);// 第六关键字：队伍名utf8字典序小的优先。这个无耻考虑，名字也很重要啊，搞不好真关奖牌的事的哦：要是前四个都一样，我说不定也不给你们判同一个排名，希望，这是个小概率事件。
}

/**
 * 整个版的类
 * @param teamCount
 */
function Board() {
	this.longConnection=false;
	this.lastUpdateTime=0;
	this.autoScroll=false;
	this.animateOn=false;
	this.moveFinshed=true;
	this.scrollFollow=false;
	this.showTeamName=true;
	this.autoToggleDisplayName=false;
	this.intervalHandle=0;
	this.teamCount = 0;
	this.problemCount = 0;
	this.teams = {};
	this.headTeam = new Team(null,"header");// 链头
	this.headTeam.submited=true;this.headTeam.girlsTeam=true;
	this.headTeam.accepted=9999;//作为链头，当然是AK又AK啦
	this.tailTeam = new Team(null,"tail");// 链尾
	this.tailTeam.submited=true;
	this.tailTeam.accepted=-9999;//作为链尾，当然是一个没做并且还得负分啦
	this.stop=false;
	this.problemList = Problem.prototype.staticList;
	this.needUpdatedTeams=new Array();
	var arrTid=new Array();
	for ( var tid in pc2_teams.teams) {// 引用全局了
		var cs_team = cs_boardinfo.teams[pc2_teams.teams[tid]];// ContestService里面传过来的查询键是displayname
		if (cs_team === undefined) {
			log("赛事系统数据中没有对应的队伍：" + pc2_teams.teams[tid]);
			continue;
		}
		this.teamCount++;
		arrTid.push(tid);
		this.teams[tid] = new Team(cs_team, tid,
			cs_boardinfo.schools[cs_team.sid]);// 引用全局了
	}
	log("队伍数量："+this.teamCount);
	// 生成表头HTML
	var $board = this.$board = $("#board");
	var problemHTML = "", problemStatistic = "";
	for ( var i = 0; i < this.problemList.length; i++) {
		var problem = this.problemList[i];
		this.problemCount++;
		problemHTML += '<div class="cell cell-small">' + problem.name.xss()
				+ '</div>';
		problemStatistic += '<div class="cell cell-small" id="problemStatistic'
				+ problem.pid + '" title="Accepted / Summited">0/0</div>';
	}
	$board.before(
	/*		*/'<div class="line" id="board-header">' +
	/*			*/'<div class="cell cell-small">' +
	/*				*/'<div class="sub-cell">TRank</div>' +
	/*				*/'<div class="sub-cell">SRank</div>' +
	/*			*/'</div>' +
	/*			*/'<div class="cell cell-big">' +
	/*				*/'<div class="sub-cell">Team</div>' +
	/*				*/'<div class="sub-cell">School</div>' +
	/*			*/'</div>' +
	/*			*/'<div class="cell cell-small">AC</div>' +
	/*			*/'<div class="cell cell-small">Time</div>' +
	/*			*/problemHTML +
	/*		*/'</div>');
	//表尾
	$board.after(
	/*		*/'<div class="line" id="board-footer">' +
	/*			*/'<div class="cell cell-small">' +
	/*				*/'<div class="sub-cell">TRank</div>' +
	/*				*/'<div class="sub-cell">SRank</div>' +
	/*			*/'</div>' +
	/*			*/'<div class="cell cell-big">' +
	/*				*/'<div class="sub-cell">Team</div>' +
	/*				*/'<div class="sub-cell">School</div>' +
	/*			*/'</div>' +
	/*			*/'<div class="cell cell-small">AC</div>' +
	/*			*/'<div class="cell cell-small">Time</div>' +
	/*			*/problemStatistic +
	/*		*/'</div>');
	//////////////////// INIT ////////////////
	var I=this;
	var $board = this.$board;
	//根据这些数据排一次序
	arrTid.sort(function(a,b){
		return TeamCompare(I.teams[a],I.teams[b]);
	});
	//生成双向链表
	//1 根据排序tid对生成链表{
	var lastTeam = this.headTeam;
	for(var i=0;i<arrTid.length;i++){
		var tid=arrTid[i];
		lastTeam.nextTeam = this.teams[tid];
		this.teams[tid].preTeam = lastTeam;
		lastTeam = this.teams[tid];
	}
	lastTeam.nextTeam = this.tailTeam;
	//}*/
	/*/2 根据原始记录生成链表{
	var lastTeam = this.headTeam;
	for(var tid in this.teams){
		lastTeam.nextTeam = this.teams[tid];
		this.teams[tid].preTeam = lastTeam;
		lastTeam = this.teams[tid];
	}
	lastTeam.nextTeam = this.tailTeam;
	//}*/
	//生成主要的榜
	$board.html("");
	for ( var tp = this.headTeam; tp != null; tp = tp.nextTeam) {
		$board.append(tp.teamHTML);
	}
	/*/根据链表进行排序{
	for ( var tp = this.headTeam; tp != this.tailTeam; tp = tp.nextTeam) {
		tp.nextTeam.updatePos();//调整的是下一个节点，就能保证访问到所有的节点
	}
	//}*/
	var I=this,$body=$("body"),scrollTop=0;
	var bodyHeight=$body.height(),scrollDelta=30,minScroll=$(window).height()/3;
	//AUTO TOGGLE DISPLAYNAME MODULE
	setInterval(
	function(){
		if(!I.autoToggleDisplayName)return;
		I.toggleDisplayName();
		setTimeout(function(){
			I.toggleDisplayName();
		},6000);
	},15000);
	//AUTO SCROLL MODULE
	setInterval(
		function(){
			if(!I.autoScroll||!I.moveFinshed)return;
			$body.scrollTop(scrollTop);
			scrollTop+=scrollDelta;
			if(scrollTop-minScroll>bodyHeight||scrollTop<-minScroll){
				scrollDelta=-scrollDelta;
			}
		},500
	);
};
/**
 * 从服务器获取新的status 
 */
Board.prototype.update = function() {
	//ajax获得数据
	// 分发status到各个模块
	// 统筹first blood
	if(this.stop)return;
	var I=this;
	$.get(
		"GetStatus?"+Math.random(),{
			longConnection:this.longConnection
		},function(result){
		//	log(result);
			if(result.code==5||result.ajaxPageRefreshTimestamp>LOAD_TIME){
				location.reload();//页面要刷新了
			}else if(result.code==4){
				//延迟3000ms发起下一次请求
				ShowMessage(result.message+"<br/>I will try again in 3s.",3000);
				I.intervalHandle=setTimeout(function(){I.update();},3000);
			}else if(result.code==0){
				var tidSet={},newStatusList=[],maxUpdateTime=-1;
				for(var i=0;i<result.status.length;i++){
					//TODO TODO !!!清除那些旧Status，以及非法的tid。
					var status=new Status(result.status[i],result.statusHeader);
					if(I.teams[status.tid]==undefined){
						log(status.tid);
						continue;
					}
					if(status.lastUpdateTime<=I.lastUpdateTime)continue;//对于那些小于这次更新时间戳之前的，直接扔掉
					if(status.lastUpdateTime>maxUpdateTime)maxUpdateTime=status.lastUpdateTime;//更新这一次最新的时间戳。
					newStatusList.push(status);
				}
				if(maxUpdateTime==-1){
					log("真开心，什么也没更新……可以偷懒咯～ ^_^# 做题的同学要加油啊！");
				}else{
					I.lastUpdateTime=maxUpdateTime;
					for(var i=0;i<newStatusList.length;i++){
						var status=newStatusList[i];
						var pListPos=Problem.prototype.id2pos[status.pid];
					//	if(I.teams[status.tid].offical)//酱油队！放开那个first blood妹纸!
							I.problemList[pListPos].updateStatus(status);//必须先更新总体的，否则无法判断FirstBlood
						tidSet[status.tid]=true;//利用json性质做个Set用了。
					}
					for(var i=0;i<newStatusList.length;i++){
						try{
							I.teams[newStatusList[i].tid].updateStatus(newStatusList[i]);//再逐个更新每个队伍里面的信息
						}catch(e){
							log("Error:"+e.message+", # "+newStatusList[i].tid);
						}
					}
					//更新榜的部分
					/*/根据链表进行排序{
					//方案一：这个版本会造成多次不必要的移动
					for ( var tp = I.headTeam; tp.nextTeam != I.tailTeam;) {
						var moveInfo=tp.nextTeam.updatePos();//调整的是下一个节点，就能保证访问到所有的节点
						if(moveInfo!=null){
							I.needUpdatedTeams.push(moveInfo);
						}
						if(moveInfo==null)
							tp = tp.nextTeam;
					}
					//}*/ /*
					/*/ //TODO
					/*/方案二：这个方案会造成一些排序错误如果前面的下降时遇到后面一个也准备要下降就会停止。
					//当然，比赛过程中很少有下降的情况，但是也可能rejudege。所以这个方案不好。
					for ( var tp = I.headTeam; tp.nextTeam != I.tailTeam;) {//根据改变的部分进行排序
						if(tidSet[tp.nextTeam.tid]){
							//更新显示的排名，写了那么多还不是为了这一行么——要会动的，实时的
							//不允许并发的，并发的太难写了……除非能够保证所有的队伍不挪到同一个位置上面
							var moveInfo=tp.nextTeam.updatePos();
							if(moveInfo!=null){
								I.needUpdatedTeams.push(moveInfo);
	//						//	log("move:"+tp.nextTeam.en);
							}else{
	//						//	log("*move:"+tp.nextTeam.en);
								tp = tp.nextTeam;
							}
						}else{
	//					//	log("visit:"+I.teams[tp.nextTeam.tid].en);
							tp = tp.nextTeam;
						}
					}//*/
					//方案三：把所有要排序的都先拆出来，然后再排序：{
					for(var tid in tidSet){//拆出来
						var nowTeam=I.teams[tid];
						if(nowTeam==undefined){
							log(tid);
							continue;
						}
						var preTeam=nowTeam.preTeam,nextTeam=nowTeam.nextTeam;
						while(tidSet[nextTeam.tid])//找出第一个不要调整的team
							nextTeam=nextTeam.nextTeam;
						while(tidSet[preTeam.tid])
							preTeam=preTeam.preTeam;
						nowTeam.nextTeam=
						preTeam.nextTeam=nextTeam;
						nowTeam.preTeam=
						nextTeam.preTeam=preTeam;
					}
					for(var tid in tidSet){
						var nowTeam=I.teams[tid],pointer,updated=true,moveBack=false;
						if(nowTeam==undefined){
							continue;
						}
						pointer=nowTeam.preTeam;
						while(TeamCompare(nowTeam,pointer)<0){//尝试往前移动
							pointer=pointer.preTeam;
						}
						if(pointer==nowTeam.preTeam){//移动失败
							pointer=nowTeam.preTeam.nextTeam;//!!考虑连续被抽出来的情况nowTeam.preTeam.nextTeam!=nowTeam的
							while(TeamCompare(nowTeam,pointer)>0){//尝试往后移动
								pointer=pointer.nextTeam;
							}
							if(pointer==nowTeam.nextTeam){//没有变化，但是还得插回去的哦
								//updated=false;//发现这个地方去掉了优化后，就没事，具体原因不明，去掉优化复杂度没有明显提升，所以去掉，减轻编码量
							}else{//pointer刚刚好比nowTeam大//这两个情况是一样的。
							}
							moveBack=true;
						}else{//移动成功，现在pointer是刚刚好比nowTeam小
							//统一为前插入，所以
							pointer=pointer.nextTeam;//pointer刚刚好比nowTeam大
						}
						//插入回去：
						pointer.preTeam.nextTeam=nowTeam;
						nowTeam.preTeam=pointer.preTeam;
						pointer.preTeam=nowTeam;
						nowTeam.nextTeam=pointer;
						if(updated){
							I.needUpdatedTeams.push({fromTid:nowTeam.tid,toTid:pointer.tid,moveBack:moveBack});
						}
					}
					//}/*/
					var existSchool={},schoolRank=0,teamRank=0;
					for(var pointer=I.headTeam.nextTeam;pointer!=I.tailTeam;pointer=pointer.nextTeam){
	//					var $rank=$("#rank"+pointer.tid+">*"),$name=$("#name"+pointer.tid+">*");
						var $team=$("#team"+pointer.tid);
	//					$rank.removeClass("gold sliver bronze");
	//					$name.removeClass("gold sliver bronze");
						$team.removeClass("gold sliver bronze");
						if(pointer.offical){
							var $schoolRank=$("#schoolRank"+pointer.tid),$teamRank=$("#teamRank"+pointer.tid);
							$teamRank.text("");
							$schoolRank.text("");
							if(pointer.accepted==0)continue;
							if(!existSchool[pointer.school.cn]){
								pointer.schoolRank=++schoolRank;
								$schoolRank.text(pointer.schoolRank);
							}
							existSchool[pointer.school.cn]=true;
							pointer.lastRank=pointer.teamRank;
							pointer.teamRank=++teamRank;
							$teamRank.text(pointer.teamRank);
						}else if(pointer.accepted>0){
							pointer.teamRank=teamRank+1;
						}else{
							continue;
						}
						if(pointer.teamRank<=GOLD){
							$team.addClass("gold");
						}else if(pointer.teamRank<=SLIVER){
							$team.addClass("sliver");
						}else if(pointer.teamRank<=BRONZE){
							$team.addClass("bronze");
						}
					}
				}
				$("#message").hide();
				I.queAnimate();
				if(I.longConnection)
					I.intervalHandle=setTimeout(function(){I.update();},100);//TODO 停止自动刷新
				else
					I.intervalHandle=setTimeout(function(){I.update();},60000);//TODO 停止自动刷新
			}else{
				//其他故障20s后刷新页面
				ShowMessage(result.message+"<br/>This Page will be reload after 20s.<br/><br/>页面将在 <i class='secondDelay'></i> 后刷新。</p>",55000);
				clearInterval(I.intervalHandle);
				setRefresh(20);
			}
		},"json"
	);
};
/**
 * 是否打开长连接
 */
Board.prototype.setLongConnection=function(enable){
	clearInterval(this.intervalHandle);
	this.longConnection=enable;
	if(enable){
		this.update();
	}
};
Board.prototype.queAnimate=function(){
	var I=this;
	if(I.moveFinshed){
		if(I.needUpdatedTeams.length==0)return;
		I.moveFinshed=false;
		if(I.needUpdatedTeams.length>MAX_ANIMATE_QUE||!I.animateOn){
			log("一次性更新了"+I.needUpdatedTeams.length+"个DOM对象。");
			if(I.animateOn)
				ShowMessage("<br/><h3>(＞﹏＜) </h3><p>瓦咔咔，这一次要播放的动画太多了，所以偷懒木有动画啦…… </p><br/>",5000);
			while(I.needUpdatedTeams.length>0){
				I.moveTeam(I.needUpdatedTeams.shift(),true);//TODO
			}
			I.moveFinshed=true;
		}else{
			I.moveTeam(I.needUpdatedTeams.shift(),false);//TODO
		}
	}
	setTimeout(function(){I.queAnimate();},100);
};
Board.prototype.toggleDisplayName=function(){
	if(!this.moveFinshed)return;
	if(this.showTeamName)
		$(".sub-cell").animate({top:-30},2000);
	else
		$(".sub-cell").animate({top:0},2000);
	this.showTeamName=!this.showTeamName;
};
/**
 * 显示一个系统消息
 * @param message 消息内容
 * @param timeout 消息显示时间
 */
function ShowMessage(message,timeout){
	var $message=$("#message");
	$message.html(message);
	$message.fadeIn();
	setTimeout(function(){$message.fadeOut();},timeout==null?10000:timeout);
}
String.prototype.xss=function(){
	return this.replace(/<script>.*<\/script>/g,'')
	.replace(/&/g, '&amp;')
	.replace(/"/g, '&quot;')
	.replace(/'/g, '&#39;')
	.replace(/</g, '&lt;')
	.replace(/>/g, '&gt;');
};
$(function(){
	$("body").append('<div id="ajaxError" style="display:none"></div>');
	$("#ajaxError").ajaxError(function(event,request, settings){
	    $(this).append("<li>出错页面:" + settings.url + "</li>");
	    ShowMessage("<br/><h3>(＞﹏＜) </h3><p>载入页面出错了：<a href='"+settings.url+"'>" + settings.url+"</a>，服务器可能出现错误了，请稍候再试……<br/>页面将在 <i class='secondDelay'></i> 后刷新。</p>",60000);
	    setRefresh(20);
	});
});
var onRefresh=false,secondDelay=0;
function setRefresh(t){
	if(onRefresh)return;
	onRefresh=true;
	secondDelay=t;
	doRefresh();
}
function doRefresh(){
	if(secondDelay==0){
		location.reload();
	}else{
		secondDelay--;
		$(".secondDelay").text(secondDelay+" 秒");
		setTimeout(doRefresh,1000);
	}
}
function getPersonHtml(person){
	return (person.m?"♂":"♀")+" "+person.en.xss()+" <small>"+person.cn.xss()+"</small>";
}
function log(msg){
	try{
		console.log(msg);
	}catch(e){}
}
/*winguse love shuxiao*/