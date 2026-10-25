<%@ page contentType="text/html;charset=UTF-8" import="app.Security" %>
<%
String selectedRole=(String)request.getAttribute("loginRole");
if(selectedRole==null)selectedRole="";
String[] roleKeys={"admin","teacher","student"};
String[] roleNames={"Admin","Teacher","Student"};
String[] roleDescriptions={"Manage accounts, academic records and timetable planning.","View your assigned lessons and published teaching timetable.","View the published timetable for your class."};
int selectedIndex=-1;
for(int i=0;i<roleKeys.length;i++){if(roleKeys[i].equals(selectedRole))selectedIndex=i;}
boolean choosing=selectedIndex<0;
String roleName=choosing?"":roleNames[selectedIndex];
%>
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title><%=choosing?"Choose your login":roleName+" login"%> | CampusSchedule</title><link rel="stylesheet" href="assets/app.css"><link rel="stylesheet" href="assets/login.css"></head>
<body class="login">
<main class="login-card <%=choosing?"role-picker":"role-signin"%>">
<div class="login-brand"><div class="brand-mark">CS</div><div><h1>CampusSchedule</h1><p>Automated Timetable Generator</p></div></div>
<div class="login-rule"></div>
<%if(choosing){%>
<span class="eyebrow">YOUR CAMPUS. YOUR SCHEDULE.</span>
<h2 class="login-heading">Choose your login</h2>
<p class="muted">Select your account type to open your personal workspace.</p>
<%}else{%>
<a class="back-to-logins" href="app">← All login options</a>
<h2 class="login-heading"><%=roleName%> login</h2>
<p class="muted"><%=roleDescriptions[selectedIndex]%></p>
<%}%>
<% if(session.getAttribute("message")!=null){ %><div class="notice <%=Boolean.TRUE.equals(session.getAttribute("error"))?"error":""%>" role="alert"><%=Security.html(session.getAttribute("message"))%></div><%session.removeAttribute("message");session.removeAttribute("error");}%>
<%if(choosing){%>
<nav class="role-options" aria-label="Choose your account type">
<%for(int i=0;i<roleKeys.length;i++){%>
<a class="role-option" href="app?loginRole=<%=roleKeys[i]%>">
<span class="role-icon" aria-hidden="true"><%=roleNames[i].charAt(0)%></span>
<h3><%=roleNames[i]%></h3><p><%=roleDescriptions[i]%></p>
<span class="role-link">Continue as <%=roleNames[i]%> <span aria-hidden="true">→</span></span>
</a>
<%}%>
</nav>
<%}else{%>
<nav class="login-switcher" aria-label="Account type">
<%for(int i=0;i<roleKeys.length;i++){%>
<a href="app?loginRole=<%=roleKeys[i]%>" <%=i==selectedIndex?"aria-current=\"page\"":""%>><%=roleNames[i]%></a>
<%}%>
</nav>
<form action="app" method="post">
<input type="hidden" name="action" value="login">
<input type="hidden" name="loginRole" value="<%=selectedRole%>">
<input type="hidden" name="csrf" value="<%=Security.html(session.getAttribute("csrf"))%>">
<label for="login-username">Username<input id="login-username" name="username" autocomplete="username" maxlength="80" required autofocus></label>
<label for="login-password">Password<input id="login-password" name="password" type="password" autocomplete="current-password" required></label>
<button class="primary full" type="submit">Sign in as <%=roleName%></button>
</form>
<%}%>
<p class="login-help">New here? <a href="app?page=register">Create your own account</a>. Teachers and students can register. For a password reset, contact your administrator.</p>
<p class="demo-note">CampusSchedule · Your timetable, in one place</p>
</main></body></html>
