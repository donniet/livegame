package com.livegameengine.model;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import org.mozilla.javascript.Scriptable;

public class NameValuePair<ValueType> implements Scriptable {
	private Scriptable parent_, prototype_;
	private boolean readonly_ = true;
	
	private String name;
	private ValueType value;
	
	public NameValuePair(String name, ValueType value) {
		this.name = name;
		this.value = value;
	}
	public NameValuePair(String name, ValueType value, boolean readonly) {
		this(name, value);
		readonly_ = readonly;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ValueType getValue() {
		return value;
	}
	public void setValue(ValueType value) {
		this.value = value;
	}
	
	

	@Override
	public void delete(String arg0) {}

	@Override
	public void delete(int arg0) {}

	@Override
	public Object get(String arg0, Scriptable arg1) {
		if(arg0.equals("name"))
			return getName();
		else if(arg0.equals("value"))
			return getValue();
		
		return NOT_FOUND;
	}

	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}

	@Override
	public String getClassName() {
		return "NameValuePair";
	}

	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return String.format("[object %s]", getClassName());
	}

	@Override
	public Object[] getIds() {
		return new Object[] { "name", "value" };
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
		String[] ids = (String[])getIds();
		
		return Arrays.binarySearch(ids, arg0) >= 0;
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
	public void put(String arg0, Scriptable arg1, Object arg2) {	
		if(readonly_) return;
		
		if(arg0.equals("name"))
			setName(arg2.toString());
		else if(arg0.equals("value"))
			setValue((ValueType)arg2);
	}

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
