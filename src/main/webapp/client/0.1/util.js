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

function replacePatternNode(node, dict) {
	if(!node) return null;
	
	var doc = node.ownerDocument;
	
	switch(node.nodeType) {
	case Node.ELEMENT_NODE:
		var ret = doc.createElementNS(node.namespaceURI, node.localName);
		for(var i = 0; i < node.childNodes.length; i++) {
			var n = node.childNodes[i];
			ret.appendChild(replacePatternNode(n, dict));
		}
		return ret;
		break;
	case Node.ATTRIBUTE_NODE:
		var ret = doc.createAttributeNS(node.namespaceURI, node.localName);
		ret.nodeValue = replacePattern(node.nodeValue, dict);
		return ret;
		break;
	case Node.TEXT_NODE:
		return doc.createTextNode(replacePattern(node.nodeValue, dict));
		break;
	case Node.DATA_SECTION_NODE:
		return doc.createCDATASection(replacePattern(node.nodeValue, dict));
		break;
	default:
		return node.cloneNode(true);
		break;
	}
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
		if(n.nodeName) {
			var handler = nodehandlers[n.nodeName];
			if(typeof handler == "function") {
				handler.apply(scope, [n]);
			}
		}
	}
}
