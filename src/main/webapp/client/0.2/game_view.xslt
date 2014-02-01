<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:view="http://www.livegameengine.com/schemas/view.xsd"
	xmlns:game="http://www.livegameengine.com/schemas/game.xsd"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:java="http://xml.apache.org/xalan/java"
	xmlns:html="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="xsl view game xalan java html">

	<xsl:param name="eventEndpointUrl" />
	<xsl:param name="eventEndpointMethod" select="'POST'" />
	<xsl:param name="gameEventEndpointUrl" />
	<xsl:param name="gameEventEndpointMethod" select="'POST'" />
	<xsl:param name="doctype-public" />
	<xsl:param name="doctype-system" />
	<xsl:param name="jsapiUrl" />
	<xsl:param name="userToken" select="''" />
	<xsl:param name="titlePrefix" select="'Live Game Engine - '" />
	<xsl:param name="clientMessageUrl" />
	<xsl:param name="serverTime" />
	
	<xsl:variable name="version" select="0.2" />
	
	<xsl:output method="xhtml" indent="yes" cdata-section-elements="script"  />
		
	<xsl:template name="write-doctype">
		<xsl:text disable-output-escaping="yes">
&lt;!DOCTYPE html PUBLIC &quot;</xsl:text><xsl:value-of select="$doctype-public" /><xsl:text disable-output-escaping="yes">&quot; &quot;</xsl:text><xsl:value-of select="$doctype-system" /><xsl:text disable-output-escaping="yes">&quot; &gt;
</xsl:text>
	</xsl:template>
	
	<xsl:template match="game:root">
		<xsl:apply-templates />
	</xsl:template>
		
	<xsl:template match="comment()">
		<xsl:comment>
			<xsl:value-of select="." />
		</xsl:comment>
	</xsl:template>
		
	<xsl:template match="html:html">
		<xsl:call-template name="write-doctype" />
		<xsl:copy>
			<xsl:apply-templates select="@*|*|text()" />
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="html:title">
		<xsl:copy>
			<xsl:value-of select="concat($titlePrefix, text())" />
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="html:head">
		<xsl:copy>
			<script type="text/javascript" src="/client/{$version}/event.js"></script>
			<script type="text/javascript" src="/client/{$version}/view.js"></script>
			<script type="text/javascript">
				View.setClientMessageChannelUrl("<xsl:value-of select="$clientMessageUrl" />");
				View.setServerLoadTime("<xsl:value-of select="$serverTime" />");
				View.setEventEndpoint({ "url": "<xsl:value-of select="$eventEndpointUrl" />", "method":"<xsl:value-of select="$eventEndpointMethod" />" });
				View.setGameEventEndpoint({ "url": "<xsl:value-of select="$gameEventEndpointUrl" />", "method":"<xsl:value-of select="$gameEventEndpointMethod" />" });
				View.setGameViewNamespace("http://www.livegameengine.com/schemas/view.xsd");
				View.setGameNamespace("http://www.livegameengine.com/schemas/game.xsd");
			</script>
			
			<xsl:apply-templates select="@*|*|text()" />	
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="@*|*|text()" priority="-20">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|text()" />
		</xsl:copy>
	</xsl:template>
	
</xsl:stylesheet>