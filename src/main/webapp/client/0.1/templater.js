function TemplateHandler(template) {
    this.template_ = template;
}
TemplateHandler.prototype.renderIn = function(temp, obj, parent) {}

function EvaluatingBase(template, data) {
	this.__template = template;
	this.__data = data;
	this.__xpathresult = null;
	
	this.__arrayindex = 0;
	this.__arrayresult = null;
}
EvaluatingBase.prototype.evalXPath = function(xpath) {
   if(!this.__data || !(this.__data instanceof Node)) return null;
   
    var doc = this.__data.ownerDocument;
    var nsResolver = this.__template.createNamespaceResolver();

    var result = doc.evaluate(xpath, this.__data, nsResolver, XPathResult.ANY_TYPE, null);
    
    var ret = new EvaluatingBase(this.__template);
    
    switch(result.resultType) {
    case XPathResult.NUMBER_TYPE:
    	ret.__data = result.numberValue;
    	break;
    case XPathResult.STRING_TYPE:
    	ret.__data = result.stringValue;
    	break;
    case XPathResult.BOOLEAN_TYPE:
    	ret.__data = result.booleanValue;
    	break;
    case XPathResult.ANY_UNORDERED_NODE_TYPE:
    case XPathResult.FIRST_ORDERED_NODE_TYPE:
    	ret.__data = result.singleNodeValue;
    	ret.__xpathresult = result;
    	break;
    case XPathResult.UNORDERED_NODE_ITERATOR_TYPE:
    case XPathResult.ORDERED_NODE_ITERATOR_TYPE:
    case XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE:
    case XPathResult.ORDERED_NODE_SNAPSHOT_TYPE:
    	ret.__data = result.iterateNext();
    	ret.__xpathresult = result;
    	break;
    }
}
EvaluatingBase.prototype._next() = function() {
	if(this.__xpathresult != null) {
		this.__data = this.__xpathresult.iterateNext();
		return !!this.__data;
	}
	else if(this.__arrayresult != null) {
		if(this.__arrayIndex >= this.__arrayresult.length) {
			return false;
		}
		else {
			this.__data = this.__arrayresult[this.__arrayindex++];
			return true;
		}
	}
}


function Template(templateNode, doc) {
    this.templateNode_ = templateNode;
    this.doc_ = doc ? doc : document;
    this.debug_ = true;
   
    this.templatingRegex_ = new RegExp("\{([^\{\}]+)\}", "g");
    this.escapedLeft_ = new RegExp("\{\{", "g");
    this.escapedRight_ = new RegExp("\}\}", "g");
    
    this.nsDict_ = new Object();
}
Template.handlers_ = new Object();
Template.registerHandler = function(namespace, localName, handler) {
    if(!Template.handlers_[namespace]) {
        Template.handlers_[namespace] = new Object();
    }
    Template.handlers_[namespace][localName] = new handler(this);
}

Template.prototype.addNamespacePrefix = function(prefix, namespaceUri) {
	this.nsDict_[prefix] = namespaceUri;
}
Template.prototype.createNamespaceResolver = function() {
	var self = this;
	return function(prefix) {
		return self.nsDict_[prefix];
	}
}

Template.prototype.isDebug = function() { return this.debug_; };

