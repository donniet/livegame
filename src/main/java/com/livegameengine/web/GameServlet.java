package com.livegameengine.web;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.livegameengine.config.Config;
import com.livegameengine.error.GameLoadException;
import com.livegameengine.model.ClientMessage;
import com.livegameengine.model.Game;
import com.livegameengine.model.GameState;
import com.livegameengine.model.GameType;
import com.livegameengine.model.GameURIResolver;
import com.livegameengine.model.GameUser;
import com.livegameengine.model.Player;
import com.livegameengine.model.Watcher;
import com.livegameengine.persist.PMF;
import com.livegameengine.util.AddListenersParser;
import com.livegameengine.util.Util;
import com.sun.org.apache.xerces.internal.dom.DocumentTypeImpl;
import com.sun.xml.internal.fastinfoset.stax.util.StAXParserWrapper;

@Controller
@RequestMapping("/game")
public class GameServlet implements ServletContextAware {
	Log log = LogFactory.getLog(GameServlet.class);
	@Autowired Config config;
	ServletContext cxt = null;
	
	@RequestMapping("/-/{gameId}")
	public void doGetGame(@PathVariable String gameId, HttpServletResponse resp) throws IOException, GameLoadException, XMLStreamException, TransformerException {
		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		GameState gs = g.getMostRecentState();
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		GameUser gu = GameUser.findOrCreateGameUserByUser(u);
		
		resp.setContentType("application/xhtml+xml");
		
		GameSource s = new GameSource(gs);					
		Transformer gametrans = new GameTransformer(gu);
		
		gametrans.setOutputProperty(GameTransformer.PROPERTY_SERVLET_CONTEXT_PATH, cxt.getRealPath(""));

		gametrans.transform(s, new StreamResult(resp.getOutputStream()));
	}
	
	@RequestMapping("/-/{gameId}/meta")
	public void doGetMeta(@PathVariable String gameId, HttpServletResponse resp) throws IOException, GameLoadException, XMLStreamException {
		resp.setContentType("text/xml");		
		OutputStream responseStream = resp.getOutputStream();	
		
		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
		XMLStreamWriter writer = null;
		
		writer = factory.createXMLStreamWriter(responseStream, config.getEncoding());
		writer.setDefaultNamespace(config.getGameEngineNamespace());
		
		
		writer.writeStartDocument();
		
		g.serializeToXml("game", writer);
		writer.writeEndDocument();
		writer.flush();
	}
	
	@RequestMapping("/-/{gameId}/data")
	public void doGetData(@PathVariable String gameId, HttpServletResponse resp) throws IOException, GameLoadException, TransformerException {
		resp.setContentType("text/xml");

		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		GameState gs = g.getMostRecentState();
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		GameUser gu = GameUser.findOrCreateGameUserByUser(u);
		
		GameSource s = new GameSource(gs);
		Transformer gametrans = new GameTransformer(gu);
		
		gametrans.setOutputProperty(GameTransformer.PROPERTY_OUTPUT_TYPE, "Data");
		gametrans.setOutputProperty(GameTransformer.PROPERTY_SERVLET_CONTEXT_PATH, cxt.getRealPath(""));
		
		gametrans.transform(s, new StreamResult(resp.getOutputStream()));
	}
	
	@RequestMapping("/-/{gameId}/raw_view")
	public void doGetRawView(@PathVariable String gameId, HttpServletResponse resp)  throws IOException, GameLoadException, TransformerException {
		resp.setContentType("text/xml");

		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		GameUser gu = GameUser.findOrCreateGameUserByUser(u);
		
		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		GameState gs = g.getMostRecentState();
		
		GameSource s = new GameSource(gs);
		Transformer gametrans = new GameTransformer(gu);
		
		gametrans.setOutputProperty(GameTransformer.PROPERTY_OUTPUT_TYPE, "Raw");
		gametrans.setOutputProperty(GameTransformer.PROPERTY_SERVLET_CONTEXT_PATH, cxt.getRealPath(""));

		gametrans.transform(s, new StreamResult(resp.getOutputStream()));
	}
	
	private void handleActionError(Game g, HttpServletResponse resp) throws IOException, TransformerException {
		Document doc1 = config.newXmlDocument();
		
		String errorMessage = "";
		if(!g.isError()) {
			// there was no official error, the event is simply not valid
			errorMessage = "Action not valid in the current game state";
		}
		else {
			errorMessage = g.getErrorMessage();
		}
		
		Node error = doc1.createElementNS(config.getGameEngineNamespace(), "error");
		error.appendChild(doc1.createTextNode(errorMessage));
		doc1.appendChild(error);
								
		resp.setContentType("application/xml");
		resp.setStatus(400);
		Transformer trans;
		trans = config.newTransformer();
		trans.transform(new DOMSource(doc1), new StreamResult(resp.getOutputStream()));
		
		g.clearError();
	}
	
