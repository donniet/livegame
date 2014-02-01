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
				<view:scripts><![CDATA[
					function updateConnectedStatus(meta) {
						console.log("loading players: " + meta.players.length);
						
						for(var i = 0; i < meta.players.length; i++) {							
							var p = meta.players[i];
							
							console.log("player: " + p.gameUser.key);
							
							var el = document.getElementById("connected-" + p.gameUser.key);
							console.log("element: " + el);
							if(!el) continue;
							
							el.appendChild(document.createTextNode(p.gameUser.connected ? "connected" : "disconnected"));
							
						}
					}
				
					Event.addListener(View.getInstance(), "load", function() {
						console.log("view loaded...");
						
						var resp = Meta.loadMeta();
						
						Event.addListener(resp, "load", function(meta) {
							console.log("loaded meta: " + meta.key);
							
							updateConnectedStatus(meta);
						});
					});
					
					Event.addListener(View.getInstance(), "game.playerConnectionChange", function(event) {
						var player = event.params_["player"];
						var connected = (event.params_["connected"] == "true");
						
						var el = document.getElementById("connected-" + player);
						console.log("element: " + el);
						if(!el) return;
						
						emptyNode(el);
						
						el.appendChild(document.createTextNode(connected ? "connected" : "disconnected"));
						
					});
				]]></view:scripts>
			</view:meta>
			<view:body>
				<input type="button" value="Start">
					<view:event gameEvent="start" event="click" />
				</input>
				<input type="button" value="Join">
					<view:event gameEvent="join" event="click" />
				</input>
				
				<xsl:apply-templates select="scxml:datamodel/scxml:data/tic:board" />
				
				<xsl:call-template name="players" />
			</view:body>
		</view:doc>
	</xsl:template>
	
	<xsl:template name="players">
		<ul>
			<xsl:apply-templates select="$game-meta-doc//game:player" />
		</ul>
	</xsl:template>
	
	<xsl:template match="game:player">
		<li><span id="connected-{game:gameUser/@key}"></span><xsl:value-of select="game:gameUser/game:nickname" /> : <xsl:value-of select="game:role" /></li>
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
		<td>
			<xsl:if test="@highlight = 'true'">
				<xsl:attribute name="class">highlight</xsl:attribute>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="count(tic:mark) > 0">
					<xsl:value-of select="tic:mark/@player" />
				</xsl:when>
				<xsl:otherwise>
					<view:event event="click" endpointEvent="click">
						<tic:square x="{count(preceding-sibling::tic:col)}" y="{count(../preceding-sibling::tic:row)}" />
					</view:event>
				</xsl:otherwise>
			</xsl:choose>
		</td>
	</xsl:template>
	
</xsl:stylesheet>