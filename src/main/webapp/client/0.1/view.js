
function GameEvent(view) {
	this.view_ = view;
	this.event_ = "";
	this.params_ = new Object();
	this.content_ = null;
	
}
GameEvent.prototype.getEventName = function() { return this.event_; };
GameEvent.prototype.getParameter = function(parameterName) {
	return this.params_[parameterName];
}
GameEvent.prototype.getContent = function() { return this.content_; };
GameEvent.prototype.selectContent = function(xpath) {
	if(this.content_ == null) return null;
	
	var doc = this.content_.ownerDocument;
	var nsResolver = this.view_.createNSResolver();
	
	var result = doc.evaluate(xpath, this.content_, nsResolver, XPathResult.ANY_TYPE, null);
		
	var found = [];
	var res;
	while (res = result.iterateNext()) {
		found.push(res);
	}
	
	if(found.length == 1) {
		return found[0];
	}
	else {
		return found;
	}
}

GameEvent.prototype.loadXML = function(xml) {
	console.log("GameEvent: loading xml: " + xml);
	forChildNodes(xml, {
		"event": this.loadEventNode
	}, this);
};
GameEvent.prototype.loadEventNode = function(xml) {
	console.log("GameEvent: loadingEventNode: " + xml);
	this.event_ = xml.getAttribute("name");
	forChildNodes(xml, {
		"param": this.loadParam,
		"content": this.loadContent
	}, this);
};
GameEvent.prototype.loadParam = function(xml) {
	console.log("GameEvent: loading param: " + xml);
	this.params_[xml.getAttribute("name")] = nodeText(xml);
};
GameEvent.prototype.loadContent = function(xml) {
	console.log("GameEvent: loading content: " + xml);
	this.content_ = xml;
};

