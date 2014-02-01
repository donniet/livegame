package com.livegameengine.scxml.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.model.Action;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.Send;

import com.livegameengine.config.Config;

public class PlayerJoin  extends Action {
	private String role_ = null;
	private String roleExpr_ = null;
	private String playerExpr_ = null;

	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter arg1,
			SCInstance scInstance, Log arg3, Collection arg4) throws ModelException,
			SCXMLExpressionException {
		String playerId = "";
		Evaluator eval = scInstance.getEvaluator();
		
		if(playerExpr_ != null) {
			playerId = eval.eval(scInstance.getRootContext(), playerExpr_).toString();
		}
		
		if(roleExpr_ != null) {
			role_ = eval.eval(scInstance.getRootContext(), roleExpr_).toString();
		}
		
		Map<String,String> params = new HashMap<String,String>();
		
		params.put("playerid", playerId);
		params.put("role", role_);
		
		evtDispatcher.send(
				"game.playerJoin.send", 
				Config.getInstance().getGameEventTarget(),
				Config.getInstance().getGameEventTargetType(), 
				"game.playerJoin", 
				params, null, 0L, null, null);
	}
	
	public String getRole() {
		return role_;
	}
	public void setRole(String role) {
		role_ = role;
	}
	public String getRoleExpr() {
		return roleExpr_;
	}
	public void setRoleExpr(String roleExpr) {
		roleExpr_ = roleExpr;
	}
	public String getPlayerExpr() {
		return playerExpr_;
	}
	public void setPlayerExpr(String value) {
		playerExpr_ = value;
	}

}
