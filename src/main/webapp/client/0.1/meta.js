
function dateFromString(s) {
	if(s == null) return null;
	
	var bits = s.split(/[-T:]/g);
	var d = new Date(bits[0], bits[1]-1, bits[2]);
	d.setHours(bits[3], bits[4], bits[5]);
	
	return d;
}

function GameUser(node) {
	if(node) this.load(node);
}
GameUser.prototype.load = function(node) {
	var ns = Meta.MetaNamespace;
	
	this.key = node.attributes.getNamedItem("key") ? node.attributes.getNamedItem("key").nodeValue : null;
	
	for(var i = 0; i < node.childNodes.length; i++) {
		var n = node.childNodes[i];

		if(n.namespaceURI != ns) continue;
		
		switch(n.localName) {
		case "userid":
			this.userid = nodeText(n);
			break;
		case "nickname":
			this.nickname = nodeText(n);
			break;
		case "hashedEmail":
			this.hashedEmail = nodeText(n);
			break;
		case "connected":
			console.log("connected value: " + nodeText(n));
			
			this.connected = (nodeText(n) == "true");
			break;
		}
	}
}

function Player(node) {
	if(node) this.load(node);
}
Player.prototype.load = function(node) {
	var ns = Meta.MetaNamespace;

	this.key = node.attributes.getNamedItem("key") ? node.attributes.getNamedItem("key").nodeValue : null;
	
	for(var i = 0; i < node.childNodes.length; i++) {
		var n = node.childNodes[i];
		
		if(n.namespaceURI != ns) continue;
		
		switch(n.localName) {
		case "gameUser":
			this.gameUser = new GameUser(n);
			break;
		case "role":
			this.role = nodeText(n);
			break;
		case "playerJoin":
			this.playerJoin = dateFromString(nodeText(n));
			break;
		}
	}
}

function Data(node) {
	if(node) this.load(node);
}
Data.prototype.load = function(node) {
	var ns = Meta.MetaNamespace;

	this.key = node.attributes.getNamedItem("key") ? node.attributes.getNamedItem("key").nodeValue : null;
	
	for(var i = 0; i < node.childNodes.length; i++) {
		var n = node.childNodes[i];
		
		if(n.namespaceURI != ns) continue;
		
		switch(n.localName) {
		case "id":
			this.id = nodeText(n);
			break;
		}
	}
}

function GameState(node) {
	if(node) this.load(node);
}
GameState.prototype.load = function(node) {
	var ns = Meta.MetaNamespace;
	
	this.key = node.attributes.getNamedItem("key") ? node.attributes.getNamedItem("key").nodeValue : null;
	
	for(var i = 0; i < node.childNodes.length; i++) {
		var n = node.childNodes[i];
		
		if(n.namespaceURI != ns) continue;
		
		switch(n.localName) {
		case "datamodel":
			this.datamodel = new Array();
			
			for(var j = 0; j < n.childNodes.length; j++) {
				var m = n.childNodes[j];
				
				if(m.namespaceURI != ns) continue;
				
				switch(m.localName) {
				case "data":
					this.datamodel.push(new Data(m));
					break;
				}
			}
			break;
		case "important":
			this.important = (nodeText(n) == "true");
			break;
		case "stateDate":
			this.stateDate = dateFromString(nodeText(n));
			break;
		case "stateSet":
			this.stateSet = new Array();
			
			for(var j = 0; j < n.childNodes.length; j++) {
				var m = n.childNodes[j];
				
				if(m.namespaceURI != ns) continue;
				
				switch(m.localName) {
				case "state":
					this.stateSet.push(nodeText(m));
					break;
				}
			}
			break;
		case "owner":
			this.owner = new GameUser(n);
			break;
		case "gameTypeKey":
			this.gameTypeKey = nodeText(n);
			break;
		}
	}
}

function Meta(node) {	
	if(node) this.load(node);
}
Meta.prototype.load = function(node) {
	var ns = Meta.MetaNamespace;
	
	this.key = node.attributes.getNamedItem("key") ? node.attributes.getNamedItem("key").nodeValue : null;
	
	for(var i = 0; i < node.childNodes.length; i++) {
		var n = node.childNodes[i];
		
		if(n.namespaceURI != ns) continue;
		
		switch(n.localName) {
		case "players":
			this.players = new Array();
			for(var j = 0; j < n.childNodes.length; j++) {
				var m = n.childNodes[j];
				
				console.log("parsing player: " + m.localName);
				
				if(m.namespaceURI != ns || m.localName != "player") continue;
				
				this.players.push(new Player(m));				
			}
			break;
		case "currentTime":
			this.serverTimeDelta = dateFromString(n.nodeValue) - new Date();
			break;
		case "created":
			this.created = dateFromString(n.nodeValue);
			break;
		case "mostRecentState":
			this.mostRecentState = new GameState(n);
			break;
		
		}
	}
	
}
Meta.MetaNamespace = "http://www.livegameengine.com/schemas/game.xsd";
Meta.MetaUrl = "meta";
Meta.loadMeta = function() {
	var resp = new Object();
	
	setTimeout(function() { Meta.__loadMetaInner(resp) }, 500);
	
	return resp;
}
	
	
Meta.__loadMetaInner = function(ret) {
	var xhr = new XMLHttpRequest();
		
	xhr.open("GET", Meta.MetaUrl, true);
	xhr.onreadystatechange = function() {
		if(xhr.readyState == 4) {
			if(xhr.status == 200) {
				console.log("load success!");
				
				var ns = Meta.MetaNamespace;
				
				for(var i = 0; i < xhr.responseXML.childNodes.length; i++) {
					var n = xhr.responseXML.childNodes[i];
					
					console.log("node: " + n.nodeName);
					
					if(n.namespaceURI == ns && n.localName == "game") {
						console.log("found the game node!");
						
						var meta = new Meta(n);
						
						Event.fire(ret, "load", [meta]);
						console.log("fired load event: " + meta.key);
						
						break;
					}
				}
				
			}
			else {
				Event.fire(ret, "error", []);
			}
		}
	}
	xhr.send();
}