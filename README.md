NENULiveBoard
=============

A Live ACM/ICPC Onsite Live Board Solution. Perfectly run during 2012 ACM-ICPC Asia Regional Contest (Changchun Site).

This project is base on object oriented Javascript and J2EE programing.

By using long connection AJAX to fetch newly changed status (JSON) from backend to provide all users the live time score board of the ACM-ICPC on site contest.

By using some features of CSS3 and HTML5, users will get a very well exprences of display the score board.

Animation is also available for showing the changes of the ranking.

Preview: http://acm.nenu.edu.cn:8080/BoardWeb/

License
=============
GPLv3
Additional requirement: you may not remove the links to http://acm.nenu.edu.cn and http://winguse.com.
You are welcome not to remove some of the last line of the code.

For More
=============
Read system_guide.docx (or pdf)

video description (Chinese): http://v.youku.com/v_show/id_XNDY3MTQwNzUy.html

Exporting Data to ICPC
=============

Firstly, download "Export empty template" in the standing tab of you site.
Secondly, replace that xml file using regex expression, from
  .+?ReservationID="(.+?)".+?TeamName="(.+?)".+?>
to
  "\2":"\1",
Then, edit the data to json format, and you will get something like this:

var tn2id={"+18远古遗愿":"161940",
"ALPC_ACOnFingers":"161713",
"ALPC_Tour_de_Force":"161714",...};

Thridly, paste the the code above and the code following into Chrome javascript console,
you will get the final exporting xml.

var result="";
for(var t=board.headTeam.nextTeam;t!=board.tailTeam;t=t.nextTeam){
  if(t.en=="BHU_sec")t.en="BHU_Sec";
	if(tn2id[t.en.xss()]==undefined){
		if(t.offical)
			console.log("error:"+t.en.xss());
		continue;
	}
	result+='<Standing LastProblemTime="'+t.lastAccptedTime+'" ProblemsSolved="'+t.accepted+'" Rank="'+t.teamRank+'" ReservationID="'+tn2id[t.en.xss()]+'" TeamName="'+t.en.xss()+'" TotalTime="'+t.penalty+'"/>';
}
