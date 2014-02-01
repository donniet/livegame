<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:txsl="http://www.w3.org/1999/XSL/Transform#Client"
	xmlns="http://www.w3.org/1999/xhtml" 
	xmlns:tic="http://www.livegameengine.com/schemas/games/tictactoe.xsd"
	xmlns:view="http://www.livegameengine.com/schemas/view.xsd"
	xmlns:game="http://www.livegameengine.com/schemas/game.xsd"
	xmlns:scxml="http://www.w3.org/2005/07/scxml"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:fn="http://www.w3.org/2005/xpath-functions">
	
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
			</body>
		</html>
	</xsl:template>
	
	<xsl:template name="players">
		<ul>
			<view:eventHandler event="game.update-meta">
				<view:template match="/">
					<li>
						<span>
							<view:eventHandler event="game.playerConnectionChange">
								<view:parameter name="connected" />
								<view:parameter name="player" />
								<view:template match="/">
									<txsl:if test="$player = '{game:gameUser/game:userid}'">
										<strong>
											<txsl:choose>
												<txsl:when test="$connected = 'true'">connected</txsl:when>
												<txsl:otherwise>disconnected</txsl:otherwise>
											</txsl:choose>
										</strong>
									</txsl:if>
								</view:template>
							</view:eventHandler>
						</span>
						<txsl:value-of select="decendent-or-self::game:gameUser/game:nickname" /> 
						<txsl:value-of select="decendent-or-self::game:gameUser/game:userid" />
					</li>
				</view:template>	
			</view:eventHandler>
		</ul>
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
					<span>
						<view:eventHandler event="board.placement">
							<view:parameter name="px" />
							<view:parameter name="py" />
							<view:parameter name="role" />
							<view:template match="/">
								<txsl:if test="$px == {$x} and $py == {$y}">
									<strong><txsl:value-of select="$role" /></strong>
								</txsl:if>
							</view:template>
						</view:eventHandler>
					</span>
				</xsl:otherwise>
			</xsl:choose>
		</td>
	</xsl:template>
	
</xsl:stylesheet>