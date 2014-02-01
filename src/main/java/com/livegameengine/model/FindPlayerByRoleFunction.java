package com.livegameengine.model;

import java.util.Iterator;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class FindPlayerByRoleFunction implements Function {
	private Game game_;
	
	private Scriptable parent_ = null, prototype_ = null;
	
	public FindPlayerByRoleFunction(Game g) {
		game_ = g;
	}
	
	private Player findPlayerByRole(String role) {
		for(Iterator<Player> i = game_.getPlayers().iterator(); i.hasNext();) {
			Player p = i.next();
			
			if(p.getRole().equals(role)) 
				return p;
		}
		
		return null;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if(args.length == 0) 
			return null;
		
		return findPlayerByRole(args[0].toString());
	}
	

	@Override
	public void delete(String arg0) {}

	@Override
	public void delete(int arg0) {}

	@Override
	public Object get(String arg0, Scriptable arg1) {
		return NOT_FOUND;
	}

	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}

	@Override
	public String getClassName() {
		return "GetPlayerByRoleFunction";
	}

	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return "[function getPlayerByRole]";
	}

	@Override
	public Object[] getIds() {
		return new Object[] {};
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
		return false;
	}

	@Override
	public boolean has(int arg0, Scriptable arg1) {
		return false;
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
	

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		if(args.length == 0) 
			return null;
		
		return findPlayerByRole(args[0].toString());
	}

}
