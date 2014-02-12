package com.livegameengine.scxml.js;

import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.Node;

import org.apache.commons.scxml.EventDispatcher;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.xmlimpl.XMLLibImpl; 

import com.livegameengine.config.Config;
import com.livegameengine.scxml.model.SendWatcherEvent;

public class SendWatcherEventJsFunction extends ScriptableObject implements Function {
	private EventDispatcher dispatcher = null;
	
	public SendWatcherEventJsFunction(EventDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if(args.length < 1) return null;
		
		// first argument should be the event name
		String event = args[0].toString();
		
		// second argument should be a scriptable object of params
		Map<String,Object> params = new HashMap<String,Object>();
		
		
		if(args.length > 1 && args[1] != null && Scriptable.class.isAssignableFrom(args[1].getClass())) {
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
		if(args.length > 2) {
			if(Node.class.isAssignableFrom(args[2].getClass())) {
				content = (Node)content;
			}
			else if(Scriptable.class.isAssignableFrom(args[2].getClass())) {
				try {
					content = XMLLibImpl.toDomNode(args[2]);
				}
				catch(Exception e) {
					content = args[2].toString();
				}
			}
			else {
				content = args[2].toString();
			}
		}
		
		dispatcher.send("game.send.gameEvent.id", 
				Config.getInstance().getWatcherEventTarget(), 
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
		return SendWatcherEvent.class.getName();
	}
	
}
