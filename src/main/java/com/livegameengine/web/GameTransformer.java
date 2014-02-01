package com.livegameengine.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.appengine.api.datastore.Key;
import com.livegameengine.config.Config;
import com.livegameengine.model.ClientMessage;
import com.livegameengine.model.Game;
import com.livegameengine.model.GameState;
import com.livegameengine.model.GameStateData;
import com.livegameengine.model.GameType;
import com.livegameengine.model.GameURIResolver;
import com.livegameengine.model.GameUser;
import com.livegameengine.model.NameValuePair;
import com.livegameengine.util.Util;

public class GameTransformer extends Transformer {
	public static String PROPERTY_OUTPUT_TYPE = "game.output_type";
	public static String PROPERTY_OUTPUT_TYPE_VIEW = "View";
	public static String PROPERTY_OUTPUT_TYPE_RAW = "Raw";
	public static String PROPERTY_OUTPUT_TYPE_DATA = "Data";
		
	private static Config config = Config.getInstance();
	
	Log log = LogFactory.getLog(GameTransformer.class);
	
	public enum OutputType {
		View, Raw, Data
	}
	
	private Game game_ = null;
	private GameUser gameUser_ = null;
	private OutputType outputType_ = OutputType.View;
	
	Map<String,Object> params_ = new HashMap<String,Object>();
	Properties props_ = new Properties();
	ErrorListener listener_ = null;
	
	public GameTransformer(GameUser player) {
		gameUser_ = player;
	}
	
	public GameTransformer(Game game, GameUser player) {
		game_ = game;
		gameUser_ = player;
	}
	
	@Override
	public void transform(Source xmlSource, Result outputTarget)
			throws TransformerException {
		
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
		
		if(GameSource.class.isAssignableFrom(xmlSource.getClass())) {
			GameSource s = (GameSource)xmlSource;
					
			Document doc = config.newXmlDocument();
			
			Node datamodelNode = doc.createElementNS(config.getSCXMLNamespace(), "datamodel");
			doc.appendChild(datamodelNode);
			
			List<GameStateData> datamodel = s.getGameState().getDatamodel();
			
			Transformer trans = null;
			
			for(Iterator<GameStateData> i = datamodel.iterator(); i.hasNext();) {
				GameStateData gsd = i.next();

				trans = config.newTransformer();
				
				gsd.appendValueInto(datamodelNode);
			}
			
			transform_xml_helper(new DOMSource(doc), outputTarget, s.getGame());
		}
		else if(ClientMessageSource.class.isAssignableFrom(xmlSource.getClass())) {
			try {
				ClientMessageSource s = (ClientMessageSource)xmlSource;
				
				Document doc = config.newXmlDocument();
				
				Node messagesNode = doc.createElementNS(config.getGameEngineNamespace(), "messages");
				doc.appendChild(messagesNode);
				
				XMLStreamWriter messagewriter = factory.createXMLStreamWriter(new DOMResult(messagesNode));
				
				Key lastKey = null;
				Game g = null;
				
				
				for(Iterator<ClientMessage> i = s.getMessages().iterator(); i.hasNext();) {
					ClientMessage cm = i.next();
					
					if(lastKey != cm.getGameKey()) {
						lastKey = cm.getGameKey();
						g = Game.findUninitializedGameByKey(lastKey);
					}
					
					Document doc1 = config.newXmlDocument();
					Document doc2 = config.newXmlDocument();
					
					DocumentFragment frag = doc2.createDocumentFragment();
					
					XMLStreamWriter doc1writer = factory.createXMLStreamWriter(new DOMResult(doc1));
					cm.serializeToXml("message", doc1writer);
					
					Node content = Util.findFirstElementNode(doc1);
										
					transform_xml_helper(new DOMSource(content), new DOMResult(frag), g, cm.getParametersAsMap());
										
					cm.serializeToXml("message", messagewriter, frag);
					
					if(!i.hasNext()) {
						Node latestDateNode = doc.createAttribute("latestDate");
						latestDateNode.setNodeValue(config.getDateFormat().format(cm.getMessageDate()));
						messagesNode.getAttributes().setNamedItem(latestDateNode);
					}
				}				
				
				Transformer t = config.newTransformer();
				t.transform(new DOMSource(doc), outputTarget);
			}
			catch(XMLStreamException e) {
				throw new TransformerException("XML Stream Exception", e);
			}
		}
		else if(game_ != null){
			// normal source...
			transform_xml_helper(xmlSource, outputTarget, game_);			
		}
		else {
			throw new TransformerException("invalid source type: " + xmlSource.getClass().getName());
		}
	}
	
	private void transform_xml_helper(Source xmlSource, Result outputTarget, Game g)
			throws TransformerException {
		transform_xml_helper(xmlSource, outputTarget, g, null);
	}
	
