
function replacePattern(pattern, dict) {
	var m = null;
	var start = 0;
	var ret = "";
	while((m = replacePattern.TemplatingRegex.exec(pattern)) !== null) {
		ret += pattern.substring(start, m.index);
		
		var str = "";
		
		with(dict) {
			try {
				str = eval(m[1]);
			}
			catch(e) {
				str = "[error: '" + e.toString() + "']";
			}
		}
				
		ret += str;
		start = m.index + m[0].length;
	}
	if(start < pattern.length) {
		ret += pattern.substring(start, pattern.length);
	}
	
	return ret;
}
replacePattern.TemplatingRegex = new RegExp("\{([^\}]+)\}", "g");


//has trouble parsing this date: 	2012-12-27T02:09:29.046GMT
// parses it as: 					2012-12-27T02:00:29.046GMT
var iso8601datepattern = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(\.\d+)((([-+])(\d{2}):(\d{2}))|UTC|GMT|Z)$/;

function dateFromString(s) {
	var m = iso8601datepattern.exec(s);
	
	if(m) {	
		
		var millis = Math.floor(1000.0 * parseFloat(m[7],10));
		
		var d = Date.UTC(
			parseInt(m[1], 10), parseInt(m[2], 10)-1, parseInt(m[3], 10),
			parseInt(m[4], 10), parseInt(m[5], 10), parseInt(m[6], 10), millis);
		
		switch(m[8]) {
		case "UTC":
		case "GMT":
		case "Z":
			break;
		default:
			var minutes = parseInt(m[11]) * 60 + parseInt(m[12]);
		
			if(m[10] == "-") {
				d -= minutes * 60000;
			}
			else {
				d += minutes * 60000;
			}
			break;
		}

		var ret = new Date(d);

		return ret;
	}
	
	return null;
}
function stringFromDate(date) {
	function pad(n, digs) {
		if(typeof digs == "undefined") digs = 2;
		
		var s = n.toString();
		while(s.length < digs) {
			s = '0' + s;
		}
		return s;
	};
	
	//var months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

	var yyyy = date.getUTCFullYear();
	//var MMM  = months[date.getUTCMonth()];
	var MM  = pad(date.getUTCMonth()+1);
	var dd   = pad(date.getUTCDate());
	var hh   = pad(date.getUTCHours());
	var mm  = pad(date.getUTCMinutes());
	var ss   = pad(date.getUTCSeconds());
	var ssss = pad(date.getUTCMilliseconds(),3);
	
	//return dd + ' ' + MMM + ' ' + yyyy + ' ' + hh + ':' + mm2 + ':' + ss + '.' + ssss +  'GMT';
	//return yyyy +'-' +mm1 +'-' +dd +'T' +hh +':' +mm2 +':' +ss +'.' +ssss;
	return yyyy + "-" + MM + "-" + dd + "T" + hh + ":" + mm + ":" + ss + "." + ssss + "GMT";
}


function nodeText(node) {
	if(!node) return null;
	
	var ret = "";
	for(var i = 0; i < node.childNodes.length; i++) {
		var n = node.childNodes[i];
		if(n.nodeType == Node.TEXT_NODE) {
			ret += n.nodeValue;
		}
	}
	return ret;
}

function emptyNode(node) {
	while(node.firstChild) {
		node.removeChild(node.firstChild);
	}
}

function forChildNodes(node, nodehandlers, scope) {
	if(!scope) scope = this;
	
	if(node && nodehandlers) for(var i = 0; i < node.childNodes.length; i++) {
		var n = node.childNodes[i];
		if(n.localName) {
			var handler = nodehandlers[n.localName];
			if(typeof handler == "function") {
				handler.apply(scope, [n]);
			}
		}
	}
}

