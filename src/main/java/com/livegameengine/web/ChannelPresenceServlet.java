package com.livegameengine.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.ObjectState;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.livegameengine.model.ClientMessage;
import com.livegameengine.model.Game;
import com.livegameengine.model.GameUser;
import com.livegameengine.model.Player;
import com.livegameengine.model.Watcher;
import com.livegameengine.persist.PMF;
import com.livegameengine.util.Util;


@Controller
public class ChannelPresenceServlet extends HttpServlet {
	
	@Autowired private PMF pmf;
	@Autowired private Util util;
	
	public Watcher findWatcherByChannelKey(final String channelkeyIn) {
		PersistenceManager pm = pmf.getFactory().getPersistenceManager();
		
		try {
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
		finally {
			pm.close();
		}
	}
	
	@RequestMapping(value="/_ah/channel/*",method=RequestMethod.POST)	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// In the handler for _ah/channel/connected/
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		ChannelPresence presence = channelService.parsePresence(req);
		
		PersistenceManager pm = null;
		
		String channelKey = presence.clientId();
		
		Watcher w = findWatcherByChannelKey(channelKey);

		if(w == null) return;
		
		GameUser gu = w.getGameUser();
		
		if(gu.isConnected() != presence.isConnected()) {
			gu.setConnected(presence.isConnected());
	
			pm = JDOHelper.getPersistenceManager(gu);
			pm.makePersistent(gu);
			
			Game g = Game.findUninitializedGameByKey(w.getGameKey());
			
			boolean playerConnectedChange = false;
			
			List<Player> players = g.getPlayers();
			for(Iterator<Player> i = players.iterator(); i.hasNext();) {
				Player p = i.next();
				
				if(p.getGameUserKey().equals(gu.getKey())) {
					playerConnectedChange = true;
					break;
				}
			}
			
			if(playerConnectedChange) {
				
				Map<String,String> params = new HashMap<String,String>();
				params.put("player", gu.getHashedUserId());
				params.put("connected", Boolean.toString(presence.isConnected()));
				

				ClientMessage m = new ClientMessage(util, g, "game.playerConnectionChange", params);
				pm = pmf.getFactory().getPersistenceManager();
				
				try {
					pm.makePersistent(m);
				}
				finally {
					pm.close();
				}
				
				
				//g.sendWatcherMessage("game.playerConnectionChange", params, null);
			}
		}
		
		if(!presence.isConnected()) {
			pm = JDOHelper.getPersistenceManager(w);
			
			pm.deletePersistent(w);
		}
	}
}
