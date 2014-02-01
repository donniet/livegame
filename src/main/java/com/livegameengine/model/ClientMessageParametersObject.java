package com.livegameengine.model;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Scriptable;

public class ClientMessageParametersObject implements Scriptable {
	private Scriptable parent_, prototype_;
	
	private ClientMessage message;
	
	public ClientMessageParametersObject(ClientMessage message) {
		this.message = message;
	}

	@Override
	public void delete(String arg0) {}

	@Override
	public void delete(int arg0) {}

	@Override
	public Object get(String arg0, Scriptable arg1) {
		NameValuePair<String> ret = message.getParameter(arg0);
		
		if(ret != null) return ret;
		
		return NOT_FOUND;
	}

	@Override
	public Object get(int arg0, Scriptable arg1) {
		NameValuePair<String> ret = message.getParameter(arg0);
		
		if(ret != null) return ret;
		
		return NOT_FOUND;
	}

	@Override
	public String getClassName() {
		return "ClientMessageParametersObject";
	}

	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return String.format("[object %s]", this.getClassName());
	}

	@Override
	public Object[] getIds() {
		List<Object> ids = new ArrayList<Object>();
		
		for(int i = 0; i < message.parameterNames.size(); i++) {
			ids.add(message.parameterNames.get(i));
			ids.add(i);
		}
		
		return ids.toArray();
	}

	@Override
	public Scriptable getParentScope() {
		return parent_;
	}

	@Override
	public Scriptable getPrototype() {
		return prototype_;
	}

	@Override
	public boolean has(String arg0, Scriptable arg1) {
		return (message.getParameter(arg0) != null);
	}

	@Override
	public boolean has(int arg0, Scriptable arg1) {
		return (message.getParameter(arg0) != null);
	}

	@Override
	public boolean hasInstance(Scriptable value) {
        Scriptable proto = value.getPrototype();
        while (proto != null) {
            if (proto.equals(this))
                return true;
            proto = proto.getPrototype();
        }

        return false;
	}

	@Override
	public void put(String arg0, Scriptable arg1, Object arg2) {}

	@Override
	public void put(int arg0, Scriptable arg1, Object arg2) {}

	@Override
	public void setParentScope(Scriptable arg0) {
		parent_ = arg0;
	}

	@Override
	public void setPrototype(Scriptable arg0) {
		prototype_ = arg0;
	}

}
