$(document).ready(function() {
	bindBotton();
});

function bindBotton() {
	$("#openSocket").bind("click", {}, function(event) {
		openSocket();
	});
	$("#closeSocket").bind("click", {}, function(event) {
		closeSocket();
	});
	$("#sendMessage").bind("click", {}, function(event) {
		if(sendMessage()){
			$("#contentText").val("");
			$("#contentText").focus();
		}
	});
	
	$("#contentText").keydown(function(e){
		if(e.keyCode==13){
			if(sendMessage()){
				$("#contentText").val("");
				$("#contentText").focus();
			}
		}
	});
}

var socket;
function openSocket() {
	if (socket != null) {
		console.log("请先退出");
		$("#responseText").append("请先退出\n");
		return;
	}
	$("onlineServiceCount").empty("");
	$("onlineCustomerCount").empty("");
	var initOnlineUser = $(".online_user");
	if (initOnlineUser.length > 0) {
		initOnlineUser.each(function(i) {
			this.remove();
		});
	}

	if (typeof (WebSocket) == "undefined") {
		console.log("您的浏览器不支持WebSocket");
	} else {
		console.log("您的浏览器支持WebSocket");
		// 实现化WebSocket对象，指定要连接的服务器地址与端口 建立连接
		// 等同于socket = new WebSocket("ws://localhost:8888/xxxx/im/25");
		// var socketUrl="${request.contextPath}/im/"+$("#userId").val();

		var urlPath = window.location.pathname;
		urlPath = urlPath.substring(0, urlPath.lastIndexOf("/"));
		var socketUrl = "ws://" + window.location.host + urlPath + "/chat/"
				+ $("#userType").val() + "/" + $("#userId").val();
		console.log(socketUrl);
		if (socket != null) {
			socket.close();
			socket = null;
		}
		socket = new WebSocket(socketUrl);
		// 打开事件
		socket.onopen = function() {
			console.log("websocket已打开");
			$("#responseText").append("websocket已打开\n");
			// socket.send("这是来自客户端的消息" + location.href + new Date());
		};
		// 获得消息事件
		socket.onmessage = function(msg) {
			var dto = JSON.parse(msg.data);
			console.log(msg.data);
			if (dto.messageType == "contentText") {
				showContentText(dto);
			} else if (dto.messageType == "unreadMessage") {
				showUnreadMessage(dto);
			} else if (dto.messageType == "historyContentText") {
				showContentText(dto);
			} else if (dto.messageType == "onlineInfo") {
				showOnlineInfo(dto);
			}
		};
		// 关闭事件
		socket.onclose = function() {
			socket = null;
			console.log("websocket已关闭");
			$("#responseText").append("websocket已关闭\n");
		};
		// 发生了错误事件
		socket.onerror = function() {
			console.log("websocket发生了错误");
			("#responseText").append("websocket发生了错误\n");
		}
	}
}

function anchoringResponseTextBottom(){
	$("#responseText").animate({scrollTop:$("#responseText").children("div:last-child").offset().top},500);
}

function sendMessage() {
	if(socket==null||$("#contentText").val()==""
		||($("#userType").val()=="service"&&($("#toUserType").val()==""||$("#toUserId").val()==""))){
		return false;
	}
	if (typeof (WebSocket) == "undefined") {
		console.log("您的浏览器不支持WebSocket");
	} else {
		console.log("您的浏览器支持WebSocket");
		var messageJson = '{"action":"sendMessage","targetUser":{"type":"'
				+ $("#toUserType").val()
				+ '","id":"'
				+ $("#toUserId").val()
				+ '"},"data":{"contentText":"' + $("#contentText").val() + '"}}';
		console.log(messageJson);
		socket.send(messageJson);
		
		var htmlText = $("<div style='clear:both'></div>");
		htmlText.html("我:"+$("#contentText").val());
		htmlText.css("float","right");
		
		$("#responseText").append(htmlText);
		anchoringResponseTextBottom();
	}
	return true;
}

function pullMessage() {
	if (typeof (WebSocket) == "undefined") {
		console.log("您的浏览器不支持WebSocket");
	} else {
		console.log("您的浏览器支持WebSocket");
		var messageJson = '{"action":"pullMessage","targetUser":{"type":"'
				+ $("#toUserType").val()
				+ '","id":"'
				+ $("#toUserId").val()
				+ '"}}';
		console.log("pullMessage--->");
		console.log(messageJson);
		socket.send(messageJson);
	}
}

function pullMessageHis(data,firstDate) {
	if(data==null&&firstDate==null){
		return;
	}
	if(data==null){
		data={"toUserType":$("#toUserType").val(),"toUserId":$("#toUserId").val()};
	}else{
		$("#responseText").empty();
	}
	if (typeof (WebSocket) == "undefined") {
		console.log("您的浏览器不支持WebSocket");
	} else {
		console.log("您的浏览器支持WebSocket");
		var messageJson = {"action":"pullMessageHis",
				"targetDate":+firstDate==null?"":firstDate,
				"targetUser":{"type":data.toUserType,
					"id":data.toUserId}
				};
		console.log("pullMessageHis--->");
		console.log(messageJson);
		socket.send(JSON.stringify(messageJson));
	}
}

function closeSocket() {
	if (typeof (WebSocket) == "undefined") {
		console.log("您的浏览器不支持WebSocket");
	} else {
		socket.close();
	}
}

