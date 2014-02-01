<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:view="http://www.livegameengine.com/schemas/view.xsd"
	xmlns:game="http://www.livegameengine.com/schemas/game.xsd"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:java="http://xml.apache.org/xalan/java"
	xmlns:html="http://www.w3.org/1999/xhtml">

	<xsl:param name="eventEndpointUrl" />
	<xsl:param name="eventEndpointMethod" select="'POST'" />
	<xsl:param name="joinEndpointUrl" />
	<xsl:param name="joinEndpointMethod" select="'POST'" />
	<xsl:param name="startEndpointUrl" />
	<xsl:param name="startEndpointMethod" select="'POST'" />
	<xsl:param name="doctype-public" />
	<xsl:param name="doctype-system" />
	<xsl:param name="jsapiUrl" />
	<xsl:param name="userToken" select="''" />
	
	<xsl:variable name="version" select="0.1" />

	<xsl:output method="xhtml" indent="yes" cdata-section-elements="script"  />
	
	<xsl:template name="xml-to-js-string">
		<xsl:param name="node" />
	
		<xsl:value-of select="java:com.livegameengine.util.Util.escapeJS(java:com.livegameengine.util.Util.serializeXml($node))" />
	</xsl:template>
	
	
	<xsl:template name="extract-payload">
		<xsl:param name="event" select="." />
		
		<xsl:choose>
			<xsl:when test="count($event/view:payload) > 0">
				<xsl:value-of select="java:com.livegameengine.util.Util.escapeJS(java:com.livegameengine.util.Util.serializeXml($event/view:payload/*))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="java:com.livegameengine.util.Util.escapeJS(java:com.livegameengine.util.Util.serializeXml($event/*[namespace-uri() != 'http://www.livegameengine.com/schemas/view.xsd']))" />
			</xsl:otherwise>
		</xsl:choose>		
	</xsl:template>
	
	<xsl:template match="/view:doc">
		<xsl:call-template name="write-doctype" />
		<html>
			<head>
				<xsl:apply-templates select="view:meta" />
				
				<script type="text/javascript">
					View.getInstance().addEventHandlers([
					<xsl:apply-templates select="//view:eventHandler" mode="header" />
					]);
					
					View.getInstance().addEvents([
					<xsl:apply-templates select="//view:event" mode="header" />
					]);
				</script>
				
			</head>
			<body>
				<xsl:call-template name="header" />

				<xsl:apply-templates select="view:body" />

				<xsl:call-template name="footer" />
				
				<div style="display:none;" id="templates">
					<xsl:apply-templates select="//view:eventHandler" mode="templates" />
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template name="write-doctype">
		<xsl:text disable-output-escaping="yes">
&lt;!DOCTYPE html PUBLIC &quot;</xsl:text><xsl:value-of select="$doctype-public" /><xsl:text disable-output-escaping="yes">&quot; &quot;</xsl:text><xsl:value-of select="$doctype-system" /><xsl:text disable-output-escaping="yes">&quot; &gt;
</xsl:text>
	</xsl:template>

	<xsl:template match="view:body">
		<xsl:apply-templates select="*" />
	</xsl:template>
	
	<xsl:template match="view:meta">
		<title>
			<xsl:value-of select="view:title/text()" />
		</title>
		<xsl:call-template name="include-scripts" />
		
		<xsl:apply-templates select="view:styles" />
		<xsl:apply-templates select="view:scripts" />
		<script type="text/javascript">
			<xsl:call-template name="setViewNamespaces" />
			<xsl:call-template name="eventEndpoint" />
			<xsl:call-template name="startEndpoint" />
			<xsl:call-template name="joinEndpoint" />
			<xsl:call-template name="channelSetup" />
		</script>
	</xsl:template>
	
	<xsl:template match="view:event" mode="header">
		{
			id: "<xsl:value-of select="generate-id(.)" />",
			on: "<xsl:value-of select="@on" />",
			gameEvent: "<xsl:value-of select="@gameEvent" />",
			endpointEvent: "<xsl:value-of select="@endpointEvent" />",
			payload: "<xsl:call-template name="extract-payload" />"
		},
	</xsl:template>

	<xsl:template match="view:styles">
		<xsl:choose>
			<xsl:when test="@url">
				<link rel="stylesheet" type="text/css" href="{@url}" />
			</xsl:when>
			<xsl:otherwise>
				<style type="text/css">
					<xsl:value-of select="text()" />
				</style>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="view:scripts">
		<xsl:comment>scripts</xsl:comment>
		<xsl:choose>
			<xsl:when test="@url">
				<script type="text/javascript" src="{@url}"></script>
			</xsl:when>
			<xsl:otherwise>
				<script type="text/javascript">
					<xsl:value-of select="text()" />
				</script>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="eventEndpoint">
		View.getInstance().registerEventEndpoint({
			url:"<xsl:value-of select="$eventEndpointUrl" />",
			method:"<xsl:value-of select="$eventEndpointMethod" />"
		});
	</xsl:template>
	<xsl:template name="startEndpoint">
		View.getInstance().registerStartEndpoint({
			url:"<xsl:value-of select="$startEndpointUrl" />",
			method:"<xsl:value-of select="$startEndpointMethod" />"
		});
	</xsl:template>
	<xsl:template name="joinEndpoint">
		View.getInstance().registerJoinEndpoint({
			url:"<xsl:value-of select="$joinEndpointUrl" />",
			method:"<xsl:value-of select="$joinEndpointMethod" />"
		});
	</xsl:template>
	<xsl:template name="setViewNamespaces">
		<!-- TODO: replace these with namespace-from-prefix or something similar -->
		View.getInstance().setViewNamespace("http://www.livegameengine.com/schemas/view.xsd");
		View.getInstance().setGameNamespace("http://www.livegameengine.com/schemas/game.xsd");
	</xsl:template>
	<xsl:template name="channelSetup">
		<xsl:if test="string-length($userToken) > 0">
		
		View.getInstance().setUserToken("<xsl:value-of select="$userToken" />");
		
		</xsl:if>
		
		<xsl:if test="string-length($userToken) = 0">
			<xsl:comment>no usertoken!</xsl:comment>
		</xsl:if>
	</xsl:template>

	<xsl:template match="view:event">
		<xsl:attribute name="on{@on}">View.getInstance().fireEvent(this, '<xsl:value-of select="generate-id(.)" />')</xsl:attribute>
	</xsl:template>
	
	<xsl:template match="view:eventHandler" mode="header">
		{
			id: "<xsl:value-of select="generate-id(.)" />",
			event: "<xsl:value-of select="@event" />",
			handlerMode: "<xsl:value-of select="@handler-mode" />",
			condition: "<xsl:value-of select="java:com.livegameengine.util.Util.escapeJS(view:condition/@expr)" />",
			resultExpr: "<xsl:value-of select="java:com.livegameengine.util.Util.escapeJS(view:result/@expr)" />",
		},
	</xsl:template>
	
	<xsl:template match="view:eventHandler" mode="templates">
		<xsl:if test="count(view:result/*) > 0">
			<div id="template-{generate-id(.)}">
				<xsl:apply-templates select="view:result/*" />
			</div>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="view:eventHandler">	
		<xsl:variable name="localName">
			<xsl:choose>
				<xsl:when test="string-length(@element-name) > 0">
					<xsl:value-of select="@element-name" />
				</xsl:when>
				<xsl:otherwise>span</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="namespaceURI">
			<xsl:choose>
				<xsl:when test="string-length(@element-namespace) > 0">
					<xsl:value-of select="@element-namespace" />
				</xsl:when>
				<xsl:otherwise>http://www.w3.org/1999/xhtml</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:element name="{$localName}" namespace="{$namespaceURI}">
			<xsl:attribute name="id"><xsl:value-of select="generate-id(.)" /></xsl:attribute>
			
			<xsl:apply-templates select="view:defaultValue/*" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="*|@*|text()">
		<xsl:copy>
			<xsl:apply-templates select="@*" />
			<xsl:apply-templates select="view:event" />
			<xsl:apply-templates select="text()|*[namespace-uri() != 'http://www.livegameengine.com/schemas/view.xsd' or local-name() != 'event']" />
		</xsl:copy>
	</xsl:template>

	<xsl:template name="header">
		<h1>Header</h1>
	</xsl:template>

	<xsl:template name="footer">
		<div><a href="/">home</a></div>
	</xsl:template>

	<xsl:template name="include-scripts">
		<script type="text/javascript" src="{$jsapiUrl}"></script>
		<script type="text/javascript" src="/client/{$version}/event.js"></script>
		<script type="text/javascript" src="/client/{$version}/util.js"></script>
		<script type="text/javascript" src="/client/{$version}/meta.js"></script>
		<script type="text/javascript" src="/client/{$version}/view.js"></script>
	</xsl:template>
</xsl:stylesheet>