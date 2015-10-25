NENULiveBoard
=============

This is a live score board based on PC^2 API, and runs quite well during the onsite contest of 2012/2015 ACM-ICPC Asia Regional Changchun Site. It offered some fancy features like showing the animation of the real time changes of teams' ranking.

The code is a little old, ugly, imperfect as I am looking back now, three years later. There should have better design of the system as well as user experiences. But it still works. I do not have planty of time to improve it nowadays, but if you plan to use it, and faced problems, I would like to provide support as much as I can (You can reach me by QQ: 416529576, or email).

License
=============
MIT

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
<pre><code>
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
</code></pre>