function ClientMessage(n) {
	this.messageDate = null;
	this.key = null;
	this.event = null;
	this.params = new Object();
	this.content = null;
			
	this.parse(n);
}
ClientMessage.prototype.select = function(string) {
	if(!this.content) return null;
	
	var doc = this.content.ownerDocument;
	
	return doc.evaluate(string, this.content, View.getNamespaceResolver(), XPathResult.ANY_TYPE, null);
}
ClientMessage.prototype.parse = function(node) {
	if(!node) return;
	
	var keyAttr = node.attributes["key"];
	if(keyAttr) this.key = keyAttr.nodeValue;
	
	forChildNodes(node, {
		"messageDate": function(node) {
			this.messageDate = dateFromString(nodeText(node));
		},
		"event": function(node) {
			this.event = nodeText(node);
		},
		"param": function(node) {
			var nameAttr = node.attributes["name"];
			//console.log("nameAttr: " + nameAttr);
			//console.log("this.params: " + this.params);
			if(nameAttr) {
				this.params[nameAttr.nodeValue] = nodeText(node);
			}
		},
		"content": function(node) {
			this.content = node;
		}
	}, this);
}

function ClientMessageChannel(messagesUrl, since) {
	this.messagesUrl = messagesUrl;
	this.messagesMethod = "GET";
	this.interval = null;
	this.requestInProgress = false;
	this.since = since;
	
	this.intervalTime = 1000;
	
	// add a millisecond buffer on all latest messages
	this.messageDelta = 1;
}
ClientMessageChannel.prototype.open = function() {
	//console.log("channel openning...");
	
	if(this.interval != null) return;
	
	var self = this;
	this.interval = setInterval(function() {
		self.oninterval();
	}, this.intervalTime)
}
ClientMessageChannel.prototype.close = function() {
	if(this.interval == null) return;
	
	clearInterval(this.interval);
	this.interval = null;
}
ClientMessageChannel.prototype.oninterval = function() {
	if(this.requestInProgress) return;
	
	this.requestInProgress = true;
	
	var xhr = new XMLHttpRequest();
	xhr.open(this.messagesMethod, this.messagesUrl + "?since=" + escape(stringFromDate(this.since)), true);
	
	var self = this;
	xhr.onreadystatechange = function() {
		//console.log("ready state: " + xhr.readyState);
		//console.log("status: " + xhr.status);
		
		if(xhr.readyState == 4) {
			if(xhr.status == 200) {
				self.onclientmessages(xhr.responseXML);
			}
			else if(xhr.status == 204) {
				// do nothing, no additional data
			}
			else if(xhr.status >= 400 && xhr.status < 500) {
				self.onerror(xhr.responseXML);
			}
			else if(xhr.status < 200 || xhr.status >= 500) {
				// server error
				self.onerror();
			}
			self.requestInProgress = false;
		}
	}
	
	try {
		xhr.send();
	}
	catch(e) {
		console.log("error openning connection, shutting down");
		this.close();
	}
}
ClientMessageChannel.prototype.onclientmessages = function(messagesXml) {
	var ser = new XMLSerializer();
	
	//console.log("messages: " + ser.serializeToString(messagesXml));
	
	forChildNodes(messagesXml, {
		"messages": function(messagesNode) {
			//console.log("latestDate: " +  messagesNode.attributes["latestDate"].nodeValue);
			
			var latestNode = messagesNode.attributes["latestDate"];
			if(latestNode) {
				var d = dateFromString(latestNode.nodeValue);
				
				if(d < this.since) {
					console.log("something strange, latestDate < since");
				}
				else {
					this.since = new Date(d.getTime() + this.messageDelta);
				}
				
				//console.log("this.since == " + stringFromDate(this.since));
			}
			
			forChildNodes(messagesNode, {
				"message": function(messageNode) {
					Event.fire(this, "clientmessage", [new ClientMessage(messageNode)]);
				}
			}, this);
		}
	}, this);
}
ClientMessageChannel.prototype.onerror = function(errorXml) {
	//console.log("there was an error, shutting down...");
	this.close();
}

