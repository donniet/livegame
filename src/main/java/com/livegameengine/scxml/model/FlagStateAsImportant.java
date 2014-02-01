package com.livegameengine.scxml.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.model.Action;
import org.apache.commons.scxml.model.ModelException;

import com.livegameengine.config.Config;

public class FlagStateAsImportant extends Action {
	private boolean important_ = false;
	
	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException {
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("important", Boolean.toString(important_));
		
		evtDispatcher.send(
				"game.flagStateAsImportant.send", 
				Config.getInstance().getGameEventTarget(),
				Config.getInstance().getGameEventTargetType(), 
				"game.flagStateAsImportant", 
				params, 
				null, 0L, null, null);
	}
	
	public void setImportant(boolean important) {
		important_ = important;
	}
	public boolean getImportant() {
		return important_;
	}
	
}
