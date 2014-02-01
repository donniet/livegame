package com.livegameengine.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.livegameengine.model.GameType;
import com.livegameengine.persist.PMF;

public class HomeServlet extends HttpServlet {
	private Log log = LogFactory.getLog(HomeServlet.class);
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		UserService userService = UserServiceFactory.getUserService();
		
		String thisURL = req.getRequestURI();
		
		boolean authenticated = userService.isUserLoggedIn();
		
		
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		Query q = pm.newQuery(GameType.class);
		q.setOrdering("typeName");
		List<GameType> types = (List<GameType>)q.execute();
				
		req.setAttribute("types", types);
		req.setAttribute("authenticated", authenticated);
		req.setAttribute("login_url", userService.createLoginURL(thisURL));
		req.setAttribute("logout_url", userService.createLogoutURL(thisURL));
		
		RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/home.jsp");
		try {
			dispatcher.forward(req, resp);
		}
		catch(ServletException e) {
			throw new IOException(e);
		}
	}
}
