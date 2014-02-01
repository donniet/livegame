<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://www.w3.org/1999/xhtml" 
	xmlns:tic="http://www.livegameengine.com/schemas/games/tictactoe.xsd"
	xmlns:view="http://www.livegameengine.com/schemas/view.xsd"
	xmlns:scxml="http://www.w3.org/2005/07/scxml"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:fn="http://www.w3.org/2005/xpath-functions">
	
	<xsl:template match="/">
		<view:doc>
			<view:meta>
				<view:title>Tic Tac Toe</view:title>
				<view:styles>
					td { width: 100px; height: 100px; border: solid 1px black; }
					.highlight { background-color: rgb(50,50,255); }
				</view:styles>
			</view:meta>
			<view:body>
				<xsl:apply-templates select="scxml:datamodel/scxml:data/tic:board" />
			</view:body>
		</view:doc>
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