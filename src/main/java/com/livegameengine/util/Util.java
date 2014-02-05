package com.livegameengine.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import javax.jdo.annotations.NotPersistent;
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
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.appengine.api.datastore.KeyFactory;
import com.livegameengine.config.Config;
import com.livegameengine.model.Game;
import com.livegameengine.model.GameUser;
import com.livegameengine.model.Watcher;
import com.livegameengine.model.XmlSerializable;
import com.livegameengine.persist.PMF;


public class Util {
	@Value("${digestAlgorithm}") private String digestAlgorithm;
	@Value("${encoding}") private String encoding;
	@Value("${encryptionAlgorithm}") private String encryptionAlgorithm;
	@Value("${privateKey}") private String privateKey;
	@Value("${gameenginedefaultprefix}") private String gameEngineDefaultNamespacePrefix;
	@Value("${gameenginenamespace}") private String gameEngineNamespace;
	@Value("${datamodeltransformplayeridparam}") private String dataModelTransformPlayerIdParam;
	@Value("${datamodeltransformresource}") private String dataModelTransformResource;
	@Value("${dateFormat}") private String dateFormat;
		
	DateFormat dateFormatter = null;
	DocumentBuilderFactory builderFactory = null;
	DocumentBuilder builder = null;
	TransformerFactory transformerFactory = null;
	
	@Autowired private PMF pmf;
	
	public Util() throws ParserConfigurationException {
		builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setNamespaceAware(true);
		
		builder = builderFactory.newDocumentBuilder();
		
		transformerFactory = TransformerFactory.newInstance();
		
		dateFormatter = new SimpleDateFormat(dateFormat);
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	}	
	
	public PMF getPmf() {
		return pmf;
	}

	public String formatDate(Date d) {
		return dateFormatter.format(d);
	}
	
	public Date parseDate(String s) throws ParseException {
		return dateFormatter.parse(s);
	}
	
	public NodeList serializeToNodeList(String localName, XmlSerializable obj, Node parent) throws XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
	    //factory.setProperty("javax.xml.stream.isPrefixDefaulting",Boolean.TRUE);
				
		XMLStreamWriter writer = factory.createXMLStreamWriter(new DOMResult(parent));

		//writer.setNamespaceContext(this);
		
