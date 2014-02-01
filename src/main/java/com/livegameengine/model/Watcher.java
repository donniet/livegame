package com.livegameengine.model;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Persistent;

import org.apache.commons.codec.binary.Base64;
import com.livegameengine.config.Config;
import org.mozilla.javascript.ScriptableObject;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.livegameengine.persist.PMF;

@PersistenceCapable 
public class Watcher extends ScriptableObject { 
	private static final long serialVersionUID = 1L;

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String channelkey;
	
	@Persistent
	private Key gameKey;
	
	@Persistent
	private Key gameUserKey;
	
	@Persistent
	private Set<String> listeningFor;
		
	public Watcher() {}
	
	public Watcher(Game gameIn, GameUser gameUserIn) {
		gameKey = gameIn.getKey();
		gameUserKey = gameUserIn.getKey();
		listeningFor = new HashSet<String>();
		
		channelkey = hashGameAndGameUser(gameIn, gameUserIn);
	}
	
	public static Watcher findWatcherByChannelKey(final String channelkeyIn) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		Query q = pm.newQuery(Watcher.class);
		q.setFilter("channelkey == channelkeyIn");
		q.declareParameters(String.class.getName() + " channelkeyIn");
		List<Watcher> results = (List<Watcher>)q.execute(channelkeyIn);
		
		if(results.size() > 0) {
			return results.get(0);
		}
		else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<Watcher> findWatchersByGame(final Game gameIn) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		Query q = pm.newQuery(Watcher.class);
		q.setFilter("gameKey == gameKeyIn");
		q.declareParameters(Key.class.getName() + " gameKeyIn");
		List<Watcher> results = (List<Watcher>)q.execute(gameIn.getKey());
		
		return results;
	}
	
	public static Watcher findWatcherByGameAndGameUser(final Game gameIn, final GameUser gameUserIn) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		Query q = pm.newQuery(Watcher.class);
		q.setFilter("gameKey == gameKeyIn && gameUserKey == gameUserKeyIn");
		q.declareParameters(Key.class.getName() + " gameKeyIn, " + Key.class.getName() + " gameUserKeyIn");
		List<Watcher> results = (List<Watcher>)q.execute(gameIn.getKey(), gameUserIn.getKey());
		
		if(results.size() > 0) {
			return results.get(0);
		}
		else {
			return null;
		}
	}
	
	public static String hashGameAndGameUser(final Game gameIn, final GameUser gameUserIn) {
		Config config = Config.getInstance();
		
		String ret = "";
		
		try {
			MessageDigest digest = MessageDigest.getInstance(config.getDigestAlgorithm());
			digest.reset();
			// salt with the class name
			digest.update(Watcher.class.getName().getBytes(config.getEncoding()));
			digest.update(KeyFactory.keyToString(gameIn.getKey()).getBytes(config.getEncoding()));
			digest.update(KeyFactory.keyToString(gameUserIn.getKey()).getBytes(config.getEncoding()));
			byte[] out = digest.digest();
			ret = Base64.encodeBase64URLSafeString(out);
		}
		catch(UnsupportedEncodingException e) {
			//TODO: handle exception
			e.printStackTrace();
		} 
		catch (NoSuchAlgorithmException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public void addListners(Set<String> events) {
		listeningFor.addAll(events);
	}
	
	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}
	
	public Key getGameUserKey() {
		return gameUserKey;
	}

	public GameUser getGameUser() {
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		
		return pm.getObjectById(GameUser.class, this.gameUserKey);
	}
	
	public void setGameUser(GameUser gu) {
		gameUserKey = gu.getKey();
	}
	
	public Key getGameKey() {
		return gameKey;
	}

	public void setGame(Game game) {
		this.gameKey = game.getKey();
	}
	/*
	public void makePersistent() {
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		
		if(pm == null) 
			pm = PMF.getInstance().getPersistenceManager();
		
		pm.makePersistent(this);
	}
	*/

	@Override
	public String getClassName() {
		return "Watcher";
	}

	public String getChannelkey() {
		return channelkey;
	}

}
