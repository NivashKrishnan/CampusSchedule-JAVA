<%@ page contentType="text/html;charset=UTF-8" import="app.Security,java.util.*" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Create account | CampusSchedule</title><link rel="stylesheet" href="assets/app.css"><link rel="stylesheet" href="assets/login.css"></head>
<body class="login"><main class="login-card role-signin">
<div class="login-brand"><div class="brand-mark">CS</div><div><h1>CampusSchedule</h1><p>Your personal timetable account</p></div></div>
<div class="login-rule"></div><a class="back-to-logins" href="app">← Back to sign in</a>
<h2 class="login-heading">Create your account</h2><p class="muted">Choose your own username and password. Your administrator will check your details and approve access.</p>
<%if(session.getAttribute("message")!=null){%><div class="notice error" role="alert"><%=Security.html(session.getAttribute("message"))%></div><%session.removeAttribute("message");session.removeAttribute("error");}%>
<%if(Boolean.TRUE.equals(request.getAttribute("registrationUnavailable"))){%><div class="notice error">Registration is temporarily unavailable. Please try again later.</div><%}else{%>
<form action="app" method="post">
<input type="hidden" name="action" value="register"><input type="hidden" name="csrf" value="<%=Security.html(session.getAttribute("csrf"))%>">
<label>Username<input name="username" autocomplete="username" pattern="[A-Za-z0-9_.-]{3,40}" minlength="3" maxlength="40" title="3-40 letters, numbers, dots, underscores or hyphens" required></label>
<label>Account type<select name="role" id="registration-role"><option value="student">Student</option><option value="teacher">Teacher</option></select></label>
<label id="registration-class">Your class<select name="class_id" id="registration-class-input" required><option value="">Choose your class</option><%for(Map<String,String>row:(List<Map<String,String>>)request.getAttribute("registrationClasses")){%><option value="<%=row.get("id")%>"><%=Security.html(row.get("name"))%></option><%}%></select></label>
<label id="registration-teacher" hidden>Your teacher record<select name="faculty_id" id="registration-teacher-input" disabled><option value="">Choose your name</option><%for(Map<String,String>row:(List<Map<String,String>>)request.getAttribute("registrationTeachers")){%><option value="<%=row.get("id")%>"><%=Security.html(row.get("name"))%></option><%}%></select></label>
<label>Password<input type="password" name="password" autocomplete="new-password" minlength="12" maxlength="100" required></label>
<label>Confirm password<input type="password" name="confirm_password" autocomplete="new-password" minlength="12" maxlength="100" required></label>
<p class="muted">Use 12 or more characters. If your class or name is missing, ask your administrator to add it.</p>
<button class="primary full" type="submit">Request account</button></form>
<script>
function registrationRole(){var teacher=document.getElementById('registration-role').value==='teacher';['class','teacher'].forEach(function(kind){var show=(kind==='teacher')===teacher;document.getElementById('registration-'+kind).hidden=!show;var input=document.getElementById('registration-'+kind+'-input');input.disabled=!show;input.required=show;});}
document.getElementById('registration-role').addEventListener('change',registrationRole);registrationRole();
</script><%}%><p class="login-help">Already registered? <a href="app">Sign in</a></p>
</main></body></html>