function GameError(node) {
	this.message = "";
	
	if(node) this.parse(node);
}
GameError.prototype.parse = function(node) {
	if(!node) return;
	
	forChildNodes(node, {
		"error": function(errorNode) {
			this.message = nodeText(errorNode);
		}
	}, this);
}

function ErrorDisplay(node) {
	this.element = null;
	this.timeout = 5000;
	
	this.timeoutHandle_ = null;
	
	this.parse(node);
}
ErrorDisplay.prototype.parse = function(node) {
	if(!node) return;
	
	this.element = node.parentNode;
	
	this.errorClassName = node.attributes["errorClassName"] ? node.attributes["errorClassName"].nodeValue : null;
	this.noErrorClassName = node.attributes["noErrorClassName"] ? node.attributes["noErrorClassName"].nodeValue : null;
	
	var timeoutAttr = node.attributes["timeout"];
	var timeoutValue = this.timeout;
	if(timeoutAttr) {
		timeoutValue = parseInt(timeoutAttr.nodeValue);
		if(timeoutValue != Number.NaN) {
			this.timeout = timeoutValue;
		}
	}
}
ErrorDisplay.prototype.clearError = function() {
	emptyNode(this.element);
	if(this.noErrorClassName != null) {
		this.element.className = this.noErrorClassName;
	}
	Event.fire(this, "errorCleared", []);
}
ErrorDisplay.prototype.displayError = function(gameError) {
	if(this.timeoutHandle_ != null) {
		clearTimeout(this.timeoutHandle_);
		this.timeoutHandle_ = null;
	}
	
	this.clearError();
	
	this.element.appendChild(document.createTextNode(gameError.message));
	if(this.errorClassName != null) {
		this.element.className = this.errorClassName;
	}
	Event.fire(this, "errorShown", []);
	
	if(this.timeout > 0) {
		var self = this;
		this.timeoutHandle_ = setTimeout(function() {
			self.clearError();
			self.timeoutHandle_ = null;
		}, this.timeout);
	}
}

function ViewConstructor() {	
	this.channel = null;
	this.clientMessageChannelUrl_ = null;
	this.serverLoadTime_ = new Date();
	this.events_ = new Object();
	this.handlers_ = new Object();
	this.placeholders_ = new Object();
	this.placeholdersDict_ = new Object();
	this.eventEndpoint_ = null;
	this.gameEventEndpoint_ = null;
	this.gameViewNamespace_ = "";
	this.gameNamespace_ = "";
	
	this.namespacesByPrefix_ = new Object();
	
	this.errorDisplay_ = null;
	
	var self = this;
	window.onload = function() { self.handleLoad(); }
}
ViewConstructor.prototype.getNamespaceResolver = function() {
	var self = this;
	
	return function(prefix) {
		switch(prefix) {
		case "game": return self.gameNamespace_;
		case "view": return self.gameViewNamespace_;
		default: return self.namespacesByPrefix_[prefix] || null;
		}
	}
}
ViewConstructor.prototype.registerNamespacePrefix = function(prefix, namespaceUri) {
	this.namespacesByPrefix_[prefix] = namespaceUri;
}
ViewConstructor.prototype.setGameViewNamespace = function(ns) {
	this.gameViewNamespace_ = ns;
}
ViewConstructor.prototype.setGameNamespace = function(ns) {
	this.gameNamespace_ = ns;
}
ViewConstructor.prototype.setEventEndpoint = function(obj) {
	this.eventEndpoint_ = obj;
}
ViewConstructor.prototype.setGameEventEndpoint = function(obj) {
	this.gameEventEndpoint_ = obj;
}
ViewConstructor.prototype.registerEvents = function(arr) {
	for(var i = 0; i < arr.length; i++) {
		var e = arr[i];
		
		this.events_[e.id] = e;
	}
}
ViewConstructor.prototype.registerEventHandlerPlaceholder = function(arr) {
	for(var i = 0; i < arr.length; i++) {
		var h = arr[i];
		//console.log("registering handler: " + h.event);
		
		if(typeof this.placeholders_[h.event] == "undefined") {
			this.placeholders_[h.event] = new Array();
			this.placeholdersDict_[h.event] = new Object();
		}
		
		this.placeholders_[h.event].push(h);
		if(typeof h.key == "string" && h.key != "")
			this.placeholdersDict_[h.event][h.key] = h;
	}
}
ViewConstructor.prototype.registerEventHandler = function(arr) {
	for(var i = 0; i < arr.length; i++) {
		var h = arr[i];
		
		this.handlers_[h.event] = h;
	}
}