	@RequestMapping(value="/-/{gameId}/join", method=RequestMethod.POST)
	public void doPostJoin(@PathVariable String gameId, HttpServletResponse resp)   throws IOException, GameLoadException, TransformerException {
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		
		boolean success = g.sendPlayerJoinRequest(u);
		
		if(!success) {
			handleActionError(g, resp);			
		}
		else {
			resp.setStatus(200);
		}
	}
	
	@RequestMapping(value="/-/{gameId}/start", method=RequestMethod.POST)
	public void doPostStart(@PathVariable String gameId, HttpServletResponse resp)   throws IOException, GameLoadException, TransformerException {
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		
		boolean success = g.sendStartGameRequest(u);
		
		if(!success) {
			handleActionError(g, resp);			
		}
		else {
			resp.setStatus(200);
		}
	}
	
	@RequestMapping(value="/-/{gameId}/event/{event}", method=RequestMethod.POST)
	public void doPostEvent(
			@PathVariable String gameId, 
			@PathVariable String event, 
			HttpServletRequest req, 
			HttpServletResponse resp)  
					throws IOException, GameLoadException, TransformerException {
		
		String eventName = "board." + event;
		Document doc = config.newXmlDocument();
		
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		GameUser gu = GameUser.findOrCreateGameUserByUser(u);
		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		
		
		Node data = doc.createElementNS(config.getGameEngineNamespace(), "data");
		
		Node player = doc.createElementNS(config.getGameEngineNamespace(), "player");
		player.appendChild(doc.createTextNode(gu.getHashedUserId()));
		
		data.appendChild(player);
		doc.appendChild(data);
		
		DOMResult dr = new DOMResult(data);
						
		if(req.getContentLength() > 0) {
			try {
				Transformer trans = config.newTransformer();
				
				trans.transform(new StreamSource(req.getInputStream()), dr);
			} catch (TransformerException e) {
				// ignore error
			}
		}
		
		if(!g.triggerEvent(eventName, data)) {
			handleActionError(g, resp);
		}
		else {
			resp.setStatus(200);
		}
	}
	
	@RequestMapping(value="/-/{gameId}/addListener", method=RequestMethod.POST)
	public void doPostAddListener(@PathVariable String gameId, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, GameLoadException, TransformerException {
		
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		GameUser gu = GameUser.findOrCreateGameUserByUser(u);
		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		
		
		/*
		Document d = config.newXmlDocument();
		Transformer t = null;
		try {
			t = config.newTransformer();
			t.transform(new StreamSource(req.getInputStream()), new DOMResult(d));
		} catch (TransformerConfigurationException e) {
			resp.setStatus(500);
			e.printStackTrace();
		} catch (TransformerException e) {
			// invalid xml input
			resp.setStatus(400);
			e.printStackTrace();
		}
		*/
		
		Set<String> listeners = parseAddListenersRequest(req);
		
		Watcher w = Watcher.findWatcherByGameAndGameUser(g, gu);
		
		if(w != null) {
			w.addListners(listeners);
		}		
	}
	
	@RequestMapping(value="/-/{gameId}/message", method=RequestMethod.GET)
	public void doGetMessage(@PathVariable String gameId, @RequestParam(value="since", required=true) String since, HttpServletResponse resp)
			throws IOException, GameLoadException, TransformerException {

		StreamResult responseResult = new StreamResult(resp.getOutputStream());
		
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		GameUser gu = GameUser.findOrCreateGameUserByUser(u);
		Game g = Game.findGameByKey(KeyFactory.stringToKey(gameId));
		
		
		Date s;
		try {
			s = config.getDateFormat().parse(since);
		} catch (ParseException e) {
			resp.setStatus(400);
			return;
		}
		
		//log.info(String.format("since: %s, parsed: %s", since, config.getDateFormat().format(s)));

		List<ClientMessage> messages = ClientMessage.findClientMessagesSince(g, s);
		
		if(messages == null || messages.size() == 0) {
			resp.setStatus(204);
			return;
		}
		
		ClientMessageSource so = new ClientMessageSource(messages);
		Transformer gametrans = new GameTransformer(gu);
		
		gametrans.setOutputProperty(GameTransformer.PROPERTY_OUTPUT_TYPE, "View");
		gametrans.setOutputProperty(GameTransformer.PROPERTY_SERVLET_CONTEXT_PATH, cxt.getRealPath(""));

		gametrans.transform(so, responseResult);
	}
	
	private Set<String> parseAddListenersRequest(HttpServletRequest req) throws IOException {
		AddListenersParser alp;
		try {
			alp = new AddListenersParser(req);
		} catch (XMLStreamException e) {
			throw new IOException("parser error.");
		}
		
		return alp.getEvents();
		
	}
	@Override
	public void setServletContext(ServletContext cxt) {
		this.cxt = cxt;		
	}
}
