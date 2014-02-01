package com.livegameengine.config;


import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.livegameengine.model.Game;
import com.livegameengine.model.GameState;
import com.livegameengine.model.GameStateData;
import com.livegameengine.model.XmlSerializable;

import flexjson.transformer.DateTransformer;

public class Config implements NamespaceContext {
	private static Config instance_ = null;
	
	private Properties props_ = null;
	private String dateFormatString_ = null;
	private DateFormat dateFormat_ = null;
	private DateTransformer dateTransformer_ = null;
	private Random random_ = null;
	private String encoding_ = null;
	private String digestAlgorithm_ = null;
	private String encryptionAlgorithm_ = null;
	private byte[] privateKey_ = new byte[] {0x0};
	private byte[] initializationVector_ = new byte[] {0x0};
	private String gameEngineDefaultNamespacePrefix_ = "";
	private String gameEngineNamespace_ = "";
	private String gameEventTargetType_;
	private String gameEventTarget_;
	private String watcherEventTarget_;
	private String gameViewNamespace_ = "";
	private String scxmlNamespace_ = "";
	private int maxScriptRuntime_ = 1;
	private String datamodeltransformresource_ = "";
	private String datamodeltransformplayeridparam_ = "";
	private int stateHistorySize_ = 100;
	private int maxPlayers_ = 100;
	private TransformerFactory transformerFactory_ = null;
	private DocumentBuilderFactory documentBuilderFactory_ = null;
	private DocumentBuilder documentBuilder_ = null;
	private String viewDoctypeName_ = null;
	private String viewDoctypeSystem_ = null;
	private String viewDoctypePublic_ = null;
	
	private MultiMap aliasMap_ = null;
	private Map<String,String> namespaceMap_ = null;
	
	public static Config getInstance() {
		if(instance_ == null) {
			instance_ = new Config();
		}
		return instance_;
	}
	
