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
			if (dto.response.messageType == "contentText") {
				showContentText(dto);
			} else if (dto.response.messageType == "unreadMessage") {
				showUnreadMessage(dto);
			} else if (dto.response.messageType == "onlineInfo") {
				showOnlineInfo(dto);
			}
		};
		// 关闭事件
		socket.onclose = function() {
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
function sendMessage() {
	if(socket==null||$("#contentText").val()==""
		||($("#userType").val()=="service"&&($("#toUserType").val()==""||$("#toUserId").val()==""))){
		return false;
	}
	if (typeof (WebSocket) == "undefined") {
		console.log("您的浏览器不支持WebSocket");
	} else {
		console.log("您的浏览器支持WebSocket");
		var messageJson = '{"request":{"action":"sendMessage","data":{"toUsers":[{"type":"'
				+ $("#toUserType").val()
				+ '","id":"'
				+ $("#toUserId").val()
				+ '"}],"contentText":"' + $("#contentText").val() + '"}}}';
		console.log(messageJson);
		socket.send(messageJson);
		var seeText = "我--->";
		if ($("#toUserType").val() == "customer") {
			seeText =  seeText+"[客户]";
		} else if ($("#toUserType").val() == "service") {
			seeText =  seeText+"[客服]";
		}
		seeText=seeText+$("#toUserId").val() + ":";
		
		$("#responseText").append(seeText+$("#contentText").val()+"\n");
		var textarea = document.getElementById("responseText");
		textarea.scrollTop = textarea.scrollHeight;
	}
	return true;
}

function pullMessage() {
	if (typeof (WebSocket) == "undefined") {
		console.log("您的浏览器不支持WebSocket");
	} else {
		console.log("您的浏览器支持WebSocket");
		var messageJson = '{"request":{"action":"pullMessage","fromUser":{"type":"'
				+ $("#toUserType").val()
				+ '","id":"'
				+ $("#toUserId").val()
				+ '"}}}';
		console.log("pullMessage--->");
		console.log(messageJson);
		socket.send(messageJson);
	}
}

function closeSocket() {
	if (typeof (WebSocket) == "undefined") {
		console.log("您的浏览器不支持WebSocket");
	} else {
		socket.close();
	}
}

function showContentText(dto) {
	if (!(dto.response.data == null || dto.response.data.length == 0)) {
		$(dto.response.data).each(
				function(i) {
					var seeText = dto.request.fromUser.id + "--->我:";
					if (dto.request.fromUser.type == "customer") {
						seeText = "[客户]" + seeText;
					} else if (dto.request.fromUser.type == "service") {
						seeText = "[客服]" + seeText;
					}
					var responseText = this.produceDate + "\n" + seeText
							+ this.contentText;
					$("#responseText").append(responseText+"\n");
				});
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
	if (!(dto.response.data == null || dto.response.data.length == 0)) {
		$(dto.response.data).each(function(i) {
			showUnreadMessageRes(this);
		});
	}
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

	if (!(dto.response.data == null || dto.response.data.length == 0)) {
		$(dto.response.data).each(function(i) {
			showOnlineInfoRes(this, dto.response.data.just);
		});
	}
}