<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.google.appengine.api.datastore.KeyFactory" %>
<%@ page import="com.livegameengine.model.GameType" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta name="viewport" content="width=480, initial-scale=1" />
<title>Live Game Engine</title>

<style type="text/css">
@font-face {
	font-family: "Fanwood";
	src: url("/client/0.2/fonts/Fanwood.otf");
}
body, input {
	font-family: Fanwood, Serif;
}
form {
	display: inline;
}
input {
	font-family: Fanwood;
}

body {
	position:absolute;
	margin: 10px;
	width: 100%;
}

h1, h2, section, li, ul {
	margin: 0px;
	padding: 0px;
	list-style-type: none;
}
h1 {
	height:50px;
	max-height:50px;
	overflow:hidden;
}
h2 {
	height:30px;
}

.user {
	position: absolute;
	text-align: right;
	right: 10px;
	top: 10px;
}

input {
	height:25px;
	font-size: 20px;
}

@media only screen and (min-width:481px) {

section.game-types {
	position:absolute;
	width: 300px;
	right:0px;
	top: 50px;
	
}
section.games {
	position:absolute;
	padding-right:300px;
	left:0px;
	top: 50px;
	
}

#create-type-form {
	position:absolute;
	top: 0px;
	right: 10px;
}

}

@media only screen and (max-width:480px) {

section.game-types {
	position:static;
	width: 100%;
	
}
section.games {
	position:static;
	width: 100%;
	
}

#create-type-form {
	position:absolute;
	right:10px;
	margin-top: -30px;
}

}

</style>

</head>

<body>

<header>
<h1>Live Game Engine</h1>
<div class="user">
	<c:choose>
		<c:when test="${authenticated}">
			<a href="${logout_url}">Sign Out</a>
		</c:when>
		<c:otherwise>
			<a href="${login_url}">Sign In</a>
		</c:otherwise>
	</c:choose>
</div>
</header>

<section class="game-types">
<h2>Game Types</h2>

<form action="type/create" method="POST" id="create-type-form">
<input type="submit" value="Create" />
</form>


<ul>
	<c:forEach var="type" items="${types}">
		<li>
			<a href="">${type.typeName}</a>
			<form action="type/-/<%= KeyFactory.keyToString(((GameType)pageContext.getAttribute("type")).getKey()) %>/create" method="POST">
				<input type="submit" value="New Game" />
			</form>
		</li>
	</c:forEach>
</ul>
</section>

<section class="games">
<h2>Games</h2>
</section>

<footer>
	
</footer>


</body>
</html>