function View() {
	this.eventEndpoint_ = null;
	this.joinEndpoint_ = null;
	this.startEndpoint_ = null;
	this.gameNamespace_ = "";
	this.viewNamespace_ = "";
	this.channel_ = null;
	this.socket_ = null;
	this.userToken_ = "";
	this.socketConnected_ = false;
	this.events_ = new Object();
	this.eventHandlers_ = new Object();
	
	this.gameEventHandlers_ = {
	};
	
	if(document.selectNodes) {
		document.setProperty("SelectionLanguage", "XPath");
	}
	
	var self = this;
	window.onload = function() { self.init(); }
}
View.prototype.createNSResolver = function() {
	var self = this;
	return function(prefix) {
		var dict = {
			"view": self.viewNamespace_,
			"game": self.gameNamespace_,
		}
		return dict[prefix];
	}
}
View.instance_ = null;
View.getInstance = function() {
	if(View.instance_ == null) {
		View.instance_ = new View();
	}
	return View.instance_;
}
View.prototype.addEvents = function(events) {
	for(var i = 0; i < events.length; i++) {
		var h = events[i];
		
		this.events_[h.id] = h;
	}
}
View.prototype.addEventHandlers = function(eventHandlers) {
	for(var i = 0; i < eventHandlers.length; i++) {
		var h = eventHandlers[i];
		
		if(!this.eventHandlers_[h.event]) {
			this.eventHandlers_[h.event] = new Array();
		}
		
		this.eventHandlers_[h.event].push(h);
	}
}
View.prototype.init = function() {
	this.createChannel();
	
	Event.fire(this, "load", []);
}
View.prototype.reload = function() {
	window.location.reload();
}
View.prototype.triggerEventHandlers = function(handlers, event) {
	console.log("triggering event handlers for event: " + event.getEventName());
	var filtered = new Array();
	
	for(var i = 0; i < handlers.length; i++) {
		var h = handlers[i];
		
		var val = true;
		
		if(h.condition && h.condition != "") {
		
			with(event) {
				try { 
					val = !!eval(h.condition); 
				}
				catch(e) { 
					console.log("error evaluationg '" + h.condition + "': " + e.toString());
					val = false;
				}
			}
		}
		
		if(val) {
			filtered.push(h);
		}
	}
	
	
	
	for(var i = 0; i < filtered.length; i++) {
		var h = filtered[i];
		
		var el = document.getElementById(h.id);
		
		if(!el) continue;
				
		var temp = document.getElementById("template-" + h.id);
		
		if(temp) {
			if(!h.handlerMode || h.handlerMode == "" || h.handlerMode == "replace")
				emptyNode(el);
			
			var n = replacePatternNode(temp, event);
			
			console.log("appending children: " + n.childNodes.length);
			
			for(var j = 0; j < n.childNodes.length; j++) {
				el.appendChild(n.childNodes[i]);
			}
		}
	}
}
View.prototype.handleSocketMessage = function(msg) {
	console.log("entire socket message: " + msg.data);	
	//this.reload();
	
	var event = new GameEvent(this);
	event.loadXML((new window.DOMParser()).parseFromString(msg.data, "text/xml"));
	
	console.log("socket message: " + event.event_);
	
	var handler = this.eventHandlers_[event.event_];
	
	if(typeof handler == "function") {
		handler.apply(this, [event]);
	}
	else if(handler) {
		this.triggerEventHandlers(handler, event);
	}
	
	Event.fire(this, event.event_, [event]);
}
View.prototype.handleSocketError = function() {
	
}
View.prototype.handleSocketClose = function() {
	self.connected_ = false;
}
View.prototype.createChannel = function() {
	console.log("creating channel: " + this.userToken_);
	
	if(this.userToken_ == "") return;
	
	this.channel_ = new goog.appengine.Channel(this.userToken_);
	this.socket_ = this.channel_.open();
	
	var self = this;
	this.socket_.onopen = function() {
		self.socketConnected_ = true;
	}
	this.socket_.onmessage = function() {
		self.handleSocketMessage.apply(self, arguments);
	}
	this.socket_.onerror = function() {
		self.handleSocketError.apply(self, arguments);
	}
	this.socket_.onclose = function() {
		self.handleSocketClose.apply(self, arguments);
		self.socketConnected_ = false;
	}
}
View.prototype.setGameNamespace = function(str) {
	this.gameNamespace_ = str;
}
View.prototype.setUserToken = function(token) {
	this.userToken_ = token;
}
View.prototype.setViewNamespace = function(str) {
	this.viewNamespace_ = str;
}
View.prototype.registerEventEndpoint = function(obj) {
	this.eventEndpoint_ = obj;
};
View.prototype.registerStartEndpoint = function(obj) {
	this.startEndpoint_ = obj;
};
View.prototype.registerJoinEndpoint = function(obj) {
	this.joinEndpoint_ = obj;
};
View.prototype.handleEventSuccess = function(el, event, responseXML) {
	console.log("success!");
}
View.prototype.handleEventError = function(el, event, responseXML) {
	var el = responseXML.getElementsByTagNameNS(this.gameNamespace_, "error");
	
	var message = "unknown";
	if(el.length > 0) {
		message = nodeText(el[0]);
	}
	alert("there was an error: " + message);
}
View.prototype.createAttributeDict = function(node) {
	var dict = new Object();
	for(var i = 0; i < node.attributes.length; i++) {
		var a = node.attributes[i];
		dict[a.localName] = a.nodeValue;
	}
	return dict;
}
View.prototype.fireEvent = function(el, eventid) {
	console.log("firing event.");
	if(this.eventEndpoint_ == null) return;
	
	var event = this.events_[eventid];
	
	if(!event) {
		console.log("event not found.");
		return;
	}
	
	var url = "";
	var method = "GET";
	if(event.gameEvent == "start") {
		url = replacePattern(this.startEndpoint_.url, event);
		method = this.startEndpoint_.method;
	}
	else if(event.gameEvent == "join") {
		url = replacePattern(this.joinEndpoint_.url, event);
		method = this.joinEndpoint_.method;
	}
	else {
		url = replacePattern(this.eventEndpoint_.url, event);
		method = this.eventEndpoint_.method;
	}
		
	console.log("data: " + event.payload);
	
	var self = this;
	
	var xhr = new XMLHttpRequest();
	xhr.open(method, url, true);
	xhr.onreadystatechange = function() {
		if(xhr.readyState == 4) {
			if(xhr.status == 200) {
				self.handleEventSuccess(el, event, xhr.responseXML);
			}
			else if(xhr.status == 400) {
				self.handleEventError(el, event, xhr.responseXML);
			}
		}
		
	};
	xhr.send(event.payload);
};
