package com.livegameengine.model;

import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListHelper implements NodeList {
	List<Object> list_ = null;
	
	public NodeListHelper(List<Object> list) {
		list_ = list;
	}

	@Override
	public Node item(int index) {
		if(list_ == null || index < 0 || index >= list_.size()) {
			return null;
		}
		
		Object o = list_.get(index);
		
		return (Node)o;
	}

	@Override
	public int getLength() {
		// TODO Auto-generated method stub
		return 0;
	}

}