	private Config() {
		props_ = new Properties();
		
		aliasMap_ = new MultiHashMap();
		namespaceMap_ = new HashMap<String,String>();
		 
		try {
			props_.load(Config.class.getResourceAsStream("/app.properties"));
			
			dateFormatString_ = props_.getProperty("dateFormat");
			dateFormat_ = new SimpleDateFormat(dateFormatString_);
			dateFormat_.setTimeZone(TimeZone.getTimeZone("GMT"));
			
			dateTransformer_ = new DateTransformer(dateFormatString_);
						
			encoding_ = props_.getProperty("encoding");
			digestAlgorithm_ = props_.getProperty("digestAlgorithm");
			encryptionAlgorithm_ = props_.getProperty("ecryptionAlgorithm");
			privateKey_ = props_.getProperty("privateKey").getBytes(encoding_);
			initializationVector_ = props_.getProperty("initializationVector").getBytes(encoding_);
			
			gameEngineNamespace_ = props_.getProperty("gameenginenamespace");
			gameEngineDefaultNamespacePrefix_ = props_.getProperty("gameenginedefaultprefix");
			gameEventTargetType_ = props_.getProperty("gameeventtargettype");
			gameEventTarget_ = props_.getProperty("gameeventtarget");
			watcherEventTarget_ = props_.getProperty("watchereventtarget");
			gameViewNamespace_ = props_.getProperty("gameviewnamespace");
			scxmlNamespace_ = props_.getProperty("scxmlnamespace");
			
			aliasMap_.put(gameEngineNamespace_, gameEngineDefaultNamespacePrefix_);
			aliasMap_.put(gameEngineNamespace_, "");
			namespaceMap_.put(gameEngineDefaultNamespacePrefix_, gameEngineNamespace_);
			namespaceMap_.put("", gameEngineNamespace_);
			
			aliasMap_.put(scxmlNamespace_, "scxml");
			namespaceMap_.put("scxml", scxmlNamespace_);
			
			aliasMap_.put(gameViewNamespace_, "view");
			namespaceMap_.put("view", gameViewNamespace_);
			
			maxScriptRuntime_ = Integer.parseInt(props_.getProperty("maxScriptRuntime"));
			
			datamodeltransformresource_ = props_.getProperty("datamodeltransformresource");
			datamodeltransformplayeridparam_ = props_.getProperty("datamodeltransformplayeridparam");
			
			stateHistorySize_ = Integer.parseInt(props_.getProperty("stateHistorySize"));
			maxPlayers_ = Integer.parseInt(props_.getProperty("maxPlayers"));
			
			transformerFactory_ = TransformerFactory.newInstance();
			documentBuilderFactory_ = DocumentBuilderFactory.newInstance();
			documentBuilderFactory_.setNamespaceAware(true);
			
			documentBuilder_ = documentBuilderFactory_.newDocumentBuilder();
			
			viewDoctypeName_ = props_.getProperty("viewDoctypeName");
			viewDoctypePublic_ = props_.getProperty("viewDoctypePublic");
			viewDoctypeSystem_ = props_.getProperty("viewDoctypeSystem");
			
			random_ = new SecureRandom();
			//dateFormat_.setCalendar(new GregorianCalendar());
		}
		catch(IOException e) {
			//TODO: do something better here...
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public InputStream getDataModelTransformStream() {
		return Config.class.getResourceAsStream(datamodeltransformresource_);
	}
	public String getDataModelTransformPlayerIdParam() {
		return datamodeltransformplayeridparam_;
	}
	
	public DateFormat getDateFormat() {
		return dateFormat_;
	}
	public String getDateFormatString() {
		return dateFormatString_;
	}
	public DateTransformer getDateTransfomer() {
		return dateTransformer_;
	}
	public Random getRandom() {
		return random_;
	}
	public String getEncoding() {
		return encoding_;
	}
	public String getDigestAlgorithm() {
		return digestAlgorithm_;
	}
	public byte[] getPrivateKey() {
		return privateKey_;
	}
	public String getEncryptionAlgorithm() {
		return encryptionAlgorithm_;
	}
	public byte[] getInitializationVector() {
		return initializationVector_;
	}
	public String getGameEngineDefaultNamespacePrefix() {
		return gameEngineDefaultNamespacePrefix_;
	}
	public String getGameEngineNamespace() { 
		return gameEngineNamespace_;
	}
	public String getGameEventTargetType() {
		return gameEventTargetType_;
	}
	public String getGameEventTarget() {
		return gameEventTarget_;
	}
	public String getWatcherEventTarget() {
		return watcherEventTarget_;
	}
	public String getGameViewNamespace() {
		return gameViewNamespace_;
	}
	public String getSCXMLNamespace() { 
		return scxmlNamespace_;
	}
	public int getStateHistorySize() {
		return stateHistorySize_;
	}
	public int getMaxPlayers() {
		return maxPlayers_;
	}
	public String getViewDoctypeName() {
		return viewDoctypeName_;
	}
	public String getViewDoctypeSystem() {
		return viewDoctypeSystem_;
	}
	public String getViewDoctypePublic() {
		return viewDoctypePublic_;
	}
	private static String hex(byte[] array) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length; ++i) {
		sb.append(Integer.toHexString((array[i]
				& 0xFF) | 0x100).substring(1,3));       
		}
		return sb.toString();
	}
	public String getMD5Hash (String message) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return hex (md.digest(message.getBytes("CP1252")));
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	/*
	public String getMD5Hash(String str) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] out = digest.digest(str.getBytes(getEncoding()));
			BigInteger outInt = new BigInteger(out); 
			
			return outInt.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return null;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	*/
	public String getHash(String str, byte[] salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance(getDigestAlgorithm());
			digest.reset();
			digest.update(salt);
			byte[] out = digest.digest(str.getBytes(getEncoding()));
			
			return Base64.encodeBase64URLSafeString(out);
		}
		catch(UnsupportedEncodingException e) {
			//TODO: add logging
			return null;
		}
		catch(NoSuchAlgorithmException e) {
			//TODO: add logging
			return null;
		}
	}

	public int getMaxScriptRuntime() {
		return maxScriptRuntime_;
	}
	
	public String encryptString(String plain) {
		SecretKeySpec key = new SecretKeySpec(getPrivateKey(), getEncryptionAlgorithm());
		String ret = null;
				
		try {
			byte[] input = plain.getBytes(getEncoding());
			
			Cipher cipher = Cipher.getInstance(getEncryptionAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] encrypted = new byte[cipher.getOutputSize(input.length)];
			int enc_len = cipher.update(input, 0, input.length, encrypted, 0);
			enc_len += cipher.doFinal(encrypted, enc_len);
			
			ret = Base64.encodeBase64String(encrypted);
			
		} 
		catch (NoSuchAlgorithmException e) {} 
		catch (NoSuchPaddingException e) {} 
		catch (InvalidKeyException e) {
			e.printStackTrace();
		} 
		catch (ShortBufferException e) {} 
		catch (UnsupportedEncodingException e) {} 
		catch (IllegalBlockSizeException e) {} 
		catch (BadPaddingException e) {}
		
		return ret;
		
	}
	public String decryptString(String crypt) {
		SecretKeySpec key = new SecretKeySpec(getPrivateKey(), getEncryptionAlgorithm());
		String ret = null;
		
		byte[] input = Base64.decodeBase64(crypt);
		
		try {
			Cipher cipher = Cipher.getInstance(getEncryptionAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, key);
			
			byte[] decrypted = new byte[cipher.getOutputSize(input.length)];
			int dec_len = cipher.update(input, 0, input.length, decrypted, 0);
			dec_len += cipher.doFinal(decrypted, dec_len);
			
			ret = new String(decrypted, getEncoding());			
		} 
		catch (NoSuchAlgorithmException e) {} 
		catch (NoSuchPaddingException e) {} 
		catch (InvalidKeyException e) {} 
		catch (ShortBufferException e) {} 
		catch (UnsupportedEncodingException e) {} 
		catch (IllegalBlockSizeException e) {} 
		catch (BadPaddingException e) {}
		
		return ret;
	}
	
	public Document newXmlDocument() {
		return documentBuilder_.newDocument();
	}
	
	public DocumentBuilder getDocumentBuilder() {
		return documentBuilder_;
	}
	
	@Override
	public Iterator getPrefixes(String namespaceURI) {
		Collection prefixes = (Collection)aliasMap_.get(namespaceURI);
		
		if(prefixes != null) {
			return prefixes.iterator();
		}
		else {
			return IteratorUtils.EMPTY_ITERATOR;
		}
	}
	
	@Override
	public String getPrefix(String namespaceURI) {
		Collection prefixes = (Collection)aliasMap_.get(namespaceURI);
		
		if(prefixes == null || prefixes.isEmpty()) {
			return null;
		}
		else {
			return (String)prefixes.iterator().next();
		}
	}
	
	@Override
	public String getNamespaceURI(String prefix) {
		return namespaceMap_.get(prefix);
	}
	
	public NodeList serializeToNodeList(String localName, XmlSerializable obj, Node parent) throws XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
	    //factory.setProperty("javax.xml.stream.isPrefixDefaulting",Boolean.TRUE);
				
		XMLStreamWriter writer = factory.createXMLStreamWriter(new DOMResult(parent));

		//writer.setNamespaceContext(this);
		
		writer.setDefaultNamespace(obj.getNamespaceUri());
		writer.setPrefix(getGameEngineDefaultNamespacePrefix(), getGameEngineNamespace());		
		
		obj.serializeToXml(localName, writer);
		
		return parent.getChildNodes();
	}
	
	public NodeList serializeToNodeList(String localName, XmlSerializable obj) throws XMLStreamException {
		Document doc = newXmlDocument();
				
		return serializeToNodeList(localName,  obj, doc);
	}
	
	public NodeList serializeToNodeList(XmlSerializable obj) throws XMLStreamException {
		return serializeToNodeList(obj.getDefaultLocalName(), obj);
	}

	public Transformer newTransformer() throws TransformerConfigurationException {
		return transformerFactory_.newTransformer();
	}
	
	public Transformer newTransformer(Source source) throws TransformerConfigurationException {
		return transformerFactory_.newTransformer(source);
	}
	/*
	public void transformDatamodel(GameState gameState, Result result, String playerid) throws TransformerConfigurationException, TransformerException {
		Document doc = this.newXmlDocument();
		
		Node datamodelNode = doc.createElementNS(this.getSCXMLNamespace(), "datamodel");
		doc.appendChild(datamodelNode);
		
		List<GameStateData> datamodel = gameState.getDatamodel();
		
		Transformer trans = null;
		
		for(Iterator<GameStateData> i = datamodel.iterator(); i.hasNext();) {
			GameStateData gsd = i.next();

			trans = transformerFactory_.newTransformer();
			
			gsd.appendValueInto(datamodelNode);
		}
		
		transformAsDatamodel(new DOMSource(doc), result, playerid);
	}
	*/
	public void transformAsDatamodel(Source source, Result result, String playerid) throws TransformerConfigurationException, TransformerException {
		Transformer trans = transformerFactory_.newTransformer(new StreamSource(getDataModelTransformStream()));
		trans.setParameter(this.getDataModelTransformPlayerIdParam(), playerid);				
		
		trans.transform(source, result);
	}
	

	public void transformFromResource(String resourceUrl, Source source, Result result)  
			throws TransformerConfigurationException, TransformerException {
		this.transformFromResource(resourceUrl, source, result, null);
	}
	
	
	public void transformFromResource(String resourceUrl, Source source, Result result, Map<String, Object> params) 
			throws TransformerConfigurationException, TransformerException {
		InputStream resource = Config.class.getResourceAsStream(resourceUrl);
		
		Transformer trans = transformerFactory_.newTransformer(new StreamSource(resource));
		
		if(trans == null) {
			throw new TransformerException("transformer not valid");
		}
		
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		
		if(params != null) {
			for(Iterator<String> i = params.keySet().iterator(); i.hasNext();) {
				String key = i.next();
				
				trans.setParameter(key, params.get(key));
			}
		}
		
		trans.transform(source, result);
	}
}
