/*
see more:
	https://medium.freecodecamp.org/node-js-child-processes-everything-you-need-to-know-e69498fe970a
	https://gist.github.com/fkowal/3447400
*/

var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
const { spawn } = require('child_process');

var cpu = require('./cpu.js');


var interval = 1000;
var port = 3500;
 
 
app.get('/', function(req, res){
    res.sendFile(__dirname + '/index.html');
});

setInterval(function(){
    cpu.getPercentageUsage(interval, function(percentage){
        io.emit('cpu_usage', percentage);
    });
}, interval);


const cmdLoadavg = spawn(
	//'hostname',
	'top -b ', 
	//'stdbuf -i0 -o0 -e0 top -b ',
{
  shell: true
});
/*
cmdLoadavg.stdout.on('exit', 		function(data){ console.log("exit") });
cmdLoadavg.stdout.on('disconnect', 	function(data){ console.log("disconnect") });
cmdLoadavg.stdout.on('error', 		function(data){ console.log("error") });
cmdLoadavg.stdout.on('close', 		function(data){ console.log("close") });
cmdLoadavg.stdout.on('message', 	function(data){ console.log("message") });
*/

///const cmdGrep = spawn('grep load');
///cmdLoadavg.stdout.pipe(cmdGrep.stdin)

cmdLoadavg.stdout.on('data', function(dataBuffer){
	var data = dataBuffer + "";

	var cmdOutLines = data.split("\n");
	var strLoadAvg = cmdOutLines;
	/*
	///@investigate: Dont work as expected naively..
	var strLoadAvg = cmdOutLines.filter(function (el){
		return (el.indexOf("load average" > -1));
	});
	console.log(strLoadAvg);
	*/

	
	for(i = 0; i < strLoadAvg.length; i++){
		// simulate a 'grep' but without de stdout-cmd-buffer problems
		if(strLoadAvg[i].indexOf("load average") > -1){
			io.emit('load_average', strLoadAvg[i]);
		}
	}
});

http.listen(port, function(){
  console.log('Listening on *:' + port);
});

