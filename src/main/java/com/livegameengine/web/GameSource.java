package com.livegameengine.web;

import javax.xml.transform.Source;

import com.livegameengine.model.Game;
import com.livegameengine.model.GameState;

public class GameSource implements Source {
	private Game game_ = null;
	private GameState gameState_ = null;
	
	private String systemId_ = "GameSource"; 
	
	public GameSource(Game g) {
		game_ = g;
		gameState_ = g.getMostRecentState();
	}
	
	public GameSource(GameState gs) {
		gameState_ = gs;
		game_ = Game.findUninitializedGameByKey(gs.getGameKey());
	}

	@Override
	public void setSystemId(String systemId) {
		systemId_ = String.format("GameSource '%s'", systemId);
	}

	@Override
	public String getSystemId() {
		return systemId_;
	}
	
	public Game getGame() {
		return game_;
	}
	
	public GameState getGameState() {
		return gameState_;
	}

}