		writer.setDefaultNamespace(obj.getNamespaceUri());
		writer.setPrefix(gameEngineDefaultNamespacePrefix, gameEngineNamespace);		
		
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
		return transformerFactory.newTransformer();
	}
	
	public Transformer newTransformer(Source source) throws TransformerConfigurationException {
		return transformerFactory.newTransformer(source);
	}
	
	public Document newXmlDocument() {
		return builder.newDocument();
	}
	
	public void transformAsDatamodel(Source source, Result result, String playerid) throws TransformerConfigurationException, TransformerException {
		InputStream dataModelResourceStream = Util.class.getResourceAsStream(dataModelTransformResource);
		
		Transformer trans = transformerFactory.newTransformer(new StreamSource(dataModelResourceStream));
		trans.setParameter(dataModelTransformPlayerIdParam, playerid);				
		
		trans.transform(source, result);
	}
	

	public void transformFromResource(String resourceUrl, Source source, Result result)  
			throws TransformerConfigurationException, TransformerException, FileNotFoundException {
		this.transformFromResource(resourceUrl, source, result, null);
	}
	
	
	public void transformFromResource(String resourceUrl, Source source, Result result, Map<String, Object> params) 
			throws TransformerConfigurationException, TransformerException, FileNotFoundException {
		//InputStream resource = Config.class.getResourceAsStream(resourceUrl);
		FileInputStream resource = new FileInputStream(resourceUrl);
		
		Transformer trans = transformerFactory.newTransformer(new StreamSource(resource));
		
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
	

	public String encryptString(String plain) {
		String ret = null;
		
		try {
			SecretKeySpec key = new SecretKeySpec(privateKey.getBytes(encoding), encryptionAlgorithm);
					
			byte[] input = plain.getBytes(encoding);
			
			Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
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
		String ret = null;
		
		
		try {
			byte[] input = Base64.decodeBase64(crypt);
			
			SecretKeySpec key = new SecretKeySpec(privateKey.getBytes(encoding), encryptionAlgorithm);
			
			Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
			cipher.init(Cipher.DECRYPT_MODE, key);
			
			byte[] decrypted = new byte[cipher.getOutputSize(input.length)];
			int dec_len = cipher.update(input, 0, input.length, decrypted, 0);
			dec_len += cipher.doFinal(decrypted, dec_len);
			
			ret = new String(decrypted, encoding);			
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
	
	
	public String getHash(String str, byte[] salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
			digest.reset();
			digest.update(salt);
			byte[] out = digest.digest(str.getBytes(encoding));
			
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

	private String hex(byte[] array) {
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
	
	public Node findFirstElementNode(Node parent) {
		for(int i = 0; i < parent.getChildNodes().getLength(); i++) {
			Node n = parent.getChildNodes().item(i);
			
			if(n.getNodeType() == Node.ELEMENT_NODE) {
				return n;
			}
		}
		
		return null;
	}
	
	public void writeNode(Node n, XMLStreamWriter writer) throws DOMException, XMLStreamException {
		String prefix = null;
		
		switch(n.getNodeType()) {
		case Node.ATTRIBUTE_NODE:
			if(n.getLocalName() == "xmlns") { 
				writer.writeNamespace("", n.getNodeValue());
			}
			else if(n.getPrefix() == "xmlns") {
				writer.writeNamespace(n.getLocalName(), n.getNodeValue());
			}
			else if(n.getPrefix() != null && n.getPrefix() != "") {
				writer.writeAttribute(n.getPrefix(), n.getNamespaceURI(), n.getLocalName(), n.getNodeValue());
			}
			else { 
				writer.writeAttribute(n.getLocalName(), n.getNodeValue());
			}
			break;
		case Node.CDATA_SECTION_NODE:
			writer.writeCData(n.getNodeValue());
			break;
		case Node.COMMENT_NODE:
			writer.writeComment(n.getNodeValue());
			break;
		case Node.DOCUMENT_NODE:
			writer.writeStartDocument();
			for(int i = 0; i < n.getChildNodes().getLength(); i++) {
				Node m = n.getChildNodes().item(i);
				writeNode(m, writer);
			}
			writer.writeEndDocument();
			break;
		case Node.DOCUMENT_TYPE_NODE:
			writer.writeDTD(n.getNodeValue());
			break;
			
		case Node.DOCUMENT_FRAGMENT_NODE:
			for(int i = 0; i < n.getChildNodes().getLength(); i++) {
				Node m = n.getChildNodes().item(i);
				writeNode(m, writer);
			}
			break;
		case Node.ELEMENT_NODE:
			
			if(n.getPrefix() != null && n.getPrefix() != "")
				writer.writeStartElement(n.getPrefix(), n.getLocalName(), n.getNamespaceURI());
			else
				writer.writeStartElement(n.getLocalName());
			
			for(int i = 0; i < n.getAttributes().getLength(); i++) {
				Node m = n.getAttributes().item(i);
				writeNode(m, writer);
			}
			for(int i = 0; i < n.getChildNodes().getLength(); i++) {
				Node m = n.getChildNodes().item(i);
				writeNode(m, writer);
			}
			
			writer.writeEndElement();
			
			break;
		case Node.ENTITY_REFERENCE_NODE:
			writer.writeEntityRef(n.getNodeName());
			break;
		case Node.TEXT_NODE:
			writer.writeCharacters(n.getNodeValue());
			break;
		}
	}

	public String escapeJS(String in) {
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < in.length(); i++) {
			switch(in.charAt(i)) {
			case '\'':
			case '\"':
			case '\\':
				sb.append('\\').append(in.charAt(i));
				break;
			default:
				sb.append(in.charAt(i));
				break;
			}
		}
		
		return sb.toString();
	}
	
	public String serializeXml(NodeList list) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		StreamResult sr = new StreamResult(bos);
		
		Transformer trans = null;
		try {
			trans = newTransformer();
		} catch (TransformerConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return "";
		}
		
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		
		for(int i = 0; i < list.getLength(); i++) {
			try {
				trans.transform(new DOMSource(list.item(i)), sr);
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return bos.toString();
	}
	
	public String hashGameAndGameUser(final Game gameIn, final GameUser gameUserIn) {
		String ret = "";
		
		try {
			MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
			digest.reset();
			// salt with the class name
			digest.update(Watcher.class.getName().getBytes(encoding));
			digest.update(KeyFactory.keyToString(gameIn.getKey()).getBytes(encoding));
			digest.update(KeyFactory.keyToString(gameUserIn.getKey()).getBytes(encoding));
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

	public String getGameEngineDefaultNamespacePrefix() {
		return gameEngineDefaultNamespacePrefix;
	}

	public String getGameEngineNamespace() {
		return gameEngineNamespace;
	}
}