function showContentTextData(data,messageType){
	var htmlDate = $("<div style='clear:both;margin:0px 0px 0px 40%;'></div>");
	var htmlText = $("<div style='clear:both'></div>");
	htmlDate.html(data.produceDate.substring(0,16));
	htmlDate.attr("id",data.produceDate);
	if(messageType=="historyUnreadContentText"){
		htmlText.html("*"+data.contentText);
	}
	if(data.fromUser.type==$("#userType").val()&&data.fromUser.id==$("#userId").val()){
		htmlText.html("我:"+data.contentText);
		htmlText.css("float","right");
	}else{
		htmlText.html(data.fromUser.id+":"+data.contentText);
		htmlText.css("float","left");
	}
		
	if(messageType=="historyContentText"){
		$("#responseText").prepend(htmlText);
		$("#responseText").prepend(htmlDate);
	}else{
		$("#responseText").append(htmlDate);
		$("#responseText").append(htmlText);
	}
}

var scrollTopFlag = -1;

function showContentText(dto) {
	if(dto.messageType=="historyContentText"){
		if(dto.data!=null&&dto.data.length>0){
			$(dto.data).each(function(i) {
				showContentTextData(this,dto.messageType);
			});
			
			scrollTopFlag = 0;
			$("#responseText").bind("scroll",function(){
				var scrollTop = $("#responseText").scrollTop();
//				totalHeight = $("#responseText").height();
				if(scrollTop==scrollTopFlag){
					alert(scrollTop);
					$(this).unbind('scroll');
					scrollTopFlag=-1;
					pullMessageHis(null,$("#responseText").children("div:first-child").attr("id"));
				}
			});
		}
	}else{
		showContentTextData(dto.data,dto.messageType);
	}
	if(dto.targetDate==null){
		anchoringResponseTextBottom();
	}
}

function showUnreadMessageResUser(chatUser, onlineDivId) {
	if (chatUser.type == $("#toUserType").val()
			&& chatUser.id == $("#toUserId").val()) {
		pullMessage();
	} else {
		var onlineUser = $("[id='" + chatUser.type + ":" + chatUser.id + "']");
		if (onlineUser.length > 0) {
			onlineUser.each(function(i) {
				$(this).html(chatUser.id);
				var tfont = $("<font>[" + chatUser.toMeUnreadMessageCount
						+ "]</font>");
				$(this).append(tfont);
			});
		} else {
			var liClass = "offline";
			var li = $("<li id=" + chatUser.type + ":" + chatUser.id
					+ " class='" + liClass + "'></li>");
			li.html(chatUser.id);
			var tfont = $("<font>[" + chatUser.toMeUnreadMessageCount
					+ "]</font>");
			$(li).append(tfont);
			$("#" + onlineDivId).append(li);
			li.bind("click", {
				"toUserType" : chatUser.type,
				"toUserId" : chatUser.id
			}, function(event) {
				pullMessageHis(event.data);
				$("#toUserType").val(event.data.toUserType);
				$("#toUserId").val(event.data.toUserId);
				pullMessage();
				$(this).children().filter("font").remove();
			});
		}
	}
}

function showUnreadMessageRes(chatOnline) {
	$(chatOnline.unreadCustomerUsers).each(function(i) {
		showUnreadMessageResUser(this, "onlineCustomer");
	});
	$(chatOnline.unreadServiceUsers).each(function(i) {
		showUnreadMessageResUser(this, "onlineService");
	});
}

function showUnreadMessage(dto) {
	showUnreadMessageRes(dto.data);
}

function showOnlineInfoResUser(chatUser, onlineDivId, liClass) {
	if ($("[id='" + chatUser.type + ":" + chatUser.id + "']").length == 0) {
		var li = $("<li id=" + chatUser.type + ":" + chatUser.id + " class='"
				+ liClass + "'></li>");
		li.html(chatUser.id);
		$("#" + onlineDivId).append(li);
		li.bind("click", {
			"toUserType" : chatUser.type,
			"toUserId" : chatUser.id
		}, function(event) {
			pullMessageHis(event.data);
			$("#toUserType").val(event.data.toUserType);
			$("#toUserId").val(event.data.toUserId);
			pullMessage();
			$(this).children().filter("font").remove();
		});
	}
}

function showOnlineInfoRes(chatOnline, just) {
	
	$("#onlineServiceCount").html("[" + chatOnline.serviceCount + "]");
	$("#onlineCustomerCount").html("[" + chatOnline.customerCount + "]");
	var liClass = "online_user";
	if (just) {
		liClass += " just_online";
	}
	$(chatOnline.serviceUsers).each(function(i) {
		showOnlineInfoResUser(this, "onlineService", liClass);
	});
	$(chatOnline.customerUsers).each(function(i) {
		showOnlineInfoResUser(this, "onlineCustomer", liClass);
	});

	if (just) {
		$(chatOnline.offlineServiceUsers).each(function(i) {
			var onlineUser = $("[id='" + this.type + ":" + this.id + "']");
			if (onlineUser.length > 0) {
				onlineUser.each(function(i) {
					$(this).addClass("just_offline");
				});
			}
		});

		$(chatOnline.offlineCustomerUsers).each(function(i) {
			var onlineUser = $("[id='" + this.type + ":" + this.id + "']");
			if (onlineUser.length > 0) {
				onlineUser.each(function(i) {
					$(this).addClass("just_offline");
				});
			}
		});
	}
}

function showOnlineInfo(dto) {
	$(".just_online").each(function(i) {
		$(this).removeClass("just_online");
	});

	$(".just_offline").each(function(i) {
		$(this).remove();
	});

	showOnlineInfoRes(dto.data, dto.data.just);
}