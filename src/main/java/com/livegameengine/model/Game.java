package com.livegameengine.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.SCXMLListener;
import org.apache.commons.scxml.Status;
import org.apache.commons.scxml.TriggerEvent;
import org.apache.commons.scxml.io.SCXMLParser;
import org.apache.commons.scxml.model.CustomAction;
import org.apache.commons.scxml.model.Datamodel;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.SCXML;
import org.apache.commons.scxml.model.State;
import org.apache.commons.scxml.model.Transition;
import org.apache.commons.scxml.model.TransitionTarget;
import com.livegameengine.config.Config;
import com.livegameengine.error.GameLoadException;
import com.livegameengine.persist.PMF;
import com.livegameengine.scxml.js.JsContext;
import com.livegameengine.scxml.js.JsEvaluator;
import com.livegameengine.scxml.js.JsFunctionJsonTransformer;
import com.livegameengine.scxml.model.CompleteGame;
import com.livegameengine.scxml.model.Error;
import com.livegameengine.scxml.model.FlagStateAsImportant;
import com.livegameengine.scxml.model.PlayerJoin;
import com.livegameengine.scxml.model.SendInternalEvent;
import com.livegameengine.scxml.model.SendWatcherEvent;
import com.livegameengine.scxml.model.StartGame;
import com.livegameengine.scxml.semantics.SCXMLGameSemanticsImpl;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.xml.XMLLib;
import org.mozilla.javascript.xml.XMLObject;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.users.User;
import com.google.appengine.api.utils.SystemProperty;

import flexjson.JSONSerializer;
import flexjson.transformer.StringTransformer;

import javax.jdo.JDOHelper;
import javax.jdo.ObjectState;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Extension;
import javax.xml.bind.annotation.XmlList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


@PersistenceCapable(detachable = "true") 
//@FetchGroup(name = "GameGroup", members = { @Persistent(name="players") })
public class Game implements Scriptable, EventDispatcher, SCXMLListener, XmlSerializable, ErrorReporter {
	@NotPersistent
	private Scriptable parent_, prototype_;
		
	@NotPersistent
	private FindPlayerByRoleFunction findPlayerByRoleFunction_ = null;
		
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private Key owner;
	
	@Persistent
	private Date created;
	
	@Persistent
	private Date started;
	
	@Persistent
	private Date completed;
		
	@Persistent
	private Key gameTypeKey;
	