ViewConstructor.prototype.handleGameViewElement = function(parent, node) {
	switch(node.localName) {
	case "event":
		this.handleGameViewEventElement(parent, node);
		break;
	case "eventHandler":
		this.handleGameViewEventHandler(parent, node);
		break;
	case "eventHandlerPlaceholder":
		this.handleGameViewEventHandlerPlaceholder(parent, node);
		break;
	case "errorDisplay":
		this.errorDisplay_ = new ErrorDisplay(node);
		break;
	case "clientNamespace":
		var prefix = "";
		var namespaceUri = "";
		
		if(node.getAttribute("prefix")) prefix = node.getAttribute("prefix");
		if(node.getAttribute("namespace-uri")) namespaceUri = node.getAttribute("namespace-uri");
		
		this.registerNamespacePrefix(prefix, namespaceUri);
		break;
	}
}
ViewConstructor.prototype.handleGameViewEventElement = function(parent, node) {
	var params = new Array();
	var content = "";
	
	var ser = new XMLSerializer();
	
	for(var i = 0; i < node.childNodes.length; i++) {
		var n = node.childNodes[i];
		
		if(n.localName == "param" && n.namespaceURI == this.gameViewNamespace_) {
			params.push({ "name": n.getAttribute("name"), "value": n.getAttribute("value")});
		}
		else {
			content += ser.serializeToString(n);
		}
	}
	
	var on = node.getAttribute("on");
	var event = node.getAttribute("event");
	var gameEvent = node.getAttribute("gameEvent");
	
	//console.log("event: " + on + ", content: " + content);
	//console.log("gameEvent: " + gameEvent);
	
	var e = {
		"on": on,
		"event": event ? event : on,
		"gameEvent": gameEvent ? gameEvent : "",
		"payload": content,
		"params": params
	};
	/*
	var self = this;
	parent["on" + on] = function() {
		self.trigger(parent, e);
	}
	*/
	//parent.setAttribute("class", "added");
	Event.addListener(parent, on, function() {
		console.log("event: " + e.event + ", gameEvent: " + e.gameEvent);
		this.trigger(parent, e);
	}, this);
	
}
ViewConstructor.prototype.handleGameViewEventHandler = function(parent, node) {
	var h = {
		"event": node.attributes["event"] ? node.attributes["event"].nodeValue : "",
		"keyPattern": node.attributes["keyPattern"] ? node.attributes["keyPattern"].nodeValue : ""
	};
	
	this.registerEventHandler([h]);
}
ViewConstructor.prototype.handleGameViewEventHandlerPlaceholder = function(parent, node) {
	var h = {
		"mode": node.attributes["mode"] ? node.attributes["mode"].nodeValue : "",
		"event": node.attributes["event"] ? node.attributes["event"].nodeValue : "",
		"key": node.attributes["key"] ? node.attributes["key"].nodeValue : "",
		"condition": node.attributes["condition"] ? node.attributes["condition"].nodeValue : "",
		"content": parent,
		"attributes": new Array(),
		"params": new Array()
	};
	
	forChildNodes(node, {
		"attribute": function(attributeNode) {
			h.attributes.push({
				name: attributeNode.getAttribute("name"),
				value: nodeText(attributeNode)
			});
		},
		"param": function(paramNode) {
			h.params.push({
				name: paramNode.getAttribute("name"),
				value: nodeText(paramNode)
			});
		}
	}, this);
	
	h.params.sort(function(a,b) {
		if(a.name < b.name) return -1;
		else if(a.name > b.name) return 1;
		else return 0;
	});
	
	this.registerEventHandlerPlaceholder([h]);
}
ViewConstructor.prototype.parseDocumentBody = function(node) {
	if(!node) return;
	
	if(node.namespaceURI == this.gameViewNamespace_) {
		this.handleGameViewElement(node.parentNode, node);
		node.parentNode.removeChild(node);
	}
	
	for(var i = 0; i < node.childNodes.length; i++) {
		this.parseDocumentBody(node.childNodes[i]);
	}
}
ViewConstructor.prototype.parseMessageResponse = function(parent, messageContent) {
	if(messageContent.namespaceURI == this.gameViewNamespace_) {
		// handle these separately.
		this.handleGameViewElement(parent, messageContent);
	}
	else {
		var imported = null;
		
		switch(messageContent.nodeType) {
		case Node.ELEMENT_NODE:
			if(!messageContent.namespaceURI || messageContent.namespaceURI == "") {
				imported = document.createElement(messageContent.localName);
			}
			else {
				imported = document.createElementNS(messageContent.namespaceURI, messageContent.localName);
			}
			
			for(var i = 0; i < messageContent.attributes.length; i++) {
				var a = messageContent.attributes[i];
				
				var attr = null;
				//console.log("attribute namespace: '" + a.namespaceURI + "'");
				
				imported.setAttribute(a.nodeName, a.nodeValue);	
			}
			break;
		case Node.TEXT_NODE:
			imported = document.createTextNode(messageContent.nodeValue);
			break;
		case Node.DATA_SECTION_NODE:
			imported = document.createCDATASection(messageContent.nodeValue);
			break;
		case Node.ENTITY_REFERENCE_NODE:
		case Node.ENTITY_NODE:
			// i don't think these are used anymore
			break;
		case Node.DOCUMENT_NODE:
			imported = null;
			break;
		}
		
		if(imported != null) {
			parent.appendChild(imported);
		}
		else {
			imported = parent;
		}
		
		for(var i = 0; i < messageContent.childNodes.length; i++) {
			var n = messageContent.childNodes[i];
			
			this.parseMessageResponse(imported, n);
		}	
	}
}
ViewConstructor.prototype.renderEventPlaceholder = function(clientMessage, placeholder) {
	if(placeholder.content) {	
		if(placeholder.mode == "attribute") {
			for(var j = 0; j < placeholder.attributes.length; j++) {
				var a = placeholder.attributes[j];
				placeholder.content.setAttribute(a.name, a.value);
			}
		}
		else {
			if(placeholder.mode == "replace") {
				emptyNode(placeholder.content);
			}
			for(var j = 0; j < clientMessage.content.childNodes.length; j++) {
				this.parseMessageResponse(placeholder.content, clientMessage.content.childNodes[j]);
			}
		}
	}
}
ViewConstructor.prototype.handleClientMessage = function(clientMessage) {
	//console.log("handling client message: " + clientMessage);
	
	if(!clientMessage) return;
	
	console.log("clientMessage event: " + clientMessage.event);
	
	// first check the registered event handlers
	var h = this.handlers_[clientMessage.event];
	if(typeof h != "undefined" && h.keyPattern != "") {
		var key = replacePattern(h.keyPattern, clientMessage.params);
		
		var placeholderDict = this.placeholdersDict_[clientMessage.event];
		if(placeholderDict && typeof placeholderDict[key] != "undefined") {
			var p = placeholderDict[key];
			
			this.renderEventPlaceholder(clientMessage, p);
		}
	}
	else {
		var placeholders = this.placeholders_[clientMessage.event];
		
		console.log("placeholder: " + (placeholders ? placeholders.toString() : "null"));
			
		if(placeholders && placeholders.length > 0) {
			for(var i = 0; i < placeholders.length; i++) {
				var p = placeholders[i];
				console.log("checking handler number: " + i + ": " + p.condition);
				
				if(p.condition != "") {
					console.log("condition: " + p.condition);
					var r = false;
					with(clientMessage.params) {
						try {
							r = eval(p.condition);
						}
						catch(e) {
							console.log("condition error: " + e);
							r = false;
						}
					}
					
					if(typeof r == "undefined" || r == null) {
						continue;
					}
					else if(r instanceof XPathResult) {
						console.log("xpath result: " + r.resultType + ", " + r.booleanValue);
						
						
						if(r.resultType == XPathResult.BOOLEAN_TYPE) {
							if(!r.booleanValue) continue;
						}
						else if(r.resultType == XPathResult.NUMBER_TYPE) {
							if(r.numberValue == 0) continue;
						}
						else if(r.resultType == XPathResult.STRING_TYPE) {
							if(r.stringValue == null || r.stringValue == "" || r.stringValue == "false") continue;
						}
						else {
							//TODO: handle the node set types
							console.log("resultType: " + r.resultType);
							if(!r) continue;
						}
					}
					else if(!r) {
						continue;
					}
				}

				this.renderEventPlaceholder(clientMessage, p);
			}
		}
	}
}