	private void transform_xml_helper(Source xmlSource, Result outputTarget, Game g, Map<String,String> viewParams)
			throws TransformerException {
		
		Transformer ident = config.newTransformer();
		ByteArrayOutputStream temp = new ByteArrayOutputStream();
		
		ident.transform(xmlSource, new StreamResult(temp));
		
		//log.info("source: " + temp.toString());
		
		Document doc = config.newXmlDocument();
		
		GameType gt = g.getGameType();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		Result res1 = new StreamResult(bos);
				
		Source frontEndSource = new StreamSource(GameTransformer.class.getResourceAsStream("/pilgrims_view.xslt"));
		/*
		ByteArrayInputStream bis = new ByteArrayInputStream(gt.getFrontEnd());
		Source frontEndSource = new StreamSource(bis);
		*/

		String viewResourceName = String.format("/%s/game_view.xslt", gt.getClientVersion());
				
		Transformer frontendTrans = config.newTransformer(frontEndSource);
		frontendTrans.setOutputProperty(OutputKeys.METHOD, "xml");
		frontendTrans.setOutputProperty(OutputKeys.MEDIA_TYPE, "application/xml");
		frontendTrans.setURIResolver(new GameURIResolver(g));
		
		if(viewParams != null) {
			for(Iterator<String> i = viewParams.keySet().iterator(); i.hasNext();) {
				String key = i.next();
				
				frontendTrans.setParameter(key, viewParams.get(key));
			}
		}
		
		
		//frontendTrans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				
		Map<String,Object> params = new HashMap<String,Object>();
		
		params.put("eventEndpointUrl", "event/{event}");
		params.put("gameEventEndpointUrl", "{gameEvent}");
		params.put("doctype-public", config.getViewDoctypePublic());
		params.put("doctype-system", config.getViewDoctypeSystem());
		params.put("jsapiUrl", "/_ah/channel/jsapi");
		params.put("serverTime", config.getDateFormat().format(new Date()));
		params.put("clientMessageUrl", "message");
		
		params.put("version", gt.getClientVersion());
		
		DOMResult dres1 = new DOMResult();
		DOMResult dres2 = new DOMResult();
		
		switch(outputType_) {
		case Data:
			//config.transformAsDatamodel(xmlSource, outputTarget, gameUser_.getHashedUserId());
			config.transformAsDatamodel(new StreamSource(new ByteArrayInputStream(temp.toByteArray())), outputTarget, gameUser_.getHashedUserId());
			break;
		case Raw:
			//config.transformAsDatamodel(xmlSource, new DOMResult(doc), gameUser_.getHashedUserId());
			
			config.transformAsDatamodel(new StreamSource(new ByteArrayInputStream(temp.toByteArray())), dres1, gameUser_.getHashedUserId());
			frontendTrans.transform(new DOMSource(dres1.getNode()), outputTarget);
			break;
		case View:
		
			//config.transformAsDatamodel(xmlSource, new DOMResult(doc), gameUser_.getHashedUserId());
			config.transformAsDatamodel(new StreamSource(new ByteArrayInputStream(temp.toByteArray())), dres1, gameUser_.getHashedUserId());
			
			frontendTrans.transform(new DOMSource(dres1.getNode()), dres2);
			
			//log.info("content: " + bos.toString());
			
			config.transformFromResource(viewResourceName, new DOMSource(dres2.getNode()), outputTarget, params);
			
			break;
		}
	}

	@Override
	public void setParameter(String name, Object value) {
		params_.put(name, value);
	}

	@Override
	public Object getParameter(String name) {
		return params_.get(name);
	}

	@Override
	public void clearParameters() {
		params_.clear();
	}

	@Override
	public void setURIResolver(URIResolver resolver) {}

	@Override
	public URIResolver getURIResolver() {
		return null;
	}

	@Override
	public void setOutputProperties(Properties oformat) {
		props_.clear();
		this.outputType_ = OutputType.View;
		
		if(oformat != null) {
			for(Iterator<Object> i = oformat.keySet().iterator(); i.hasNext();) {
				String key = (String)i.next();
				
				setOutputProperty(key, oformat.getProperty(key));
			}
		}
	}

	@Override
	public Properties getOutputProperties() {
		return props_;
	}

	@Override
	public void setOutputProperty(String key, String value)
			throws IllegalArgumentException {

		if(key.equals(PROPERTY_OUTPUT_TYPE)) {
			try {
				this.outputType_ = OutputType.valueOf(value);
			}
			catch(Exception e) {
				throw new IllegalArgumentException("value not of type OutputType");
			}
		}
		else {
			props_.put(key, value);
		}
	}

	@Override
	public String getOutputProperty(String name)
			throws IllegalArgumentException {
		if(name.equals(PROPERTY_OUTPUT_TYPE)) {
			return this.outputType_.toString();
		}
		else {
			return props_.getProperty(name);
		}
	}

	@Override
	public void setErrorListener(ErrorListener listener)
			throws IllegalArgumentException {
		listener_ = listener;
	}

	@Override
	public ErrorListener getErrorListener() {
		return listener_;
	}

}