	@Persistent
	@Element(dependent = "true", mappedBy = "game", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="playerJoin asc"))	
	private List<Player> players;
	
	@Persistent
	@Element(dependent = "true", mappedBy = "game", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="eventDate asc"))
	private List<GameHistoryEvent> events;
	
	@Persistent 
	private Key currentStateKey;
		
	@NotPersistent
	transient private boolean isDirty_ = false;
	
	@NotPersistent
	transient private boolean isError_ = false;
	
	@NotPersistent
	transient private boolean isImportant_ = false;
	
	@NotPersistent
	transient private String errorMessage_ = "";
		
	//not persistent
	@NotPersistent
	transient private Map<String,String> params;
	
	@NotPersistent
	transient private SCXML scxml;
	
	@NotPersistent
	transient private List<Exception> loadWarnings;
	
	@NotPersistent
	private	transient SCXMLExecutor exec;
	
	@NotPersistent
	transient private JsEvaluator eval;
	
	@NotPersistent
	transient private JsContext cxt;
	
	@NotPersistent private Log log_;
	
	@NotPersistent transient private GameUser currentUser_ = null;
	
	//public Game() {}
	
	public Game(GameType gt) throws GameLoadException {
		setGameType(gt);
		setCreated(new Date());
		init();
	}
	
	private Log getLog() {
		if(log_ == null) {
			log_ = LogFactory.getLog(Game.class);
		}

		return log_;
	}
	
	private void init() throws GameLoadException {
		//GameType gt = getGameType();
		//InputStream bis = new ByteArrayInputStream(gt.getStateChart());
		InputStream bis = Game.class.getResourceAsStream("/pilgrims2.xml");
		
		loadWarnings = new ArrayList<Exception>();
		
		List<CustomAction> customActions = new ArrayList<CustomAction>();
		customActions.add(new CustomAction(Config.getInstance().getGameEngineNamespace(), "error", Error.class));
		customActions.add(new CustomAction(Config.getInstance().getGameEngineNamespace(), "success", Error.class));
		customActions.add(new CustomAction(Config.getInstance().getGameEngineNamespace(), "playerJoin", PlayerJoin.class));
		customActions.add(new CustomAction(Config.getInstance().getGameEngineNamespace(), "flagStateAsImportant", FlagStateAsImportant.class));
		customActions.add(new CustomAction(Config.getInstance().getGameEngineNamespace(), "sendWatcherEvent", SendWatcherEvent.class));
		customActions.add(new CustomAction(Config.getInstance().getGameEngineNamespace(), "completeGame", CompleteGame.class));
		customActions.add(new CustomAction(Config.getInstance().getGameEngineNamespace(), "startGame", StartGame.class));
		customActions.add(new CustomAction(Config.getInstance().getGameEngineNamespace(), "sendInternalEvent", SendInternalEvent.class));
				
		scxml = null;
		try {
			scxml = SCXMLParser.parse(new InputSource(bis), new ErrorHandler() {
				@Override
				public void warning(SAXParseException exception) throws SAXException {
					loadWarnings.add(exception);
				}
				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;				
				}
				@Override
				public void error(SAXParseException exception) throws SAXException {
					throw exception;							
				}
			}, customActions);
		} catch (SAXException e) {
			throw new GameLoadException("Fatal parse error.", e);
		} catch (ModelException e) {
			throw new GameLoadException("Fatal parse error.", e);
		} catch (IOException e) {
			throw new GameLoadException("Fatal parse error.", e);
		}

		eval = new JsEvaluator();
		
		exec = new SCXMLExecutor(eval, null, this, new SCXMLGameSemanticsImpl());
		exec.addListener(scxml, this);
		exec.setEventdispatcher(this);
		exec.setStateMachine(scxml);
		
		cxt = (JsContext)exec.getRootContext();
		cxt.set("game", this);		
		
		try {
			GameState gs = null;
			
			gs = getMostRecentState();		
		
			if(gs != null) {				
				gs.injectInto(cxt);
				
				Set s = exec.getCurrentStatus().getStates();
				Map ms = exec.getStateMachine().getTargets();
				s.clear();
				
				Set<String> ss = gs.getStateSet();
				for(String state : ss) {
					s.add(ms.get(state));
				}				
			}
			
			if(gs == null) {
				exec.go();
				
				persistGameState(true);
			}
		}
		catch(ModelException e) {
			throw new GameLoadException("Could not start the machine.", e);
		}
		isDirty_ = false;
		
	}
	
	@Override
	public void onError(String errCode, String errDetail, Object errCtx) {
		// TODO really handle errors here.
		getLog().error(errCode + ": " + errDetail);
		isError_ = true;
		errorMessage_ = "There was an error executing the game rules.";
	}
	/*
	public boolean getPersisted() {
		ObjectState os = JDOHelper.getObjectState(this);
		
		return os == ObjectState.PERSISTENT_CLEAN || os == ObjectState.PERSISTENT_NEW || os == ObjectState.HOLLOW_PERSISTENT_NONTRANSACTIONAL;
	}
	*/
	
	public GameState persistGameState(boolean isImportant) {
		//GameState.deleteOldStatesForGame(this);
		PersistenceManager pmf = JDOHelper.getPersistenceManager(this);
		if(pmf == null) {
			pmf = PMF.getInstance().getPersistenceManager();
		}
		
		if(this.getKey() == null) {
			pmf.makePersistent(this);
		}
		
		final GameState gs = new GameState(this);
		
		Set<State> s = exec.getCurrentStatus().getStates();
		
		for(State state : s) {
			gs.getStateSet().add(state.getId());
			
			List<Transition> trans = state.getTransitionsList();
			
			for(Transition t : trans) {
				gs.getValidEvents().add(t.getEvent());
			}
		}
		
		
		gs.setStateDate(new Date());
		gs.extractFrom(scxml.getDatamodel(), cxt);
		gs.setImportant(isImportant_ || isImportant);
		
		Node validEvents = gs.getValidEventsNode();
		
		ClientMessage m = new ClientMessage(this, "game.validEventsChanged", new HashMap<String,String>(), validEvents);
		
		pmf.makePersistent(m);	
		GameState saved = pmf.makePersistent(gs);
		
		this.setCurrentStateKey(saved.getKey());
		pmf.makePersistent(this);
		
		isDirty_ = false;
		isError_ = false;
		isImportant_ = false;
		
		return gs;
	}	
	
	private void setCurrentStateKey(Key k) {
		currentStateKey = k;
	}


	public ClientMessage getMostRecentClientMessage() {
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		
		Query q = pm.newQuery(ClientMessage.class);
		q.setFilter("gameKey == gameKeyIn");
		q.setOrdering("messageDate desc");
		q.declareParameters(Key.class.getName() + " gameKeyIn");
		q.setRange(0,1);
		
		List<ClientMessage> res = (List<ClientMessage>)q.execute(this.getKey());
		
		if(res.size() > 0) {
			return res.get(0);
		}
		
		return null;
	}
	
	public GameState getMostRecentState() {
		GameState ret = null;
		
		if(currentStateKey == null) return null;
		
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		if(pm == null) {
			pm = PMF.getInstance().getPersistenceManager();
		}
		
		ret = pm.getObjectById(GameState.class, currentStateKey);
		
		return ret;
	}

	@Override
	public void cancel(String sendId) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void send(String sendId, String target, String targetType,
			String event, Map params, Object hints, long delay,
			Object content, List externalNodes) {
		
		getLog().info(String.format("Send Event '%s'", event));
				
		if(	targetType.equals(Config.getInstance().getGameEventTargetType())) {
			if(target.equals(Config.getInstance().getGameEventTarget())) {
				if(event.equals("game.playerJoin")) {
					String playerid = null;
					GameUser gameUser = null;
					
					if(params.get("playerid") != null && params.get("playerid") != "") {
						playerid = params.get("playerid").toString();
					}
					else if(currentUser_ != null) {
						playerid = currentUser_.getHashedUserId();
						params.put("playerid", playerid);
					}
					
					String role = params.get("role").toString();
					
					if(playerid != null) {
						gameUser = GameUser.findByHashedUserId(playerid);
					}
					
					if(gameUser != null) {
						Player p = addPlayer(gameUser, role);
						
						NodeList nl = null;
						try {
							nl = Config.getInstance().serializeToNodeList("player", p);
						} catch (XMLStreamException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						if(nl != null && nl.getLength() > 0) {
							content = nl.item(0);
						}
						isError_ = false;
					}
					else {
						isError_ = true;
						errorMessage_ = "invalid player id, likely a problem with the scxml game rules";
					}
				}
				else if(event.equals("game.startGame")) {
					isDirty_ = true;
					started = new Date();
				}
				else if(event.equals("game.completeGame")) {
					if(params.containsKey("winner")) {
						Object o = params.get("winner");
						
						if(Player.class.isAssignableFrom(o.getClass())) {
							Player p = (Player)o;
							
							p.setWinner(true);
							
							// we can only send strings to the client
							params.put("winner", p.getRole());
							params.put("winning_player", p.getGameUser().getHashedUserId());
						}
						// could be a role, or a game user id...
						else if(String.class.isAssignableFrom(o.getClass())) {
							String playerid = (String)o;
							boolean found = false;
							for(Iterator<Player> i = getPlayers().iterator(); !found && i.hasNext();) {
								Player p = i.next();
								
								if(playerid.equals(p.getGameUser().getHashedUserId()) || playerid.equals(p.getRole())) {
									p.setWinner(true);
									params.put("winner", p.getRole());
									params.put("winning_player", p.getGameUser().getHashedUserId());
									found = true;
								}
							}
							
							if(!found) {
								isError_ = true;
								errorMessage_ = "invalid winner parameter.";
							}
						}
						else {
							isError_ = true;
							errorMessage_ = "invalid winner parameter.";
						}
					}
					
					if(!isError_) { 
						setCompleted(new Date());
					}
				}
				else if(event.equals("game.error")) {
					errorMessage_ = params.get("message").toString();
					getLog().error("[error]: " + params.get("message"));
					isError_ = true;
				}
				else if(event.equals("game.success")) {
					isError_ = false;
				}
				else if(event.equals("game.flagStateAsImportant")) {
					isImportant_ = Boolean.parseBoolean(params.get("important").toString());
				}
				else {
					isError_ = true;
					errorMessage_ = String.format("unknown game event: '%s'", event);
				}
			}
			else if(target.equals(Config.getInstance().getWatcherEventTarget())) {
				
				
				isError_ = false;
			}
		}
		
		if(!isError_) {
			//persistGameState();
			ClientMessage m = null;
			
			if(content == null) {
				m = new ClientMessage(this, event, params);
			}
			else if(String.class.isAssignableFrom(content.getClass())) {
				m = new ClientMessage(this, event, params, (String)content);
			}
			else if(Node.class.isAssignableFrom(content.getClass())) {
				m = new ClientMessage(this, event, params, (Node)content);
			}
			else if(Scriptable.class.isAssignableFrom(content.getClass())){
				Node c = null;
				try {
					c = XMLLibImpl.toDomNode(content);
				}
				catch(Exception e) {
					log_.info("content error: " + e.getMessage());
				}
				
				if(c != null) 
					m = new ClientMessage(this, event, params, c);
				else
					m = new ClientMessage(this, event, params, content.toString());
			}
			else {
				m = new ClientMessage(this, event, params, content.toString());
			}
			
			PersistenceManager pm = PMF.getInstance().getPersistenceManager();
			
			pm.makePersistent(m);
			
			//sendWatcherMessage(event, params, content);
		}
	}

	public boolean sendStartGameRequest(User user) {
		isError_ = false;
		
		GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		
		if(gu.getKey().compareTo(owner) != 0) {
			return false;
		}
		
		Document doc = Config.getInstance().newXmlDocument();
		
		Node player = doc.createElementNS(Config.getInstance().getGameEngineNamespace(), "player");
		player.appendChild(doc.createTextNode(gu.getHashedUserId()));
		doc.appendChild(player);
				
		try {
			getExec().triggerEvent(new TriggerEvent("game.startGame", TriggerEvent.SIGNAL_EVENT, doc));
		} catch (ModelException e) {
			e.printStackTrace();
			return false;
		}
		
		if(isDirty_ && !isError_) {
			started = new Date();
			
			persistGameState(true);
			isDirty_ = false;
			isError_ = false;
			return true;
		}
		else {
			isDirty_ = false;
			return false;
		}
	}

	public boolean sendPlayerJoinRequest(User user) {
		GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		
		return sendPlayerJoinRequest(gu);
	}
	
	public boolean sendPlayerJoinRequest(GameUser gu) {
		isError_ = false;
		
		Document doc = Config.getInstance().newXmlDocument();
		
		Node player = doc.createElementNS(Config.getInstance().getGameEngineNamespace(), "player");
		player.appendChild(doc.createTextNode(gu.getHashedUserId()));
		doc.appendChild(player);
		
		currentUser_ = gu;
		
		try {
			getExec().triggerEvent(new TriggerEvent("game.playerJoin", TriggerEvent.SIGNAL_EVENT, doc));
		} catch (ModelException e) {
			//TODO: exception handling...
			e.printStackTrace();
			return false;
		}
		
		currentUser_ = null;
		
		if(isDirty_ && !isError_) {
			
			persistGameState(true);
			return true;
		}
		else {
			isDirty_ = false;
			return false;
		}
	}
	public boolean triggerEvent(String eventid, Node node) {
		boolean ret = true;
		
		isError_ = false;
		
		try {
			getExec().triggerEvent(new TriggerEvent(eventid, TriggerEvent.SIGNAL_EVENT, node));
		} catch (ModelException e) {
			if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
				e.printStackTrace();
			return false;
		}
		
		if(isDirty_ && !isError_) {
			persistGameState(false);
		}
		else if(isError_) {
			ret = false;
		}
		else if(!isDirty_) {
			ret = false;
			errorMessage_ = String.format("Event '%s' was not valid in the current state of the game.", eventid);
		}
		
		return ret;
	}
	
	
	public boolean isError() {
		return isError_;
	}
	public String getErrorMessage() {
		if(errorMessage_ == null) return "";
		else return errorMessage_;
	}
		
	public void sendWatcherMessage(String event, Map params, Object content) {
		Document doc = Config.getInstance().newXmlDocument();
		String ns = Config.getInstance().getGameEngineNamespace();
		
		Node eventNode = doc.createElementNS(ns, "event");
		Node eventName = doc.createAttributeNS(ns, "name");
		eventName.setNodeValue(event);
		eventNode.getAttributes().setNamedItemNS(eventName);
		
		Set paramsKeys = params.keySet();
		for(Iterator i = paramsKeys.iterator(); i.hasNext(); ) {
			String paramName = (String)i.next();
			
			Node paramNode = doc.createElementNS(ns, "param");
			Node paramNameNode = doc.createAttributeNS(ns, "name");
			paramNameNode.setNodeValue(paramName);
			paramNode.getAttributes().setNamedItemNS(paramNameNode);
			
			paramNode.setTextContent(params.get(paramName).toString());
			
			eventNode.appendChild(paramNode);
		}
		
		if(content != null) {
			Node contentNode = doc.createElementNS(ns, "content");
			Node contentBody = null;
						
			if(Node.class.isAssignableFrom(content.getClass())) {
				contentBody = (Node)content;
			}
			else {
				try {
					contentBody = XMLLibImpl.toDomNode(content);
				}
				catch(IllegalArgumentException e) {
					contentBody = null;
				}
			}
			
			if(contentBody != null) {
				Node imported = doc.importNode(contentBody, true);
				contentNode.appendChild(imported);
			}
			else {
				contentNode.setTextContent(content.toString());
			}
			eventNode.appendChild(contentNode);
		}
		
		doc.appendChild(eventNode);
				
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		
		List<Watcher> watchers = getWatchers();
		for(Watcher w : watchers) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			
			try {
				Config.getInstance().transformAsDatamodel(new DOMSource(doc), new StreamResult(bos), w.getGameUser().getHashedUserId());
				String strmessage = bos.toString(Config.getInstance().getEncoding());
							
				channelService.sendMessage(new ChannelMessage(w.getChannelkey(), strmessage));
				
			} catch (TransformerConfigurationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TransformerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
		}
	}
	/*
	
	public String[] getTransitionEvents() {
		Set<String> ret = new HashSet<String>();
		
		Set<State> s = exec.getCurrentStatus().getStates();
		for(State state : s) {
			List transitions = state.getTransitionsList();
			for(Object o : transitions) {
				Transition t = (Transition)o;
				String event = t.getEvent();
				if(event != null && !event.equals("")) {
					ret.add(event);
				}
			}
		}
		
		return ret.toArray(new String[0]);
	}
	*/
	
	public static Game findUninitializedGameByKey(final Key key) {
		Game ret = null;
		
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		ret = pm.getObjectById(Game.class, key);
		
		return ret;
	}
	
	public static Game findGameByKey(Key key) throws GameLoadException {
		
		Game ret = findUninitializedGameByKey(key);
		
		if(ret != null) {
			ret.init();
		}

		return ret;
	}
	
	protected SCXMLExecutor getExec() {
		return exec;
	}
	
	public Set<State> getCurrentStates() {
		return getExec().getCurrentStatus().getStates();
	}
	
	public GameUser getOwner() {
		GameUser ret = null;
		
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		
		if(pm == null) {
			pm = PMF.getInstance().getPersistenceManager();
		}
		
		ret = pm.getObjectById(GameUser.class, this.owner);
		
		return ret;
	}
	public void setOwner(User u) {
		GameUser gu = GameUser.findOrCreateGameUserByUser(u);
		owner = gu.getKey();
	}
	
	public List<GameHistoryEvent> getEvents() {
		return events;
	}
	public Map<String,String> getParameters() {
		return params;
	}
	
	public Date getCurrentTime() {
		return new Date();
	}
	public void addEvent(Date eventDate) {
		GameHistoryEvent e = new GameHistoryEvent(this);
		e.setEventDate(eventDate);
		events.add(e);
	}
	public List<Player> getPlayers() {
		if(players == null) {
			players = new ArrayList<Player>();
		}
		return players;
	}
	private Player addPlayer(User user, String role) {
		GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		return addPlayer(gu, role);		
	}
	private Player addPlayer(GameUser gameUser, String role) {
		Player p = new Player(this, gameUser, role);
		getPlayers().add(p);
		
		return p;
	}
	public List<Watcher> getWatchers() {
		return Watcher.findWatchersByGame(this);
	}
	public Watcher addWatcher(User user) {
		final GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		final Game game = this;
		
		Watcher ret = Watcher.findWatcherByGameAndGameUser(this, gu);
		
		if(ret == null) {
			PersistenceManager pm = JDOHelper.getPersistenceManager(this);
			
			if(pm == null) {
				pm = PMF.getInstance().getPersistenceManager();
			}
			
			ret = new Watcher(game, gu);
			pm.makePersistent(ret);
			
		}
		return ret;
	}
	public boolean removeWatcher(User user) {
		final GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		final Game game = this;
				
		Watcher w = Watcher.findWatcherByGameAndGameUser(this,  gu);
		
		if(w != null) {
			PersistenceManager pm = JDOHelper.getPersistenceManager(w);
			
			pm.deletePersistent(w);
			return true;
		}
		else {
			return false;
		}
	}
	public void setGameType(GameType gt) {
		if(gt != null) this.gameTypeKey = gt.getKey();
		else this.gameTypeKey = null;
	}
	public GameType getGameType() {
		if(this.gameTypeKey == null) 
			return null;
		else 
			return GameType.findByKey(this.gameTypeKey);
	}
	
	/* transition events */
	@Override
	public void onEntry(TransitionTarget state) {
		getLog().info("OnEntry: " + state.getId());			
	}

	@Override
	public void onExit(TransitionTarget state) {
		getLog().info("OnExit: " + state.getId());	
	}

	@Override
	public void onTransition(TransitionTarget from, TransitionTarget to, Transition transition) {
		getLog().info("OnTransition: " + from.getId() + " -> " + to.getId() + ": [" + transition.getEvent() + "]");
		
		isDirty_ = true;	
	}
	/* end transition events */

	public Key getKey() {
		return key;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}
	
	public Date getCompleted() {
		return completed;
	}
	public void setCompleted(Date completed) {
		this.completed = completed;
	}
	

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        return toString();
    }

	@Override
	public String getClassName() {
		return "Game";
	}

	@Override
	public void delete(String arg0) {
		// not implemented
	}

	@Override
	public void delete(int arg0) {
		// not implemented
	}

	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}
	

	@Override
	public void serializeToXml(String elementName, XMLStreamWriter writer) throws XMLStreamException {
		String ns = Config.getInstance().getGameEngineNamespace();
		writer.writeStartElement(ns, elementName);
		writer.writeNamespace("", Config.getInstance().getGameEngineNamespace());
		writer.writeAttribute("key", KeyFactory.keyToString(getKey()));
		writer.writeStartElement(ns, "players");
		for(Iterator<Player> i = getPlayers().iterator(); i.hasNext();) {
			Player p = i.next();
			p.serializeToXml("player", writer);
		}
		writer.writeEndElement();
		writer.writeStartElement(ns, "currentTime");
		writer.writeCharacters(Config.getInstance().getDateFormat().format(getCurrentTime()));
		writer.writeEndElement();
		writer.writeStartElement(ns, "created");
		writer.writeCharacters(Config.getInstance().getDateFormat().format(getCreated()));
		writer.writeEndElement();
		
		if(getCompleted() != null) {
			writer.writeStartElement(ns, "completed");
			writer.writeCharacters(Config.getInstance().getDateFormat().format(getCompleted()));
			writer.writeEndElement();
		}
		
		//TODO: evaluate if we really need this
		GameState gs = getMostRecentState();
		if(gs != null) {
			gs.serializeToXml("mostRecentState", writer);
		}
		
		ClientMessage cm = getMostRecentClientMessage();
		if(cm != null) {
			cm.serializeToXml("mostRecentClientMessage", writer);
		}
		
		getOwner().serializeToXml("owner", writer);
		
		GameType gt = getGameType();
		gt.serializeToXml("gameType", writer);
		
		writer.writeStartElement(ns, "gameTypeKey");
		writer.writeCharacters(KeyFactory.keyToString(gameTypeKey));
		writer.writeEndElement();
		
		writer.writeEndElement();
	}
	
	@Override public void serializeToXml(XMLStreamWriter writer) throws XMLStreamException {
		serializeToXml(getDefaultLocalName(), writer);
	}
	
	@Override public String getNamespaceUri() {
		return Config.getInstance().getGameEngineNamespace();
	}
	
	@Override public String getDefaultLocalName() {
		return "game";
	}
	
	
	@Override
	public Object get(String name, Scriptable start) {
		//TODO: make this more graceful/efficient
		if(name.equals("completed"))
			return this.getCompleted();
		else if(name.equals("created"))
        	return this.getCreated();
        else if (name.equals("currentTime"))
            return this.getCurrentTime();
        else if(name.equals("errorMessage"))
        	return this.getErrorMessage();
        else if(name.equals("findPlayerByRole"))
        	return this.getFindPlayerByRoleFunction();
        else if(name.equals("gameType"))
        	return this.getGameType();
        else if(name.equals("isError"))
        	return this.isError();
        else if(name.equals("key"))
        	return KeyFactory.keyToString(this.getKey());
        else if(name.equals("owner"))
        	return this.getOwner();
        else if(name.equals("players"))
        	return this.getPlayers();
        
        
        return NOT_FOUND;
	}
	
	private FindPlayerByRoleFunction getFindPlayerByRoleFunction() {
		if(findPlayerByRoleFunction_ == null)
			findPlayerByRoleFunction_ = new FindPlayerByRoleFunction(this);
		
		return findPlayerByRoleFunction_;
	}

	@Override
	public Object[] getIds() {
		return new Object[] {
			"completed",
			"created",
			"currentStates",
			"currentTime",
			"errorMessage",
			"findPlayerByRole",
			"gameType",
			"isError",
			"key",
			"owner",
			"players"
		};
	}

	@Override
	public Scriptable getParentScope() {
		return parent_;
	}

	@Override
	public Scriptable getPrototype() {
		return prototype_;
	}

	@Override
	public boolean has(String arg0, Scriptable arg1) {
		String[] ids = (String[])getIds();
		
		return Arrays.binarySearch(ids, arg0) >= 0;
	}

	@Override
	public boolean has(int arg0, Scriptable arg1) {
		return false;
	}

	@Override
	public boolean hasInstance(Scriptable value) {
        Scriptable proto = value.getPrototype();
        while (proto != null) {
            if (proto.equals(this))
                return true;
            proto = proto.getPrototype();
        }

        return false;
	}

	@Override
	public void put(String arg0, Scriptable arg1, Object arg2) {
		// all properties are read only		
	}

	@Override
	public void put(int arg0, Scriptable arg1, Object arg2) {
		// all properties are read only		
	}

	@Override
	public void setParentScope(Scriptable arg0) {
		parent_ = arg0;
	}

	@Override
	public void setPrototype(Scriptable arg0) {
		prototype_ = arg0;
	}

	public void clearError() {
		this.isError_ = false;
		this.errorMessage_ = "";
	}

	public boolean isDirty() {
		return this.isDirty_;
	}

}
