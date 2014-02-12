package com.livegameengine.scxml.js;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.scxml.EventDispatcher;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.xml.XMLObject;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;

import com.livegameengine.config.Config;
import com.livegameengine.model.Game;

public class SendEventJSFunction extends ScriptableObject implements Function {
	private EventDispatcher dispatcher = null;
	
	public SendEventJSFunction(EventDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if(args.length < 2) {
			return null;
		}
		
		// the first argument should be the event name
		String event = args[0].toString();
		
		Map<String,Object> params = new HashMap<String,Object>();
		
		// the second argument should be a javascript object used as a set of name/value pairs
		if(args[1] != null && Scriptable.class.isAssignableFrom(args[1].getClass())) {
			Scriptable s = (Scriptable)args[1];
			
			for(Object key : s.getIds()) {
				if(String.class.isAssignableFrom(key.getClass())) {
					params.put((String)key, s.get((String)key, scope));
				}
				else if(Integer.class.isAssignableFrom(key.getClass())) {
					params.put(key.toString(), s.get((Integer)key, scope));
				}
			}
		}
		
		Object content = null;
		
		// the third argument should be an object of content (either xml or a string) that we will just pass along 
		if(args.length >= 3) {
			content = args[2];
		}
		
		// now call the send method
		dispatcher.send("javascript-send", 
				Config.getInstance().getGameEventTarget(), 
				Config.getInstance().getGameEventTargetType(), 
				event, params, null, 0, content, null);
		
		return null;
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		return this;
	}

	@Override
	public String getClassName() {
		return this.getClass().getSimpleName();
	}

}
