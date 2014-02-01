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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import com.livegameengine.error.GameLoadException;
import com.livegameengine.model.Game;

import com.livegameengine.model.GameType;
import com.livegameengine.model.GameUser;
import com.livegameengine.persist.PMF;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class GameTypeServlet extends HttpServlet {
	private Log log = LogFactory.getLog(GameTypeServlet.class);
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserService userService = UserServiceFactory.getUserService();

		boolean authenticated = userService.isUserLoggedIn();
		
		Pattern p = Pattern.compile("/t/([^/]+)/");
		
		final Matcher m = p.matcher(req.getPathInfo());
		
		if(m != null && m.matches()) {
			Key k = KeyFactory.stringToKey(m.group(1));
			GameType t = GameType.findByKey(k);
						
			RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/type.jsp");
			
			req.setAttribute("gameType", t);
			
			try {
				dispatcher.forward(req, resp);
			}
			catch(ServletException e) {
				throw new IOException(e);
			}
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("pathinfo: " + req.getPathInfo());
		
		UserService userService = UserServiceFactory.getUserService();
		
		boolean authenticated = userService.isUserLoggedIn();
		
		
		if(!authenticated) {
			resp.sendRedirect(userService.createLoginURL("/"));
			return;
		}		

		Pattern p = Pattern.compile("/-/([^/]+)/([^/]+)");
		
		
		final Matcher m = p.matcher(req.getPathInfo());
		
		if(req.getPathInfo().equals("/create")) {			
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
		else if(m != null && m.matches()) {
			Key k = KeyFactory.stringToKey(m.group(1));
			GameType t = GameType.findByKey(k);
			
			
			if(t == null) {
				resp.sendError(404);
				resp.getOutputStream().write(("game type not found: " + m.group(1)).getBytes());
				return;
			}
			
			if(m.group(2).equals("create")) {												
				Game h = null;
				try {
					h = new Game(t);
				} catch (GameLoadException e) {
					resp.setStatus(500);
					resp.setContentType("text/plain");
					resp.getWriter().println("Could not load game type: " + m.group(1));
					return;
				}
				
				h.setCreated(new Date());
				h.setOwner(userService.getCurrentUser());				
				h.addWatcher(userService.getCurrentUser());
				
				PersistenceManager pm = JDOHelper.getPersistenceManager(h);
									
				pm.makePersistent(h);
				
				resp.sendRedirect(String.format("/game/-/%s/", KeyFactory.keyToString(h.getKey())));
			}
		}
		else {
			resp.setStatus(404);
			resp.getOutputStream().write(("URL Not found: " + req.getPathInfo()).getBytes());
		}
	}
}
