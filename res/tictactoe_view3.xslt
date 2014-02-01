<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://www.w3.org/1999/xhtml" 
	xmlns:tic="http://www.livegameengine.com/schemas/games/tictactoe.xsd"
	xmlns:view="http://www.livegameengine.com/schemas/view.xsd"
	xmlns:game="http://www.livegameengine.com/schemas/game.xsd"
	xmlns:scxml="http://www.w3.org/2005/07/scxml"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:fn="http://www.w3.org/2005/xpath-functions">
	
	<xsl:param name="game-meta-uri" select="'game://current/meta'" />
	
	<xsl:variable name="game-meta-doc" select="document($game-meta-uri)" />
	
	<xsl:template match="/">
		<view:doc>
			<view:meta>
				<view:title>Tic Tac Toe</view:title>
				<view:styles>
					td { width: 100px; height: 100px; border: solid 1px black; }
					.highlight { background-color: rgb(50,50,255); }
				</view:styles>
				<view:scripts></view:scripts>
			</view:meta>
			<view:body>
				<div>
				<input type="button" value="Start">
					<view:event gameEvent="start" on="click" />
				</input>
				<input type="button" value="Join">
					<view:event gameEvent="join" on="click" />
				</input>
				</div>
				
				<xsl:apply-templates select="scxml:datamodel/scxml:data/tic:board" />
				
				<xsl:call-template name="players" />
			</view:body>
		</view:doc>
	</xsl:template>
	
	<xsl:template name="players">
		<view:eventHandler event="game.playerJoin" element-name="ul" element-namespace="http://www.w3.org/1999/xhtml" handler-mode="append">
			<view:result>
				<li>{ selectContent('//game:gameUser/game:nickname/text()').nodeValue } { selectContent('//game:gameUser/game:userid/text()').nodeValue }</li>
			</view:result>
			<view:defaultValue>
				<xsl:apply-templates select="$game-meta-doc//game:player" />
			</view:defaultValue>
		</view:eventHandler>
	</xsl:template>
	
	<xsl:template match="game:player">
		<li>
			<view:eventHandler event="game.playerConnectionChange" element-name="span" element-namespace="http://www.w3.org/1999/xhtml">
				<view:condition expr="getParameter('player') == '{game:gameUser/game:userid}'"/>
				<view:result>
					<strong>{ getParameter('connected') == 'true' ? 'connected ' : 'disconnected ' }</strong>
				</view:result> 
			</view:eventHandler>
			
			<xsl:value-of select="game:gameUser/game:nickname" /> : 
			<xsl:value-of select="game:role" />
		</li>
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
	
	<xsl:template match="tic:col">
		<xsl:variable name="x"><xsl:value-of select="count(preceding-sibling::tic:col)" /></xsl:variable>
		<xsl:variable name="y"><xsl:value-of select="count(../preceding-sibling::tic:row)" /></xsl:variable>
		<td>
			<xsl:if test="@highlight = 'true'">
				<xsl:attribute name="class">highlight</xsl:attribute>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="count(tic:mark) > 0">
					<xsl:value-of select="tic:mark/@player" />
				</xsl:when>
				<xsl:otherwise>
					<view:event on="click" endpointEvent="click">
						<tic:square x="{$x}" y="{$y}" />
					</view:event>
					<view:eventHandler event="board.placement" element-name="span" element-namespace="http://www.w3.org/1999/xhtml">
						<view:condition expr="parseInt(getParameter('x')) == {$x} &amp;&amp; parseInt(getParameter('y')) == {$y}" />
						<view:result>
							<strong>{getParameter('role')}</strong>
						</view:result>
					</view:eventHandler>
				</xsl:otherwise>
			</xsl:choose>
		</td>
	</xsl:template>
	
</xsl:stylesheet>