ViewConstructor.prototype.handleEventSuccess = function(el, event, responseXML) {
	//console.log("event success: " + (responseXML ? responseXML.toString() : "null"));
	//this.channel.oninterval();
}
ViewConstructor.prototype.handleEventError = function(el, event, responseXML) {
	//console.log("event error: " + responseXML ? responseXML.toString() : "null");
	var error = new GameError(responseXML);
	
	if(!this.errorDisplay_) {
		alert(error.message);
	}
	else {
		this.errorDisplay_.displayError(error);
	}
}

ViewConstructor.prototype.trigger = function(el, e) {
	if(typeof e == "undefined") {
		//console.log("unknown event id: '" + eventid + "'");
		return;
	}
	
	console.log("triggering event: " + e.event);
	
	var url = "";
	var method = "POST";
	if(!e.gameEvent || e.gameEvent == "") {		
		url = replacePattern(this.eventEndpoint_.url, e);
		method = this.eventEndpoint_.method;
	}
	else {
		url = replacePattern(this.gameEventEndpoint_.url, e);
		method = this.gameEventEndpoint_.method;
	}
	
	
	var self = this;
	
	var xhr = new XMLHttpRequest();
	xhr.open(method, url, true);
	xhr.onreadystatechange = function() {
		if(xhr.readyState == 4) {
			if(xhr.status == 200) {
				self.handleEventSuccess(el, e, xhr.responseXML);
			}
			else if(xhr.status == 400) {
				self.handleEventError(el, e, xhr.responseXML);
			}
		}
		
	};
	xhr.send(e.payload);
}
ViewConstructor.prototype.setClientMessageChannelUrl = function(url) {
	this.clientMessageChannelUrl_ = url;
}
ViewConstructor.prototype.setServerLoadTime = function(d) {
	this.serverLoadTime_ = dateFromString(d);
}
ViewConstructor.prototype.handleLoad = function() {
	var self = this;
	
	this.parseDocumentBody(document.body);
	
	//console.log("View loading...");
	if(this.clientMessageChannelUrl_ != null) {
		if(this.serverLoadTime_ == null) this.serverLoadTime_ = new Date();
		
		console.log("server load time: " + this.serverLoadTime_.toString());
		
		this.channel = new ClientMessageChannel(this.clientMessageChannelUrl_, this.serverLoadTime_);
		Event.addListener(this.channel, "clientmessage", function(clientMessage) {
			self.handleClientMessage(clientMessage);
		});
		this.channel.open();
	}
	
	Event.fire(this, "windowload", []);
}
View = new ViewConstructor();