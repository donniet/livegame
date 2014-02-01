package com.livegameengine.scxml.model;

import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.model.Action;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.Send;

import com.livegameengine.config.Config;

public class StartGame  extends Action {

	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException  {

		evtDispatcher.send(
			"game.startGame.send", 
			Config.getInstance().getGameEventTarget(),
			Config.getInstance().getGameEventTargetType(), 
			"game.startGame", 
			new HashMap<String,String>(), null, 0L, null, null);
		
	}

}
