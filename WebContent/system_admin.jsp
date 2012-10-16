<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Board System Admin</title>
<style>
#main>div{
	float:left;
	width:50%;
}
</style>
</head>
<body>
<div id="main">
	<div>
	<h3>推送状态</h3>
	<dl id="pushAjax">
		<dt>上次刷新时间戳：</dt><dd id="ajaxPageRefreshTimestamp"></dd>
		<dt>活跃连接数：</dt><dd id="liveCount"></dd>
		<dt>缓存命中数：</dt><dd id="cacheRequestCount"></dd>
		<dt>数据库访问数：</dt><dd id="dbRequestCount"></dd>
	</dl>
	<h3>缓存状态</h3>
	<dl id="cache">
		<dt>最旧缓存时间：</dt><dd id="oldestTimestamp"></dd>
		<dt>最新缓存时间：</dt><dd id="lastTimestamp"></dd>
		<dt>缓存线程活跃：</dt><dd id="isAlive"></dd>
		<dt>缓存线程消息：</dt><dd id="exitMessage"></dd>
		<dt>缓存线程状态字：</dt><dd id="exitCode"></dd>
		<dt>缓存周期计数器：</dt><dd id="interval"></dd>
	</dl>
	</div>
	<div>
	<h3>设置</h3>
	<dl id="setting">
		<dt><label for="freezeTime">封榜时间(min)：</label></dt>
			<dd><input id="freezeTime" type="number" /></dd>
		<dt><label for="pushAjaxInterval">长连接最大周期(ms)：</label></dt>
			<dd><input id="pushAjaxInterval" type="number" /></dd>
		<dt><label for="statusCacheInterval">单个缓存周期(ms)：</label></dt>
			<dd><input id="statusCacheInterval" type="number" /></dd>
		<dt><label for="statusCacheIntervalCount">缓存周期个数：</label></dt>
			<dd><input id="statusCacheIntervalCount" type="number" /></dd>
		<dt><label for="maxActivePushAjaxConnection">最大活跃长连接个数
		(准确说，包含其他链接，全局连接数在大于这个值130%拒绝新的长连接，100~130%时，随机允许)：</label></dt>
			<dd><input id="maxActivePushAjaxConnection" type="number" /></dd>
		<dt>停止缓存线程：（一旦检测到有获取status的请求时，线程会自动开始的，仅仅用于重置。）</dt>
			<dd><input onclick="setStop()" type="button" value="停止" id="setStop" /></dd>
		<dt>通知全体页面刷新：（通知所有在线的页面刷新，用于更新js代码。）</dt>
			<dd><input onclick="setRefresh()" type="button" value="通知" id="setRefresh" /></dd>
		<dt>清除已经查看的Pedding：</dt>
			<dd><input onclick="setClearPedding()" type="button" value="通知" id="setRefresh" /></dd>
	</dl>
	<%if(session.getAttribute("login")==null){%>
	<h3>登录</h3>
	<dl id="login">
		<dt><label for="password">密码：</label></dt>
			<dd><input id="password" type="password" /></dd>
	</dl>
	<p><input type="button" value="授权" onclick="auth()"/></p>
	<%} %>
	</div>
</div>
<script type="text/javascript" src="js/jquery.min.js"></script>
<script type="text/javascript">
var refreshInterval=0;
function setStop(){
	$.post(
		"SystemAdmin",
		{setStop:true},
		function(data){
			if(data.code==0){
				alert("set stop ok.");
			}else{
				alert("set stop error: "+data.message);
			}
		}
	);
}
function setRefresh(){
	$.post(
		"SystemAdmin",
		{setStop:true,setRefresh:true},//页面刷新后，由于请求全部原始数据的量会变大，所以缓存线程也需要重启一下，以保证性能
		function(data){
			if(data.code==0){
				alert("set refresh ok.");
			}else{
				alert("set refresh error: "+data.message);
			}
		}
	);
}
function work(){
	var localData={};
	if(refreshInterval>0){
		clearInterval(refreshInterval);
		localData={
			freezeTime:$("#freezeTime").attr("value"),
			pushAjaxInterval:$("#pushAjaxInterval").attr("value"),
			statusCacheIntervalCount:$("#statusCacheIntervalCount").attr("value"),
			statusCacheInterval:$("#statusCacheInterval").attr("value"),
			maxActivePushAjaxConnection:$("#maxActivePushAjaxConnection").attr("value")
		};
	}
	$.ajax({
		url:"SystemAdmin",
		type:"POST",
		data:localData,
		success:function(result){
			if(result.code!=0){
				alert(result.message);
				return;
			}
			$("#ajaxPageRefreshTimestamp").text(new Date(result.pushAjax.ajaxPageRefreshTimestamp));
			$("#liveCount").text(result.pushAjax.liveCount);
			$("#cacheRequestCount").text(result.pushAjax.cacheRequestCount);
			$("#dbRequestCount").text(result.pushAjax.dbRequestCount);

			$("#lastTimestamp").text(new Date(result.cache.lastTimestamp));
			$("#exitCode").text(result.cache.exitCode);
			$("#exitMessage").text(result.cache.exitMessage);
			$("#oldestTimestamp").text(new Date(result.cache.oldestTimestamp));
			$("#isAlive").text(result.cache.isAlive);
			$("#interval").text(result.cache.interval);

			$("#pushAjaxInterval").attr("value",result.setting.pushAjaxInterval);
			$("#freezeTime").attr("value",result.setting.freezeTime);
			$("#statusCacheIntervalCount").attr("value",result.setting.statusCacheIntervalCount);
			$("#statusCacheInterval").attr("value",result.setting.statusCacheInterval);
			$("#maxActivePushAjaxConnection").attr("value",result.setting.maxActivePushAjaxConnection);
		},
		async:false
	});
	refreshInterval=setTimeout(work,5000);
}
function auth(){
	$.post(
		"Login",{
			password:$("#password").attr("value")
		}
	);
	$("#password").attr("value","");
	location.reload();
}
function setClearPedding(){
	$.post("ClearPedding",{
		reload:true
	},function(d){
		if(d.code==0){
			alert("reset ok");
		}else{
			alert(d.message);
		}
	},"json");
	$("#password").attr("value","");
}
/*function dlHtml(name,values){
	var html="";
	for(var key in values){
		html+="<dt>"+key+":</dt><dd>"+values[key]+"</dd>";
	}
	$("#"+name).html(html);
}*/
$("#setting input").change(work);
work();
<%if(session.getAttribute("login")==null){%>
$("#setting input").attr("disabled","disabled");
<%}%>
</script>
</body>
</html>