Template.prototype.replacePattern = function(pattern, obj) {
    var m = null;
    var start = 0;
    var ret = "";
    while((m = this.templatingRegex_.exec(pattern)) !== null) {
        if(m.index > 0 && pattern.charAt(m.index - 1) == "{") {
            // double {, pattern doesn't count
            ret += m[0];
        }
        else {
            ret += pattern.substring(start, m.index);

            var str = "";

            with(obj) {
                try {
                    str = eval(m[1]);
                }
                catch(e) {
                    str = "[error: '" + e.toString() + "']";
                }
            }

            ret += str;
        }
        start = m.index + m[0].length;
    }
    if(start < pattern.length) {
        ret += pattern.substring(start, pattern.length);
    }

    return ret.replace(this.escapedLeft_, "{").replace(this.escapedRight_, "}");
}
Template.prototype.renderIn = function(obj, parent) {
    var o = Object.create(new EvaluatingBase(), obj);

    this._renderInHelper(this.templateNode_, o, parent);
}
Template.prototype._renderInHelper = function(temp, obj, parent) {
    if(!temp) return null;
   
    switch(temp.nodeType) {
    case Node.ELEMENT_NODE:
        this._renderInHelperElement(temp, obj, parent);
    case Node.ATTRIBUTE_NODE:
        this._renderInHelperAttribute(temp, obj, parent);
    case Node.TEXT_NODE:
        this._renderInHelperTextNode(temp, obj, parent);
    case Node.DATA_SECTION_NODE:
        this._renderInHelperCDATA(temp, obj, parent);
    default:
        parent.appendChild(this.doc_.importNode(temp));
    }
}
Template.prototype._renderHelperTextNode = function(temp, obj, parent) {
    return this.doc_.createTextNode(this.replacePattern(temp.nodeValue, obj));
}
Template.prototype._renderHelperCDATA = function(temp, obj, parent) {
    return this.doc_.createCDataNode(this.replacePattern(temp.nodeValue, obj));   
}
Template.prototype._renderHelperAttribute = function(temp, obj, parent) {
    var ns = temp.namespaceURI;
    var n = null;
    if(!ns || ns == "") {
        n = this.doc_.createAttribute(temp.nodeName);
        n.nodeValue = this.replacePattern(temp.nodeValue, obj);
        parent.attributes.setNamedItem(n.nodeName, n);
    }
    else {
        if(Template.handlers_[ns] && Template.handlers_[ns][temp.localName]) {
            var h = Template.handlers_[ns][temp.localName];
            h.renderIn(temp, obj, parent);
        }
        else {
            n = this.doc_.createAttributeNS(ns, temp.localName);
            n.nodeValue = this.replacePattern(temp.nodeValue, obj);
            parent.attributes.setNamedItem(n.nodeName, n);
        }
    }
}
Template.prototype._renderHelperElement = function(temp, obj, parent) {
    var ns = temp.namespaceURI;
   
    if(ns && ns != "" && Template.handlers_[ns] && Template.handlers_[ns][temp.localName]) {
        var h = Template.handlers_[ns][temp.localName];
        h.renderIn(temp, obj, parent);
    }
    else {
        var n = null;
       
        if(ns && ns != "") {
            n = this.doc_.createElementNS(ns, temp.localName);
        }
        else {
            n = this.doc_.createElement(temp.nodeName);
        }
       
        for(var i = 0; i < temp.childNodes.length; i++) {
            var m = temp.childNodes[i];
            var r = this._renderInHelper(m, obj, n);
        }
    }
}

function ForEachTemplateHandler(template) {
    this.template_ = template;
}
ForEachTemplateHandler.prototype.renderIn(temp, obj, parent) {
    // template should be an element node with an attribute expr and as
    var expr = null;
    var as = null;
   
    for(var i = 0; i < temp.attributes.length; i++) {
        var a = temp.attributes[i];
       
        switch(a.localName) {
        case "expr":
            expr = a.nodeValue;
            break;
        case "as":
            expr = a.nodeValue;
            break;
        }
    }   
   
    var error = null;
    var col = null;
   
    if(expr == null || expr == "" || name == null || name == "") {
        error = "null or empty expression or name in foreach statement";
    }
    else {
        with(obj) {
            try {
                col = eval(expr);
            }
            catch(e) {
                error = e.toString();
            }
        }
       
        if(error != null && col != null && typeof col.length == "number") {
           
            for(var i = 0; i < col.length; i++) {
                x[name] =
            }
        }
    }
}

function WithTemplateHandler(template) {
    this.template_ = template;
}
WithTemplateHandler.prototype.renderIn = function(temp, obj, parent) {
    // template should be an element node with an attribute 'expr'
    var expr = null;
    for(var i = 0; i < temp.attributes.length; i++) {
        var a = temp.attributes[i];
       
        if(a.localName == "expr") {
            expr = a.nodeValue;
            break;
        }
    }
   
    var error = null;
    var ret = null;
   
    if(expr == null || expr == "") {
        // null and empty string default to false
        error = "null or empty expression in with statement";
    }
    else {
        with(obj) {
            try {
                ret = eval(expr);
            }
            catch(e) {
                error = e.toString();
            }
        }
       
        if(error == null && ret != null) {
            // should we set prototypes?
           
            for(var i = 0; i < temp.childNodes.length; i++) {
                var m = temp.childNodes[i];
                this.template_._renderInHelper(m, ret, parent);
            }
        }
    }
   
    if(error != null && this.template_.isDebug()) {
        parent.appendChild(this.template_.doc_.createTextNode("[error: '" + error + "']"));
    }   
}

function IfTemplateHandler(template) {
    this.template_ = template;
}
IfTemplateHandler.prototype.renderIn = function(temp, obj, parent) {
    // template should be an element node with an attribute 'test'
    var test = null;
    for(var i = 0; i < temp.attributes.length; i++) {
        var a = temp.attributes[i];
       
        if(a.localName == "test") {
            test = a.nodeValue;
            break;
        }
    }
   
    if(test == null || test == "") {
        // null and empty string default to false
        return;
    }
    else {
        var ret = false;
        var error = null;
       
        with(obj) {
            try {
                ret = eval(test);
            }
            catch(e) {
                error = e.toString();
                ret = false;
            }
        }
       
        if(error != null && this.template_.isDebug()) {
            parent.appendChild(this.template_.doc_.createTextNode("[error: '" + error + "']"));
        }
        else if(!ret) {
            return;
        }
        else {
            for(var i = 0; i < temp.childNodes.length; i++) {
                var m = temp.childNodes[i];
                this.template_._renderInHelper(m, obj, parent);
            }
        }
    }
}