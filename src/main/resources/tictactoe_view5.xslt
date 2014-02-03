<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://www.w3.org/1999/xhtml" 
	xmlns:tic="http://www.livegameengine.com/schemas/games/tictactoe.xsd"
	xmlns:view="http://www.livegameengine.com/schemas/view.xsd"
	xmlns:game="http://www.livegameengine.com/schemas/game.xsd"
	xmlns:scxml="http://www.w3.org/2005/07/scxml"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:fn="http://www.w3.org/2005/xpath-functions"
	exclude-result-prefixes="xsl xalan fn scxml game">
	
	<xsl:param name="game-meta-uri" select="'game://current/meta'" />
	
	<xsl:variable name="game-meta-doc" select="document($game-meta-uri)" />
	
	<xsl:template match="/scxml:datamodel">
		<html>
			<head>
				<title>Tic Tac Toe</title>
				<style type="text/css">
					td { width: 100px; height: 100px; border: solid 1px black; }
					.highlight { background-color: rgb(50,50,255); }
				</style>
			</head>
			<body>
				<view:clientNamespace prefix="tic" namespace-uri="http://www.livegameengine.com/schemas/games/tictactoe.xsd" />
			
				<div>
					<input type="button" value="Start">
						<view:event gameEvent="start" on="click" />
					</input>
					<input type="button" value="Join">
						<view:event gameEvent="join" on="click" />
					</input>
				</div>
								
				<xsl:apply-templates select="scxml:data/tic:board" />
			
				<ul>
					<view:eventHandler event="game.playerJoin" mode="append" />
			
					<xsl:apply-templates select="document($game-meta-uri)//game:players/game:player" />
				</ul>
				
				<pre>
					<view:errorDisplay timeout="2000" />
				</pre>
			</body>
		</html>
	</xsl:template>
	
	<xsl:template match="/game:message[game:event = 'game.playerConnectionChange']">
		<strong>
			<xsl:choose>
				<xsl:when test="game:param[@name='connected']/text() = 'true'">connected</xsl:when>
				<xsl:otherwise>disconnected</xsl:otherwise>
			</xsl:choose>
		</strong>
	</xsl:template>
	
	<xsl:template match="/game:message[game:event = 'board.placement']">
		<xsl:for-each select="game:param[@name='role']">
			<xsl:call-template name="placement" />
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template match="/game:message[game:event = 'game.playerJoin']">
		<xsl:apply-templates select="game:content/game:player" />
	</xsl:template>
	
	<xsl:template match="/game:message[game:event = 'game.completeGame']">
		<span>WINNER!</span>
	</xsl:template>
	
	<xsl:template match="game:player">
		<li>
			<span>
				<view:eventHandler event="game.playerConnectionChange" mode="replace"  
					condition="params['playerid'] == '{descendant-or-self::game:gameUser/game:userid}'">
				</view:eventHandler>
			</span>
			
			<span>
				<view:eventHandler event="game.completeGame" mode="replace" 
					condition="params['winner'] == '{descendant-or-self::game:role}'" />
			</span>
			
			<xsl:value-of select="descendant-or-self::game:gameUser/game:nickname" /> |
			<xsl:value-of select="descendant-or-self::game:gameUser/game:userid" /> |
			<xsl:value-of select="descendant-or-self::game:role" />
		</li>
	</xsl:template>
	
	<xsl:template match="/game:message" priority="-100">
		<xsl:apply-templates select="game:content/*" mode="copy" />
	</xsl:template>
	
	<xsl:template match="@*|*|text()" mode="copy">
		<xsl:copy>
			<xsl:apply-templates select="@*|*|text()" mode="copy" />
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="tic:board">
		<table>
			<xsl:apply-templates select="tic:row" />
		</table>
	</xsl:template>
	
	<xsl:template match="tic:row">
		<tr>
			<xsl:apply-templates select="tic:col" />
		</tr>
	</xsl:template>
	
	<xsl:template name="placement">
		<strong><xsl:value-of select="." /></strong>
	</xsl:template>
	
	<xsl:template match="tic:col">
		<xsl:variable name="x"><xsl:value-of select="count(preceding-sibling::tic:col)" /></xsl:variable>
		<xsl:variable name="y"><xsl:value-of select="count(../preceding-sibling::tic:row)" /></xsl:variable>
		<td id="x{$x}y{$y}">
			<xsl:if test="@highlight = 'true'">
				<xsl:attribute name="class">highlight</xsl:attribute>
			</xsl:if>
			
			<view:eventHandler event="board.highlight" mode="attribute" 
				condition="select('count(//tic:row[{$y+1}]/tic:col[position() = {$x+1} and @highlight = &quot;true&quot;]) > 0')">
				<view:attribute name="class">highlight</view:attribute>
			</view:eventHandler>
			
			<xsl:choose>
				<xsl:when test="count(tic:mark) > 0">
					<xsl:for-each select="tic:mark/@player">
						<xsl:call-template name="placement" />
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<view:event on="click" endpointEvent="click">
						<tic:square x="{$x}" y="{$y}" />
					</view:event>
					<view:eventHandler event="board.placement" mode="replace"
						condition="parseInt(params['x']) == {$x} &amp;&amp; parseInt(params['y']) == {$y}">
					</view:eventHandler>
				</xsl:otherwise>
			</xsl:choose>
		</td>
	</xsl:template>
	
</xsl:stylesheet>