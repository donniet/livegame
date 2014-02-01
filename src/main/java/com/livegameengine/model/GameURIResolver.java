package com.livegameengine.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.livegameengine.config.Config;

public class GameURIResolver implements URIResolver {
	private Game game_;
	private XMLOutputFactory factory_ = null;
	
	private static String CURRENT_GAME_URI = "game://current/meta";
	
	public GameURIResolver(Game game) {
		game_ = game;
		factory_ = XMLOutputFactory.newFactory();
	}
	
	@Override
	public Source resolve(String href, String base) throws TransformerException {
		
		Source ret = null;
		
		if(href.equals(CURRENT_GAME_URI)) {
			try {
				ret = createGameSourceXml(game_);
			} catch (XMLStreamException e) {
				throw new TransformerException("could not construct stream from current game", e);
			}
				
		}
		else if(href.startsWith("res://client/")) {
			String clientVersion = game_.getGameType().getClientVersion();
			
			InputStream is = GameURIResolver.class.getResourceAsStream(String.format("/{%s}{%s}", clientVersion, href.substring(12)));
			
			if(is != null) {
				return new StreamSource(is);
			}
			else {
				return null;
			}
		}
		
		
		return ret;
	}
	
	public Source createGameSourceXml(Game game) throws XMLStreamException {
		//TODO: this is inefficient.  All the model classes should implement org.w3c.dom.Node, then we could just use a DOMSource here.
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		XMLStreamWriter writer = factory_.createXMLStreamWriter(baos);
		writer.setDefaultNamespace(Config.getInstance().getGameEngineNamespace());
			
		writer.writeStartDocument();
		
		game.serializeToXml("game", writer);
		
		writer.writeEndDocument();
		writer.flush();
		
		//Log log = LogFactory.getLog(GameURIResolver.class);
		//log.info(baos.toString());
		
		return new StreamSource(new ByteArrayInputStream(baos.toByteArray()));
	}

}
