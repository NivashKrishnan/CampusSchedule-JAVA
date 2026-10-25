package app;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public final class AppServlet extends HttpServlet {
    public static final Map<String,String[]> FIELDS=new LinkedHashMap<String,String[]>();
    static {
        FIELDS.put("departments",new String[]{"name"});
        FIELDS.put("faculty",new String[]{"name","max_hours","unavailable"});
        FIELDS.put("classes",new String[]{"name","department_id","student_count"});
        FIELDS.put("classrooms",new String[]{"name","capacity","type"});
        FIELDS.put("subjects",new String[]{"name","class_id","faculty_id","weekly_hours","type"});
    }
    public static String label(String s){
        if("max_hours".equals(s))return "Weekly period limit";
        if("weekly_hours".equals(s))return "Weekly periods";
        if("unavailable".equals(s))return "Unavailable periods";
        if("department_id".equals(s))return "Department";
        if("class_id".equals(s))return "Class";
        if("faculty_id".equals(s))return "Teacher";
        String x=s.replace('_',' ');return Character.toUpperCase(x.charAt(0))+x.substring(1);
    }
    private static String text(HttpServletRequest r,String k){String s=r.getParameter(k);return s==null?"":s.trim();}
    private static boolean loginRole(String role){return Arrays.asList("admin","teacher","student").contains(role);}
    private static int num(String s){try{return Integer.parseInt(s);}catch(Exception e){throw new IllegalArgumentException("Enter a valid whole number.");}}
    private static int range(String s,int low,int high,String label){int x=num(s);if(x<low||x>high)throw new IllegalArgumentException(label+" must be between "+low+" and "+high+".");return x;}
    private static void flash(HttpServletRequest r,String message,boolean error){r.getSession().setAttribute("message",message);r.getSession().setAttribute("error",error);}
    private static void redirect(HttpServletRequest r,HttpServletResponse p,String query)throws IOException{p.sendRedirect(r.getContextPath()+"/app"+query);}
    protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
        p.setCharacterEncoding("UTF-8");p.setHeader("Cache-Control","no-store");p.setHeader("X-Content-Type-Options","nosniff");
        p.setHeader("X-Frame-Options","DENY");p.setHeader("Referrer-Policy","same-origin");
        HttpSession session=r.getSession();if(session.getAttribute("csrf")==null)session.setAttribute("csrf",Security.token());
        if(session.getAttribute("user")==null){
            String role=text(r,"loginRole");
            r.setAttribute("loginRole",loginRole(role)?role:"");
            if("register".equals(text(r,"page"))){
                try(Connection c=Db.open()){
                    r.setAttribute("registrationClasses",Db.query(c,"SELECT id,name FROM classes ORDER BY name"));
                    r.setAttribute("registrationTeachers",Db.query(c,"SELECT id,name FROM faculty ORDER BY name"));
                }catch(SQLException e){getServletContext().log("Registration unavailable",e);r.setAttribute("registrationUnavailable",true);}
                r.getRequestDispatcher("/WEB-INF/register.jsp").forward(r,p);return;
            }
            r.getRequestDispatcher("/WEB-INF/login.jsp").forward(r,p);return;
        }
        String page=text(r,"page");if(page.isEmpty())page="dashboard";
        r.setAttribute("page",page);
        try(Connection c=Db.open()){
            Map<String,String> identity=Portal.identity(c,r);
            if(identity==null){redirect(r,p,"");return;}
            if(!Portal.allowed(identity,page)){p.sendError(403,"This page requires administrator access.");return;}
            Portal.load(c,r,identity,page);
            if("timetable".equals(page)||"export".equals(page)){
                int id=num(text(r,"id"));
                Map<String,String> run=Portal.run(c,identity,id,false);
                List<Map<String,String>> entries=Portal.entries(c,identity,id);
                r.setAttribute("run",run);r.setAttribute("entries",entries);
                if("export".equals(page)){export(p,entries,id);return;}
            }
            if(FIELDS.containsKey(page)&&!text(r,"edit").isEmpty()){
                List<Map<String,String>> rows=Db.query(c,"SELECT * FROM "+page+" WHERE id=?",num(text(r,"edit")));
                if(!rows.isEmpty())r.setAttribute("editRow",rows.get(0));
            }
        }catch(SecurityException e){p.sendError(403,e.getMessage());return;}
        catch(Exception e){getServletContext().log("Page load failed",e);r.setAttribute("loadError",e instanceof IllegalArgumentException?e.getMessage():"Database unavailable. Start the database and check the setup instructions.");}
        r.getRequestDispatcher("/WEB-INF/app.jsp").forward(r,p);
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException{
        r.setCharacterEncoding("UTF-8");HttpSession session=r.getSession();
        if(!text(r,"csrf").equals(session.getAttribute("csrf"))){p.sendError(403,"Session expired. Reload the page and try again.");return;}
        String action=text(r,"action");
        if(Arrays.asList("login","register").contains(action)&&!AuthThrottle.allow(action,r.getRemoteAddr(),System.currentTimeMillis())){
            p.setHeader("Retry-After","3600");p.sendError(429,"Too many attempts. Please try again later.");return;
        }
        if("logout".equals(action)){session.invalidate();redirect(r,p,"");return;}
        if("register".equals(action)){
            if(session.getAttribute("user")!=null){p.sendError(403);return;}
            String role=text(r,"role");
            try(Connection c=Db.open()){
                c.setAutoCommit(false);
                try{
                    int linked=range(text(r,"teacher".equals(role)?"faculty_id":"class_id"),1,Integer.MAX_VALUE,"Linked record");
                    Registration.create(c,text(r,"username"),text(r,"password"),text(r,"confirm_password"),role,linked);
                    c.commit();session.setAttribute("csrf",Security.token());
                }catch(Exception e){c.rollback();throw e;}
                flash(r,"Account request saved. Your administrator must review and enable it before you can sign in.",false);
                redirect(r,p,"?loginRole="+role);return;
            }catch(IllegalArgumentException e){flash(r,e.getMessage(),true);}
            catch(SQLException e){flash(r,"23000".equals(e.getSQLState())?"That username is unavailable. Choose another username.":"Registration is temporarily unavailable. Please try again later.",true);}
            catch(Exception e){getServletContext().log("Registration failed",e);flash(r,"Registration could not be completed.",true);}
            redirect(r,p,"?page=register");return;
        }
        if("login".equals(action)){
            String role=text(r,"loginRole");
            if(!loginRole(role)){
                flash(r,"Choose Admin, Teacher or Student to sign in.",true);redirect(r,p,"");return;
            }
            boolean authenticated=false;
            if(text(r,"password").length()>100||text(r,"username").length()>80){flash(r,"Sign-in failed. Check your account details.",true);redirect(r,p,"?loginRole="+role);return;}
            try(Connection c=Db.open()){
                Map<String,String> user=Registration.authenticate(c,text(r,"username"),text(r,"password"),role,"true".equalsIgnoreCase(System.getenv("TIMETABLE_PUBLIC_DEPLOYMENT")));
                if(user!=null){
                    r.changeSessionId();session.setAttribute("user",user.get("username"));session.setAttribute("csrf",Security.token());
                    authenticated=true;
                }else flash(r,"Sign-in failed. Check your account type and details. New accounts must first be approved by your administrator.",true);
            }catch(Exception e){getServletContext().log("Login failed",e);flash(r,"Cannot connect to the database. Check that the local database is running.",true);}
            redirect(r,p,authenticated?"":"?loginRole="+role);return;
        }
        if(session.getAttribute("user")==null){p.sendError(401);return;}
        String table=text(r,"entity");String destination=FIELDS.containsKey(table)?"?page="+table:"?page=generate";
        if("account".equals(action))destination="?page=account";
        if("settings".equals(action))destination="?page=settings";
        if("availability".equals(action))destination="?page=availability";
        if(Arrays.asList("createUser","userStatus","resetPassword").contains(action))destination="?page=users";
        if(Arrays.asList("renameRun","cloneRun","publishRun","deleteRun","moveLesson").contains(action))destination=text(r,"id").matches("[0-9]{1,9}")?"?page=timetable&id="+text(r,"id"):"?page=saved";
        try(Connection c=Db.open()){
            Map<String,String> identity=Portal.identity(c,r);
            if(identity==null){p.sendError(401);return;}
            if(!Portal.admin(identity)&&!"account".equals(action)){p.sendError(403);return;}
            String extra=Portal.action(c,r,identity,action);
            if(extra!=null){flash(r,"Changes saved successfully.",false);redirect(r,p,extra);return;}
            if("save".equals(action)){
                if(!FIELDS.containsKey(table))throw new IllegalArgumentException("Unknown record type.");
                String[] fields=FIELDS.get(table);List<Object> values=new ArrayList<Object>();
                for(String f:fields){
                    String v=text(r,f);
                    if("unavailable".equals(f)){
                        v=v.replace(" ","");
                        if(!v.isEmpty()&&!v.matches("[1-6]:[1-8](,[1-6]:[1-8])*"))throw new IllegalArgumentException("Use unavailable periods such as 1:1,3:4 (day:period).");
                    }else if(f.endsWith("_id"))range(v,1,Integer.MAX_VALUE,label(f));
                    else if("weekly_hours".equals(f)||"max_hours".equals(f))range(v,1,48,label(f));
                    else if("student_count".equals(f)||"capacity".equals(f))range(v,1,500,label(f));
                    else if("type".equals(f)){if(!Arrays.asList("Theory","Lab").contains(v))throw new IllegalArgumentException("Select Theory or Lab.");}
                    else if(v.isEmpty()||v.length()>80)throw new IllegalArgumentException("Names must contain 1 to 80 characters.");
                    values.add(v);
                }
                if(text(r,"id").isEmpty()){
                    String marks=String.join(",",Collections.nCopies(fields.length,"?"));
                    Db.update(c,"INSERT INTO "+table+" ("+String.join(",",fields)+") VALUES ("+marks+")",values.toArray());
                }else{
                    List<String> sets=new ArrayList<String>();for(String f:fields)sets.add(f+"=?");values.add(num(text(r,"id")));
                    if(Db.update(c,"UPDATE "+table+" SET "+String.join(",",sets)+" WHERE id=?",values.toArray())==0)throw new IllegalArgumentException("The record no longer exists or no values changed.");
                }
                flash(r,"Record saved to the database.",false);
            }else if("delete".equals(action)){
                if(!FIELDS.containsKey(table))throw new IllegalArgumentException("Unknown record type.");
                if("faculty".equals(table)||"classes".equals(table)){
                    String column="faculty".equals(table)?"faculty_id":"class_id";
                    if(!Db.query(c,"SELECT id FROM users WHERE "+column+"=?",num(text(r,"id"))).isEmpty())throw new IllegalArgumentException("This record is linked to a login account and cannot be removed.");
                }
                Db.update(c,"DELETE FROM "+table+" WHERE id=?",num(text(r,"id")));flash(r,"Record removed. Previously saved timetables are unchanged.",false);
            }else if("generate".equals(action)){
                int days=range(text(r,"days"),1,6,"Days"),periods=range(text(r,"periods"),1,8,"Periods");
                String name=text(r,"name");if(name.isEmpty()||name.length()>80)throw new IllegalArgumentException("Enter a timetable name of 1 to 80 characters.");
                int classId=range(text(r,"class_id"),0,Integer.MAX_VALUE,"Class");
                String coverage=text(r,"coverage");
                if(!coverage.isEmpty()&&!Arrays.asList("full","allow_free").contains(coverage))throw new IllegalArgumentException("Select a valid week coverage option.");
                int id=generate(c,name,days,periods,classId,!"allow_free".equals(coverage));destination="?page=timetable&id="+id;
                flash(r,"Timetable generated, checked and saved to the database.",false);
            }else throw new IllegalArgumentException("Unknown action.");
            Portal.audit(c,identity,action,FIELDS.containsKey(table)?table+" record "+text(r,"id"):"Generated "+text(r,"name"));
        }catch(IllegalArgumentException e){flash(r,e.getMessage(),true);}
        catch(SecurityException e){p.sendError(403,e.getMessage());return;}
        catch(SQLException e){getServletContext().log("Database action failed",e);flash(r,e.getSQLState()!=null&&e.getSQLState().startsWith("23")?"This change conflicts with an existing or linked record. Check duplicate names and linked subjects/classes.":"The database operation failed. No timetable was saved. Check the server log.",true);}
        catch(Exception e){getServletContext().log("Action failed",e);flash(r,"The operation could not be completed.",true);}
        redirect(r,p,destination);
    }
    private int generate(Connection c,String name,int days,int periods,int classId,boolean fullWeek)throws SQLException{
        c.setAutoCommit(false);
        try{
            Map<String,String> settings=Portal.one(c,"SELECT * FROM app_settings WHERE id=1");
            Portal.checkTimings(settings.get("start_time"),num(settings.get("period_minutes")),periods,num(settings.get("break_after")),num(settings.get("break_minutes")));
            List<Map<String,String>> groups=Db.query(c,"SELECT c.id,c.name,COALESCE(SUM(s.weekly_hours),0) assigned FROM classes c LEFT JOIN subjects s ON s.class_id=c.id WHERE (?=0 OR c.id=?) GROUP BY c.id,c.name",classId,classId);
            if(groups.isEmpty())throw new IllegalArgumentException("Select an existing class.");
            for(Map<String,String> group:groups){
                int assigned=num(group.get("assigned")),available=days*periods;
                if(assigned==0)throw new IllegalArgumentException(group.get("name")+" has no assigned subjects. Add subjects before generating.");
                if(assigned>available)throw new IllegalArgumentException(group.get("name")+" has "+assigned+" assigned periods, but this week has only "+available+". Increase the days or periods.");
                if(fullWeek&&assigned<available)throw new IllegalArgumentException(group.get("name")+" has "+assigned+" assigned periods for "+available+" slots. Add "+(available-assigned)+" subject or activity periods, or choose a week matching the assigned total. No timetable was saved.");
            }
            List<Map<String,String>> rows=Db.query(c,"SELECT s.*,c.name class_name,c.student_count,f.name faculty_name,f.max_hours,f.unavailable FROM subjects s JOIN classes c ON s.class_id=c.id JOIN faculty f ON s.faculty_id=f.id WHERE (?=0 OR c.id=?) ORDER BY s.id",classId,classId);
            List<Scheduler.Lesson> lessons=new ArrayList<Scheduler.Lesson>();Map<Integer,Map<String,String>> info=new HashMap<Integer,Map<String,String>>();
            Map<Integer,Integer> loads=new HashMap<Integer,Integer>();
            for(Map<String,String> a:rows){
                int id=num(a.get("id")),teacher=num(a.get("faculty_id")),h=num(a.get("weekly_hours"));
                loads.put(teacher,loads.getOrDefault(teacher,0)+h);
                if(loads.get(teacher)>num(a.get("max_hours")))throw new IllegalArgumentException(a.get("faculty_name")+" exceeds the configured weekly teaching limit.");
                lessons.add(new Scheduler.Lesson(id,num(a.get("class_id")),teacher,h,num(a.get("student_count")),a.get("name"),a.get("type"),a.get("unavailable")));info.put(id,a);
            }
            List<Scheduler.Room> rooms=new ArrayList<Scheduler.Room>();Map<Integer,String> roomNames=new HashMap<Integer,String>();
            for(Map<String,String>a:Db.query(c,"SELECT * FROM classrooms ORDER BY capacity,id")){int id=num(a.get("id"));rooms.add(new Scheduler.Room(id,num(a.get("capacity")),a.get("type")));roomNames.put(id,a.get("name"));}
            List<Scheduler.Entry> entries=new Scheduler(days,periods,lessons,rooms,fullWeek).generate();
            int id=Db.insert(c,"INSERT INTO saved_timetables (name,days,periods,lesson_count) VALUES (?,?,?,?)",name,days,periods,entries.size());
            Portal.snapshotSettings(c,id);
            for(Scheduler.Entry e:entries){Map<String,String>a=info.get(e.lesson.subject);
                Db.update(c,"INSERT INTO timetable_entries (timetable_id,class_id,faculty_id,room_id,subject_id,class_name,faculty_name,room_name,subject_name,day_no,period_no) VALUES (?,?,?,?,?,?,?,?,?,?,?)",id,e.lesson.group,e.lesson.teacher,e.room,e.lesson.subject,a.get("class_name"),a.get("faculty_name"),roomNames.get(e.room),e.lesson.name,e.day+1,e.period+1);
            }
            c.commit();return id;
        }catch(SQLException|RuntimeException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
    }
    private static String csv(String s){if(s==null)s="";if(s.matches("^[=+@\\-].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
    private void export(HttpServletResponse p,List<Map<String,String>> rows,int id)throws IOException{
        p.setContentType("text/csv;charset=UTF-8");p.setHeader("Content-Disposition","attachment; filename=Timetable-"+id+".csv");
        PrintWriter w=p.getWriter();w.println("Class,Day,Period,Subject,Teacher,Room");
        String[] days={"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        for(Map<String,String>a:rows)w.println(csv(a.get("class_name"))+","+days[num(a.get("day_no"))-1]+","+a.get("period_no")+","+csv(a.get("subject_name"))+","+csv(a.get("faculty_name"))+","+csv(a.get("room_name")));
    }
}
