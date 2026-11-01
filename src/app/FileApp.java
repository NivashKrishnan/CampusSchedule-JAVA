package app;

import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.*;
import javax.servlet.http.*;

/** Shared-file backend for the existing JSP interface. SQL mode remains available. */
public final class FileApp {
    private static String t(HttpServletRequest r,String k){return Portal.text(r,k);}
    private static int n(String v){return Portal.number(v,0,Integer.MAX_VALUE);}
    private static boolean eq(String a,String b){return a!=null&&a.equals(b);}
    private static Map<String,String> row(String... pairs){Map<String,String> m=new LinkedHashMap<String,String>();for(int i=0;i<pairs.length;i+=2)m.put(pairs[i],pairs[i+1]);return m;}
    private static String now(){return java.time.Instant.now().toString();}
    private static boolean cloud(){return "true".equalsIgnoreCase(System.getenv("TIMETABLE_PUBLIC_DEPLOYMENT"));}
    private static String cookieName(String kind){return (cloud()?"__Host-":"")+"cs"+kind;}
    private static String cookie(HttpServletRequest r,String kind){if(r.getCookies()!=null)for(Cookie c:r.getCookies())if(cookieName(kind).equals(c.getName()))return c.getValue();return "";}
    private static void cookie(HttpServletResponse p,String kind,String value,int age){p.addHeader("Set-Cookie",cookieName(kind)+"="+value+"; Path=/; HttpOnly; SameSite=Strict; Max-Age="+age+(cloud()?"; Secure":""));}
    private static String sign(String payload,FileStore.Data d)throws Exception{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(d.meta.get("signingKey").getBytes("UTF-8"),"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(m.doFinal(payload.getBytes("UTF-8")));}
    private static boolean constant(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),b.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static Map<String,String> identity(HttpServletRequest r,FileStore.Data d){try{String token=cookie(r,"Auth");if(token.length()>300)return null;String[] x=token.split("\\.");if(x.length!=4)return null;String payload=x[0]+"."+x[1]+"."+x[2];if(!constant(sign(payload,d),x[3])||Long.parseLong(x[1])<System.currentTimeMillis()/1000)return null;Map<String,String> u=d.find("users",x[0]);return eq(u.get("active"),"1")&&eq(u.get("version"),x[2])?u:null;}catch(Exception e){return null;}}
    private static void authenticate(HttpServletResponse p,Map<String,String> u,FileStore.Data d)throws Exception{String payload=u.get("id")+"."+(System.currentTimeMillis()/1000+28800)+"."+u.get("version");cookie(p,"Auth",payload+"."+sign(payload,d),28800);}
    private static void audit(FileStore.Data d,String username,String action,String detail){d.add("activity_log",row("username",username,"action",action,"detail",detail,"created_at",now()));while(d.rows("activity_log").size()>200)d.rows("activity_log").remove(0);}
    private static void password(Map<String,String> u,String password)throws Exception{if(password.length()<12||password.length()>100||"Demo@2026".equals(password))throw new IllegalArgumentException("Use a password with 12-100 characters.");byte[] b=new byte[24];new java.security.SecureRandom().nextBytes(b);String salt=Base64.getEncoder().encodeToString(b);u.put("salt",salt);u.put("password_hash",Security.hash(password,salt));u.put("version",Security.token());}
    private static Map<String,String> user(FileStore.Data d,String username,String pwd,String role,String linked,boolean active)throws Exception{
        if(!username.matches("[A-Za-z0-9_.-]{3,40}"))throw new IllegalArgumentException("Use 3-40 letters, numbers, dots, underscores or hyphens for the username.");
        for(Map<String,String> u:d.rows("users"))if(u.get("username").equalsIgnoreCase(username))throw new IllegalArgumentException("That username is already taken.");
        if(!Arrays.asList("admin","teacher","student").contains(role))throw new IllegalArgumentException("Choose a valid account type.");
        if(!role.equals("admin"))d.find(role.equals("teacher")?"faculty":"classes",linked);
        Map<String,String> u=row("username",username,"role",role,"active",active?"1":"0","faculty_id",role.equals("teacher")?linked:"","class_id",role.equals("student")?linked:"");password(u,pwd);return d.add("users",u);
    }
    private static List<Map<String,String>> entries(FileStore.Data d,Map<String,String> u,String id){List<Map<String,String>> out=new ArrayList<Map<String,String>>();String link=eq(u.get("role"),"teacher")?"faculty_id":"class_id";for(Map<String,String> e:d.rows("timetable_entries"))if(eq(e.get("timetable_id"),id)&&(Portal.admin(u)||eq(e.get(link),u.get(link))))out.add(e);return out;}
    private static Map<String,String> run(FileStore.Data d,Map<String,String> u,String id){Map<String,String> run=d.find("saved_timetables",id);if(!Portal.admin(u)&&(!eq(run.get("status"),"published")||entries(d,u,id).isEmpty()))throw new SecurityException("This timetable is not available to your account.");return run;}
    private static List<Map<String,String>> loadReport(FileStore.Data d,String table,String key){List<Map<String,String>> rows=new ArrayList<Map<String,String>>();for(Map<String,String> base:d.rows(table)){Map<String,String> a=new LinkedHashMap<String,String>(base);int count=0,hours=0;for(Map<String,String> s:d.rows("subjects"))if(eq(s.get(key),base.get("id"))){count++;hours+=n(s.get("weekly_hours"));}a.put("subject_count",""+count);a.put("allocated",""+hours);a.put("assigned",""+hours);rows.add(a);}return rows;}
    private static void load(HttpServletRequest r,FileStore.Data d,Map<String,String> u,String page){
        r.setAttribute("identity",u);r.setAttribute("page",page);r.setAttribute("settings",d.settings);r.setAttribute("fileStorage",true);r.setAttribute("fileRevision",d.revision==null?"":d.revision);
        List<Map<String,String>> saved=new ArrayList<Map<String,String>>();for(Map<String,String> x:d.rows("saved_timetables"))if(Portal.admin(u)||(eq(x.get("status"),"published")&&!entries(d,u,x.get("id")).isEmpty()))saved.add(x);Collections.reverse(saved);r.setAttribute("saved",saved);
        if(Portal.admin(u)){
            for(String table:AppServlet.FIELDS.keySet())r.setAttribute(table,d.rows(table));
            r.setAttribute("users",d.rows("users"));List<Map<String,String>> activity=new ArrayList<Map<String,String>>(d.rows("activity_log"));Collections.reverse(activity);r.setAttribute("activity",activity.subList(0,Math.min(40,activity.size())));
            r.setAttribute("workload",loadReport(d,"faculty","faculty_id"));r.setAttribute("classload",loadReport(d,"classes","class_id"));r.setAttribute("generationClasses",loadReport(d,"classes","class_id"));
            if(AppServlet.FIELDS.containsKey(page)&&!t(r,"edit").isEmpty())r.setAttribute("editRow",d.find(page,t(r,"edit")));
            if(page.equals("reports")&&!t(r,"id").isEmpty()){Map<String,String> run=run(d,u,t(r,"id"));r.setAttribute("reportRun",run);Map<String,Map<String,String>> rooms=new LinkedHashMap<String,Map<String,String>>();for(Map<String,String> e:entries(d,u,run.get("id"))){Map<String,String> a=rooms.get(e.get("room_id"));if(a==null){a=row("room_id",e.get("room_id"),"room_name",e.get("room_name"),"allocated","0");rooms.put(e.get("room_id"),a);}a.put("allocated",""+(n(a.get("allocated"))+1));}r.setAttribute("roomload",new ArrayList<Map<String,String>>(rooms.values()));}
        }
        if(page.equals("timetable")||page.equals("export")){r.setAttribute("run",run(d,u,t(r,"id")));r.setAttribute("entries",entries(d,u,t(r,"id")));}
    }
    public static void get(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
        String csrf=cookie(r,"Csrf");if(!csrf.matches("[A-Za-z0-9_-]{32}")){csrf=Security.token();cookie(p,"Csrf",csrf,28800);}r.getSession().setAttribute("csrf",csrf);
        try{
            FileStore.Data d=FileStore.load(r);r.setAttribute("fileRevision",d.revision==null?"":d.revision);r.setAttribute("fileStorage",true);
            if(d.rows("users").isEmpty()){r.getRequestDispatcher("/WEB-INF/file-setup.jsp").forward(r,p);return;}
            Map<String,String> u=identity(r,d);if(u==null){r.getSession().removeAttribute("user");r.setAttribute("loginRole",t(r,"loginRole"));if(t(r,"page").equals("register")){r.setAttribute("registrationClasses",d.rows("classes"));r.setAttribute("registrationTeachers",d.rows("faculty"));r.getRequestDispatcher("/WEB-INF/register.jsp").forward(r,p);}else r.getRequestDispatcher("/WEB-INF/login.jsp").forward(r,p);return;}
            r.getSession().setAttribute("user",u.get("username"));String page=t(r,"page");if(page.isEmpty())page="dashboard";if(!Portal.allowed(u,page)){p.sendError(403);return;}load(r,d,u,page);
            if(page.equals("export")){export(p,entries(d,u,t(r,"id")),t(r,"id"));return;}r.getRequestDispatcher("/WEB-INF/app.jsp").forward(r,p);
        }catch(SecurityException e){p.sendError(403,e.getMessage());}catch(IllegalArgumentException e){p.sendError(400,e.getMessage());}catch(IOException e){p.sendError(503,"Shared file storage is temporarily unavailable. No records have been changed.");}
    }
    public static void post(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
        r.setCharacterEncoding("UTF-8");p.setHeader("Cache-Control","no-store");String csrf=cookie(r,"Csrf");if(csrf.isEmpty()||!constant(csrf,t(r,"csrf"))){p.sendError(403,"Reload the page and try again.");return;}
        String a=t(r,"action"),destination="?page=dashboard";
        if(Arrays.asList("login","register","setup").contains(a)&&!AuthThrottle.allow(a.equals("setup")?"register":a,r.getRemoteAddr(),System.currentTimeMillis())){p.sendError(429,"Too many attempts. Try again later.");return;}
        try{
            FileStore.Data d=FileStore.load(r);Map<String,String> u=identity(r,d);
            if(a.equals("logout")){if(u!=null){u.put("version",Security.token());FileStore.save(r,d);}cookie(p,"Auth","",0);r.getSession().invalidate();p.sendRedirect(r.getContextPath()+"/app");return;}
            if(a.equals("setup")){
                String key=System.getenv("TIMETABLE_SETUP_KEY");if(!d.rows("users").isEmpty()||key==null||key.length()<32||!constant(key,t(r,"setup_key")))throw new SecurityException("Owner setup is unavailable or the setup key is incorrect.");
                if(!t(r,"password").equals(t(r,"confirm_password")))throw new IllegalArgumentException("Passwords do not match.");u=user(d,t(r,"username"),t(r,"password"),"admin","",true);audit(d,u.get("username"),"setup","Created owner account");FileStore.save(r,d);authenticate(p,u,d);
            }else if(a.equals("login")){
                destination="?loginRole="+safeRole(t(r,"loginRole"));Map<String,String> found=null;
                if(t(r,"password").length()<=100&&t(r,"username").length()<=40)for(Map<String,String> x:d.rows("users"))if(eq(x.get("username"),t(r,"username"))&&eq(x.get("role"),t(r,"loginRole"))&&eq(x.get("active"),"1")&&Security.verify(t(r,"password"),x.get("salt"),x.get("password_hash")))found=x;
                if(found==null)throw new IllegalArgumentException("Sign-in failed. Check your account type, password and administrator approval.");authenticate(p,found,d);destination="?page=dashboard";
            }else if(a.equals("register")){
                destination="?page=register";if(u!=null)throw new SecurityException("Sign out before registering.");String role=t(r,"role");if(!Arrays.asList("teacher","student").contains(role))throw new IllegalArgumentException("Only teachers and students can register.");if(!t(r,"password").equals(t(r,"confirm_password")))throw new IllegalArgumentException("Passwords do not match.");user(d,t(r,"username"),t(r,"password"),role,t(r,role.equals("teacher")?"faculty_id":"class_id"),false);audit(d,t(r,"username"),a,"Requested access; awaiting approval");FileStore.save(r,d);r.getSession().setAttribute("message","Account request saved. Ask your administrator to approve access.");destination="?loginRole="+role;
            }else{
                if(u==null){p.sendError(401);return;}if(!Portal.admin(u)&&!a.equals("account"))throw new SecurityException("Administrator access is required.");
                if(!constant(d.revision,t(r,"revision")))throw new ConcurrentModificationException("Another user saved changes. Reload this page before saving again.");
                destination=action(r,d,u,a);audit(d,u.get("username"),a,"Saved "+a+" changes");FileStore.save(r,d);if(a.equals("account"))authenticate(p,u,d);r.getSession().setAttribute("message","Changes saved to shared file storage.");r.getSession().setAttribute("error",false);
            }
        }catch(SecurityException e){p.sendError(403,e.getMessage());return;}catch(ConcurrentModificationException e){p.sendError(409,e.getMessage());return;}catch(IllegalArgumentException e){r.getSession().setAttribute("message",e.getMessage());r.getSession().setAttribute("error",true);if(!Arrays.asList("login","register","setup").contains(a)){p.setStatus(400);p.setContentType("text/html;charset=UTF-8");p.getWriter().write("<!doctype html><meta name=viewport content='width=device-width'><h1>Change not saved</h1><p>"+Security.html(e.getMessage())+"</p><a href='app'>Return to workspace</a>");return;}}catch(Exception e){r.getServletContext().log("Shared file operation failed: "+e.getClass().getSimpleName());p.sendError(503,"Shared storage is unavailable. The change could not be confirmed. Reload before retrying.");return;}
        p.sendRedirect(r.getContextPath()+"/app"+destination);
    }
    private static String safeRole(String role){return Arrays.asList("teacher","student","admin").contains(role)?role:"";}
    private static String action(HttpServletRequest r,FileStore.Data d,Map<String,String> u,String a)throws Exception{
        String table=t(r,"entity");
        if(a.equals("save")){saveRecord(r,d,table);return "?page="+table;}
        if(a.equals("delete")){deleteRecord(d,table,t(r,"id"));return "?page="+table;}
        if(a.equals("generate"))return "?page=timetable&id="+generate(r,d);
        if(a.equals("settings")){
            String institution=t(r,"institution"),year=t(r,"academic_year"),time=t(r,"start_time");if(institution.isEmpty()||institution.length()>80||year.isEmpty()||year.length()>30)throw new IllegalArgumentException("Enter the institution and academic year.");
            int days=Portal.number(t(r,"days"),1,6),periods=Portal.number(t(r,"periods"),1,8),minutes=Portal.number(t(r,"period_minutes"),20,120),after=Portal.number(t(r,"break_after"),0,8),pause=Portal.number(t(r,"break_minutes"),0,120);Portal.checkTimings(time,minutes,periods,after,pause);
            d.settings.putAll(row("institution",institution,"academic_year",year,"start_time",time,"days",""+days,"periods",""+periods,"period_minutes",""+minutes,"break_after",""+after,"break_minutes",""+pause));return "?page=settings";
        }
        if(a.equals("availability")){Map<String,String> f=d.find("faculty",t(r,"faculty_id"));Set<String> slots=new TreeSet<String>();String[] values=r.getParameterValues("blocked");if(values!=null)for(String x:values){if(!x.matches("[1-6]:[1-8]"))throw new IllegalArgumentException("Invalid availability period.");slots.add(x);}f.put("unavailable",String.join(",",slots));return "?page=availability&teacher="+f.get("id");}
        if(a.equals("account")){if(!Security.verify(t(r,"current_password"),u.get("salt"),u.get("password_hash")))throw new IllegalArgumentException("Current password is incorrect.");if(!eq(t(r,"new_password"),t(r,"confirm_password")))throw new IllegalArgumentException("Passwords do not match.");password(u,t(r,"new_password"));return "?page=account";}
        if(a.equals("createUser")){String role=t(r,"role");user(d,t(r,"username"),t(r,"password"),role,t(r,role.equals("teacher")?"faculty_id":"class_id"),true);return "?page=users";}
        if(a.equals("userStatus")||a.equals("resetPassword")){Map<String,String> target=d.find("users",t(r,"id"));if(eq(target.get("role"),"admin")||eq(target.get("id"),u.get("id")))throw new IllegalArgumentException("Use My account to change your own password; other administrators cannot be changed here.");if(a.equals("resetPassword"))password(target,t(r,"password"));else{target.put("active",eq(target.get("active"),"1")?"0":"1");target.put("version",Security.token());}return "?page=users";}
        if(!Arrays.asList("renameRun","cloneRun","publishRun","deleteRun","moveLesson").contains(a))throw new IllegalArgumentException("Unknown action.");
        Map<String,String> run=run(d,u,t(r,"id"));String id=run.get("id");
        if(a.equals("renameRun")){String name=t(r,"name");if(name.isEmpty()||name.length()>80)throw new IllegalArgumentException("Use a timetable name of 1-80 characters.");run.put("name",name);}
        if(a.equals("cloneRun")){Map<String,String> copy=new LinkedHashMap<String,String>(run);String name=run.get("name");copy.put("name",name.substring(0,Math.min(name.length(),70))+" (copy)");copy.put("status","draft");copy.put("created_at",now());copy=d.add("saved_timetables",copy);for(Map<String,String> e:new ArrayList<Map<String,String>>(entries(d,u,id))){Map<String,String> x=new LinkedHashMap<String,String>(e);x.put("timetable_id",copy.get("id"));d.add("timetable_entries",x);}id=copy.get("id");}
        if(a.equals("publishRun"))run.put("status",eq(run.get("status"),"published")?"draft":"published");
        if(a.equals("deleteRun")){if(eq(run.get("status"),"published"))throw new IllegalArgumentException("Unpublish before deleting.");final String deleted=id;d.rows("timetable_entries").removeIf(e->eq(e.get("timetable_id"),deleted));d.rows("saved_timetables").remove(run);return "?page=saved";}
        if(a.equals("moveLesson"))move(r,d,run);
        return "?page=timetable&id="+id;
    }
    private static void saveRecord(HttpServletRequest r,FileStore.Data d,String table){
        String[] fields=AppServlet.FIELDS.get(table);if(fields==null)throw new IllegalArgumentException("Unknown record type.");Map<String,String> value=new LinkedHashMap<String,String>();
        for(String f:fields){String v=t(r,f);if(f.equals("unavailable")){v=v.replace(" ","");if(!v.isEmpty()&&!v.matches("[1-6]:[1-8](,[1-6]:[1-8])*"))throw new IllegalArgumentException("Use day:period values such as 1:1,3:4.");}else if(f.endsWith("_id"))d.find(f.equals("class_id")?"classes":f.equals("faculty_id")?"faculty":"departments",v);else if(f.equals("weekly_hours")||f.equals("max_hours"))Portal.number(v,1,48);else if(f.equals("student_count")||f.equals("capacity"))Portal.number(v,1,500);else if(f.equals("type")){if(!Arrays.asList("Theory","Lab").contains(v))throw new IllegalArgumentException("Choose Theory or Lab.");}else if(v.isEmpty()||v.length()>80)throw new IllegalArgumentException("Use a name of 1-80 characters.");value.put(f,v);}
        for(Map<String,String> x:d.rows(table))if(!eq(x.get("id"),t(r,"id"))&&x.get("name").equalsIgnoreCase(value.get("name"))&&(!table.equals("subjects")||eq(x.get("class_id"),value.get("class_id"))))throw new IllegalArgumentException("This name already exists.");
        if(t(r,"id").isEmpty())d.add(table,value);else d.find(table,t(r,"id")).putAll(value);
    }
    private static void deleteRecord(FileStore.Data d,String table,String id){if(!AppServlet.FIELDS.containsKey(table))throw new IllegalArgumentException("Unknown record type.");Map<String,String> target=d.find(table,id);String linked=table.equals("faculty")?"faculty_id":table.equals("classes")?"class_id":table.equals("departments")?"department_id":"";if(!linked.isEmpty())for(String t:Arrays.asList("classes","subjects","users"))for(Map<String,String> x:d.rows(t))if(eq(x.get(linked),id))throw new IllegalArgumentException("This record is linked to another record or account. Remove the link first.");d.rows(table).remove(target);}
    private static String generate(HttpServletRequest r,FileStore.Data d){
        String name=t(r,"name"),group=t(r,"class_id");if(name.isEmpty()||name.length()>80)throw new IllegalArgumentException("Use a timetable name of 1-80 characters.");int days=Portal.number(t(r,"days"),1,6),periods=Portal.number(t(r,"periods"),1,8);boolean full=!eq(t(r,"coverage"),"allow_free");
        Portal.checkTimings(d.settings.get("start_time"),n(d.settings.get("period_minutes")),periods,n(d.settings.get("break_after")),n(d.settings.get("break_minutes")));
        List<Scheduler.Lesson> lessons=new ArrayList<Scheduler.Lesson>();List<Scheduler.Room> rooms=new ArrayList<Scheduler.Room>();Map<Integer,Integer> loads=new HashMap<Integer,Integer>();int groups=0;
        for(Map<String,String> c:d.rows("classes"))if(group.equals("0")||eq(c.get("id"),group)){groups++;int hours=0;for(Map<String,String> s:d.rows("subjects"))if(eq(s.get("class_id"),c.get("id"))){Map<String,String> f=d.find("faculty",s.get("faculty_id"));int h=n(s.get("weekly_hours")),teacher=n(f.get("id"));hours+=h;loads.put(teacher,loads.getOrDefault(teacher,0)+h);if(loads.get(teacher)>n(f.get("max_hours")))throw new IllegalArgumentException(f.get("name")+" exceeds the weekly teaching limit.");lessons.add(new Scheduler.Lesson(n(s.get("id")),n(c.get("id")),teacher,h,n(c.get("student_count")),s.get("name"),s.get("type"),f.get("unavailable")));}if(hours==0||hours>days*periods||(full&&hours!=days*periods))throw new IllegalArgumentException(c.get("name")+" has "+hours+" assigned periods for "+(days*periods)+" slots. Adjust subject periods before generating.");}
        if(groups==0)throw new IllegalArgumentException("Select an existing class.");for(Map<String,String> room:d.rows("classrooms"))rooms.add(new Scheduler.Room(n(room.get("id")),n(room.get("capacity")),room.get("type")));
        List<Scheduler.Entry> result=new Scheduler(days,periods,lessons,rooms,full).generate();Map<String,String> run=new LinkedHashMap<String,String>(d.settings);run.putAll(row("name",name,"days",""+days,"periods",""+periods,"lesson_count",""+result.size(),"status","draft","created_at",now()));run=d.add("saved_timetables",run);
        for(Scheduler.Entry e:result)d.add("timetable_entries",row("timetable_id",run.get("id"),"class_id",""+e.lesson.group,"faculty_id",""+e.lesson.teacher,"subject_id",""+e.lesson.subject,"room_id",""+e.room,"class_name",d.find("classes",""+e.lesson.group).get("name"),"faculty_name",d.find("faculty",""+e.lesson.teacher).get("name"),"subject_name",e.lesson.name,"room_name",d.find("classrooms",""+e.room).get("name"),"day_no",""+(e.day+1),"period_no",""+(e.period+1)));
        return run.get("id");
    }
    private static void move(HttpServletRequest r,FileStore.Data d,Map<String,String> run){
        if(eq(run.get("status"),"published"))throw new IllegalArgumentException("Unpublish or duplicate before editing.");Map<String,String> e=d.find("timetable_entries",t(r,"entry_id"));if(!eq(e.get("timetable_id"),run.get("id")))throw new IllegalArgumentException("Lesson does not belong to this timetable.");int day=Portal.number(t(r,"day_no"),1,n(run.get("days"))),period=Portal.number(t(r,"period_no"),1,n(run.get("periods")));Map<String,String> room=d.find("classrooms",t(r,"room_id")),subject=d.find("subjects",e.get("subject_id")),group=d.find("classes",e.get("class_id")),teacher=d.find("faculty",e.get("faculty_id"));
        if(!eq(subject.get("class_id"),e.get("class_id"))||!eq(subject.get("faculty_id"),e.get("faculty_id")))throw new IllegalArgumentException("Source assignments changed. Generate a new timetable.");
        if(n(room.get("capacity"))<n(group.get("student_count"))||(eq(subject.get("type"),"Lab")&&!eq(room.get("type"),"Lab")))throw new IllegalArgumentException("Choose a suitable room with enough seats.");if(Arrays.asList(teacher.get("unavailable").split(",")).contains(day+":"+period))throw new IllegalArgumentException("Teacher unavailable in this period.");
        for(Map<String,String> other:d.rows("timetable_entries"))if(other!=e&&eq(other.get("timetable_id"),run.get("id"))&&eq(other.get("day_no"),""+day)&&eq(other.get("period_no"),""+period)&&(eq(other.get("class_id"),e.get("class_id"))||eq(other.get("faculty_id"),e.get("faculty_id"))||eq(other.get("room_id"),room.get("id"))))throw new IllegalArgumentException("This slot conflicts with another class, teacher or room.");
        e.putAll(row("day_no",""+day,"period_no",""+period,"room_id",room.get("id"),"room_name",room.get("name")));
    }
    private static String csv(String s){if(s==null)s="";if(s.matches("^[=+@\\-].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
    private static void export(HttpServletResponse p,List<Map<String,String>> rows,String id)throws IOException{p.setContentType("text/csv;charset=UTF-8");p.setHeader("Content-Disposition","attachment; filename=Timetable-"+n(id)+".csv");PrintWriter w=p.getWriter();w.println("Class,Day,Period,Subject,Teacher,Room");String[] days={"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};for(Map<String,String> e:rows)w.println(csv(e.get("class_name"))+","+days[n(e.get("day_no"))-1]+","+e.get("period_no")+","+csv(e.get("subject_name"))+","+csv(e.get("faculty_name"))+","+csv(e.get("room_name")));}
}
