package com.livegameengine.scxml.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.model.Action;
import org.apache.commons.scxml.model.ExternalContent;
import org.apache.commons.scxml.model.ModelException;

import com.livegameengine.config.Config;

public class CompleteGame extends Action implements ExternalContent {
	private List externalNodes = null;
	private String winner = null;
	private String winnerExpr = null;

	public CompleteGame() {
		externalNodes = new ArrayList();
	}
	
	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException {
		
		Map<String,Object> params = new HashMap<String,Object>();
	
		
		if(winnerExpr != null) {	
			Evaluator eval = scInstance.getEvaluator();
		
			Object winner = eval.eval(scInstance.getRootContext(), winnerExpr);
			
			params.put("winner", winner);
		}
		else if(this.winner != null) {
			params.put("winner", this.winner);
		}
		
		evtDispatcher.send(
				"game.completeGame.send", 
				Config.getInstance().getGameEventTarget(),
				Config.getInstance().getGameEventTargetType(), 
				"game.completeGame", 
				params, null, 0L, null, null);
	}

	@Override
	public List getExternalNodes() {
		return externalNodes;
	}
	public String getWinnerExpr() {
		return winnerExpr;
	}
	public void setWinnerExpr(String val) {
		winnerExpr = val;
	}
	public String getWinner() {
		return winner;
	}
	public void setWinner(String winner) {
		this.winner = winner;
	}

}
