package com.livegameengine.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;

import com.livegameengine.error.GameLoadException;
import com.livegameengine.model.Game;
import com.livegameengine.model.GameType;
import com.livegameengine.model.GameUser;
import com.livegameengine.persist.PMF;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;


@Controller
@RequestMapping("/type")
public class GameTypeServlet implements ServletContextAware {
	private Log log = LogFactory.getLog(GameTypeServlet.class);
	private ServletContext cxt = null;
	
	
	@RequestMapping("/-/{typeId}/")
	public ModelAndView doGet(@PathVariable String typeId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserService userService = UserServiceFactory.getUserService();

		boolean authenticated = userService.isUserLoggedIn();
		
		Key k = KeyFactory.stringToKey(typeId);
		GameType t = GameType.findByKey(k);
		
		ModelAndView ret = new ModelAndView("type");
		ret.addObject("gameType", t);
		
		return ret;
	}
	
	@RequestMapping(value="/create", method=RequestMethod.POST)
	public void doPostCreate(HttpServletResponse resp) throws IOException {
		UserService userService = UserServiceFactory.getUserService();
		GameType t = new GameType();
		
		GameUser gu = GameUser.findOrCreateGameUserByUser(userService.getCurrentUser());
		
		t.setCreator(gu);
		t.setDescription("Test");
		t.setTypeName("Test");
		t.setClientVersion("0.2");
						
		URL u = GameServlet.class.getResource("/tictac5.xml");
		URLConnection conn = u.openConnection();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream is = conn.getInputStream();
		byte[] buffer = new byte[0x1000];
		
		int count = 0;
		while((count = is.read(buffer)) > 0) {
			bos.write(buffer, 0, count);
		}
		
		t.setStateChart(bos.toByteArray());
		
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		pm.makePersistent(t);
		
		resp.sendRedirect("-/" + KeyFactory.keyToString(t.getKey()) + "/");
	}
	
	@RequestMapping(value="/-/{typeId}/create", method=RequestMethod.POST) 
	public void doPostCreateGame(@PathVariable String typeId, HttpServletResponse resp) throws IOException, GameLoadException {
		Key k = KeyFactory.stringToKey(typeId);
		GameType t = GameType.findByKey(k);
		
		UserService userService = UserServiceFactory.getUserService();
		Game h = new Game(t);
		
		h.setCreated(new Date());
		h.setOwner(userService.getCurrentUser());				
		h.addWatcher(userService.getCurrentUser());
		
		PersistenceManager pm = JDOHelper.getPersistenceManager(h);
							
		pm.makePersistent(h);
		
		resp.sendRedirect(String.format("/game/-/%s/", KeyFactory.keyToString(h.getKey())));
	}
	
	@Override
	public void setServletContext(ServletContext cxt) {
		this.cxt = cxt;
	}
}
