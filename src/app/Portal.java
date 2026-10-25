package app;

import java.sql.*;
import java.util.*;
import java.security.SecureRandom;
import javax.servlet.http.*;

/** Role checks and transactional management of the scheduling workspace. */
public final class Portal {
    public static String text(HttpServletRequest r,String key){String s=r.getParameter(key);return s==null?"":s.trim();}
    public static int number(String s,int min,int max){try{int n=Integer.parseInt(s);if(n>=min&&n<=max)return n;}catch(Exception e){}throw new IllegalArgumentException("Enter a whole number between "+min+" and "+max+".");}
    public static Map<String,String> one(Connection c,String sql,Object...args)throws SQLException{
        List<Map<String,String>> list=Db.query(c,sql,args);if(list.isEmpty())throw new IllegalArgumentException("The requested record no longer exists.");return list.get(0);
    }
    public static boolean admin(Map<String,String> u){return "admin".equals(u.get("role"));}
    public static Map<String,String> identity(Connection c,HttpServletRequest r)throws SQLException{
        Object name=r.getSession().getAttribute("user");if(name==null)return null;
        List<Map<String,String>> list=Db.query(c,"SELECT id,username,role,faculty_id,class_id,active FROM users WHERE username=? AND active=1",name);
        if(list.isEmpty()){r.getSession().removeAttribute("user");return null;}
        Map<String,String> u=list.get(0);r.setAttribute("identity",u);return u;
    }
    public static void audit(Connection c,Map<String,String> u,String action,String detail)throws SQLException{
        Db.update(c,"INSERT INTO activity_log(username,action,detail) VALUES(?,?,?)",u.get("username"),action,detail.length()>255?detail.substring(0,255):detail);
    }
    public static boolean allowed(Map<String,String>u,String page){return admin(u)||Arrays.asList("dashboard","saved","timetable","export","account","help").contains(page);}
    public static Map<String,String> run(Connection c,Map<String,String>u,int id,boolean lock)throws SQLException{
        Map<String,String> run=one(c,"SELECT * FROM saved_timetables WHERE id=?"+(lock?" FOR UPDATE":""),id);
        if(!admin(u)){
            String column="teacher".equals(u.get("role"))?"faculty_id":"class_id";
            if(!"published".equals(run.get("status"))||u.get(column)==null||Db.query(c,"SELECT id FROM timetable_entries WHERE timetable_id=? AND "+column+"=? LIMIT 1",id,u.get(column)).isEmpty())throw new SecurityException("This timetable is not available to your account.");
        }return run;
    }
    public static List<Map<String,String>> entries(Connection c,Map<String,String>u,int id)throws SQLException{
        String column="teacher".equals(u.get("role"))?"faculty_id":"class_id";
        return admin(u)?Db.query(c,"SELECT * FROM timetable_entries WHERE timetable_id=? ORDER BY class_name,day_no,period_no",id):Db.query(c,"SELECT * FROM timetable_entries WHERE timetable_id=? AND "+column+"=? ORDER BY day_no,period_no",id,u.get(column));
    }
    public static void load(Connection c,HttpServletRequest r,Map<String,String>u,String page)throws SQLException{
        r.setAttribute("settings",one(c,"SELECT * FROM app_settings WHERE id=1"));
        if(admin(u)){
            if("generate".equals(page))r.setAttribute("generationClasses",Db.query(c,"SELECT c.id,c.name,COALESCE(SUM(s.weekly_hours),0) assigned FROM classes c LEFT JOIN subjects s ON s.class_id=c.id GROUP BY c.id,c.name ORDER BY c.id DESC"));
            for(String table:AppServlet.FIELDS.keySet())r.setAttribute(table,Db.query(c,"SELECT * FROM "+table+" ORDER BY id"));
            r.setAttribute("saved",Db.query(c,"SELECT * FROM saved_timetables ORDER BY id DESC"));
            if("users".equals(page))r.setAttribute("users",Db.query(c,"SELECT id,username,role,faculty_id,class_id,active FROM users ORDER BY id"));
            if("activity".equals(page)||"dashboard".equals(page))r.setAttribute("activity",Db.query(c,"SELECT * FROM activity_log ORDER BY id DESC LIMIT 40"));
            if("reports".equals(page)||"dashboard".equals(page)){
                r.setAttribute("workload",Db.query(c,"SELECT f.id,f.name,f.max_hours,COUNT(s.id) subject_count,COALESCE(SUM(s.weekly_hours),0) allocated FROM faculty f LEFT JOIN subjects s ON s.faculty_id=f.id GROUP BY f.id,f.name,f.max_hours ORDER BY f.name"));
                r.setAttribute("classload",Db.query(c,"SELECT c.id,c.name,COUNT(s.id) subject_count,COALESCE(SUM(s.weekly_hours),0) allocated FROM classes c LEFT JOIN subjects s ON s.class_id=c.id GROUP BY c.id,c.name ORDER BY c.name"));
            }
            if("reports".equals(page)&&!text(r,"id").isEmpty()){
                int id=number(text(r,"id"),1,Integer.MAX_VALUE);r.setAttribute("reportRun",run(c,u,id,false));
                r.setAttribute("roomload",Db.query(c,"SELECT room_id,room_name,COUNT(*) allocated FROM timetable_entries WHERE timetable_id=? GROUP BY room_id,room_name ORDER BY room_name",id));
            }
        }else{
            String column="teacher".equals(u.get("role"))?"faculty_id":"class_id";
            r.setAttribute("saved",Db.query(c,"SELECT DISTINCT t.* FROM saved_timetables t JOIN timetable_entries e ON e.timetable_id=t.id WHERE t.status='published' AND e."+column+"=? ORDER BY t.id DESC",u.get(column)));
        }
    }
    public static String times(Map<String,String> run,int period){
        String[] start=run.get("start_time").split(":");int length=Integer.parseInt(run.get("period_minutes"));
        int mins=Integer.parseInt(start[0])*60+Integer.parseInt(start[1])+(period-1)*length;
        int after=Integer.parseInt(run.get("break_after"));if(after>0&&period>after)mins+=Integer.parseInt(run.get("break_minutes"));
        return String.format("%02d:%02d - %02d:%02d",mins/60,mins%60,(mins+length)/60,(mins+length)%60);
    }
    public static void snapshotSettings(Connection c,int id)throws SQLException{
        Db.update(c,"UPDATE saved_timetables t JOIN app_settings s ON s.id=1 SET t.start_time=s.start_time,t.period_minutes=s.period_minutes,t.break_after=s.break_after,t.break_minutes=s.break_minutes WHERE t.id=?",id);
    }
    public static void checkTimings(String time,int minutes,int periods,int after,int pause){
        if(!time.matches("([01][0-9]|2[0-3]):[0-5][0-9]"))throw new IllegalArgumentException("Enter a valid start time.");
        String[] a=time.split(":");int end=Integer.parseInt(a[0])*60+Integer.parseInt(a[1])+minutes*periods+(after>0&&after<periods?pause:0);
        if(end>24*60)throw new IllegalArgumentException("The teaching day cannot extend past midnight.");
    }
    /** Every mutation below is committed with its audit entry, or entirely rolled back. */
    public static String action(Connection c,HttpServletRequest r,Map<String,String>u,String action)throws Exception{
        if(!Arrays.asList("settings","availability","account","createUser","userStatus","resetPassword","renameRun","cloneRun","publishRun","deleteRun","moveLesson").contains(action))return null;
        if(!"account".equals(action)&&!admin(u))throw new SecurityException("Administrator access is required.");
        c.setAutoCommit(false);
        try{
            String destination="?page="+action,detail="";
            if("settings".equals(action)){
                String institution=text(r,"institution"),year=text(r,"academic_year"),time=text(r,"start_time");
                if(institution.isEmpty()||institution.length()>80||year.isEmpty()||year.length()>30)throw new IllegalArgumentException("Enter an institution (up to 80 characters) and academic year (up to 30 characters).");
                int days=number(text(r,"days"),1,6),periods=number(text(r,"periods"),1,8),minutes=number(text(r,"period_minutes"),20,120),after=number(text(r,"break_after"),0,8),pause=number(text(r,"break_minutes"),0,120);
                checkTimings(time,minutes,periods,after,pause);
                Db.update(c,"UPDATE app_settings SET institution=?,academic_year=?,days=?,periods=?,start_time=?,period_minutes=?,break_after=?,break_minutes=? WHERE id=1",institution,year,days,periods,time,minutes,after,pause);detail="Updated defaults for future timetables";
            }else if("availability".equals(action)){
                int id=number(text(r,"faculty_id"),1,Integer.MAX_VALUE);one(c,"SELECT id FROM faculty WHERE id=?",id);
                Set<String> slots=new TreeSet<String>();String[] values=r.getParameterValues("blocked");
                if(values!=null)for(String s:values){if(!s.matches("[1-6]:[1-8]"))throw new IllegalArgumentException("Invalid availability slot.");slots.add(s);}
                Db.update(c,"UPDATE faculty SET unavailable=? WHERE id=?",String.join(",",slots),id);detail="Updated teacher #"+id+" availability";destination="?page=availability&teacher="+id;
            }else if("account".equals(action)){
                Map<String,String> current=one(c,"SELECT * FROM users WHERE id=? FOR UPDATE",u.get("id"));
                if(!Security.verify(text(r,"current_password"),current.get("salt"),current.get("password_hash")))throw new IllegalArgumentException("Current password is incorrect.");
                String password=text(r,"new_password");if(!password.equals(text(r,"confirm_password")))throw new IllegalArgumentException("New passwords do not match.");
                password(c,Integer.parseInt(u.get("id")),password);detail="Changed own password";
            }else if("createUser".equals(action)){
                String username=text(r,"username"),role=text(r,"role");
                if(!username.matches("[A-Za-z0-9_.-]{3,40}"))throw new IllegalArgumentException("Username needs 3-40 letters, numbers, dots, underscores or hyphens.");
                if(!Arrays.asList("admin","teacher","student").contains(role))throw new IllegalArgumentException("Select a valid role.");
                Integer teacher=null,group=null;
                if("teacher".equals(role)){teacher=number(text(r,"faculty_id"),1,Integer.MAX_VALUE);one(c,"SELECT id FROM faculty WHERE id=?",teacher);}
                if("student".equals(role)){group=number(text(r,"class_id"),1,Integer.MAX_VALUE);one(c,"SELECT id FROM classes WHERE id=?",group);}
                String pwd=text(r,"password");validatePassword(pwd);String salt=salt();
                Db.update(c,"INSERT INTO users(username,salt,password_hash,role,faculty_id,class_id,active) VALUES(?,?,?,?,?,?,1)",username,salt,Security.hash(pwd,salt),role,teacher,group);destination="?page=users";detail="Created "+role+" account "+username;
            }else if("userStatus".equals(action)||"resetPassword".equals(action)){
                int id=number(text(r,"id"),1,Integer.MAX_VALUE);Map<String,String> target=one(c,"SELECT id,username,role,active FROM users WHERE id=? FOR UPDATE",id);
                if(u.get("id").equals(String.valueOf(id)))throw new IllegalArgumentException("Use My account for your own password. You cannot disable yourself.");
                if("admin".equals(target.get("role")))throw new IllegalArgumentException("Other administrator accounts cannot be changed here.");
                if("userStatus".equals(action))Db.update(c,"UPDATE users SET active=? WHERE id=?","1".equals(target.get("active"))?0:1,id);
                else password(c,id,text(r,"password"));
                destination="?page=users";detail=action+" for "+target.get("username");
            }else{
                int id=number(text(r,"id"),1,Integer.MAX_VALUE);Map<String,String> run=run(c,u,id,true);destination="?page=timetable&id="+id;
                if("renameRun".equals(action)){
                    String name=text(r,"name");if(name.isEmpty()||name.length()>80)throw new IllegalArgumentException("Enter a name of 1-80 characters.");
                    Db.update(c,"UPDATE saved_timetables SET name=? WHERE id=?",name,id);detail="Renamed timetable #"+id;
                }else if("cloneRun".equals(action)){
                    String name=run.get("name");name=(name.length()>70?name.substring(0,70):name)+" (copy)";
                    int copy=Db.insert(c,"INSERT INTO saved_timetables(name,days,periods,lesson_count,status,start_time,period_minutes,break_after,break_minutes) SELECT ?,days,periods,lesson_count,'draft',start_time,period_minutes,break_after,break_minutes FROM saved_timetables WHERE id=?",name,id);
                    Db.update(c,"INSERT INTO timetable_entries(timetable_id,class_id,faculty_id,room_id,subject_id,class_name,faculty_name,room_name,subject_name,day_no,period_no) SELECT ?,class_id,faculty_id,room_id,subject_id,class_name,faculty_name,room_name,subject_name,day_no,period_no FROM timetable_entries WHERE timetable_id=?",copy,id);
                    destination="?page=timetable&id="+copy;detail="Copied #"+id+" to draft #"+copy;
                }else if("publishRun".equals(action)){
                    String status="published".equals(run.get("status"))?"draft":"published";
                    Db.update(c,"UPDATE saved_timetables SET status=? WHERE id=?",status,id);detail="Set timetable #"+id+" to "+status;
                }else if("deleteRun".equals(action)){
                    if("published".equals(run.get("status")))throw new IllegalArgumentException("Unpublish this timetable before deleting it.");
                    Db.update(c,"DELETE FROM timetable_entries WHERE timetable_id=?",id);Db.update(c,"DELETE FROM saved_timetables WHERE id=?",id);destination="?page=saved";detail="Deleted draft timetable #"+id;
                }else if("moveLesson".equals(action)){
                    if("published".equals(run.get("status")))throw new IllegalArgumentException("Unpublish or duplicate the timetable before editing lessons.");
                    int entry=number(text(r,"entry_id"),1,Integer.MAX_VALUE),day=number(text(r,"day_no"),1,Integer.parseInt(run.get("days"))),period=number(text(r,"period_no"),1,Integer.parseInt(run.get("periods"))),room=number(text(r,"room_id"),1,Integer.MAX_VALUE);
                    Map<String,String> e=one(c,"SELECT e.*,c.student_count,f.unavailable,s.type FROM timetable_entries e JOIN classes c ON c.id=e.class_id JOIN faculty f ON f.id=e.faculty_id JOIN subjects s ON s.id=e.subject_id AND s.class_id=e.class_id AND s.faculty_id=e.faculty_id WHERE e.id=? AND e.timetable_id=?",entry,id);
                    Map<String,String> rm=one(c,"SELECT * FROM classrooms WHERE id=?",room);
                    if(Integer.parseInt(rm.get("capacity"))<Integer.parseInt(e.get("student_count")))throw new IllegalArgumentException("That room has too few seats.");
                    if("Lab".equals(e.get("type"))&&!"Lab".equals(rm.get("type")))throw new IllegalArgumentException("A lab lesson needs a lab room.");
                    if(Arrays.asList(e.get("unavailable").split(",")).contains(day+":"+period))throw new IllegalArgumentException("The teacher is unavailable in that period.");
                    if(!Db.query(c,"SELECT id FROM timetable_entries WHERE timetable_id=? AND id<>? AND day_no=? AND period_no=? AND (class_id=? OR faculty_id=? OR room_id=?)",id,entry,day,period,e.get("class_id"),e.get("faculty_id"),room).isEmpty())throw new IllegalArgumentException("That slot conflicts with a class, teacher or room booking. Choose a free slot.");
                    Db.update(c,"UPDATE timetable_entries SET day_no=?,period_no=?,room_id=?,room_name=? WHERE id=?",day,period,room,rm.get("name"),entry);detail="Moved lesson #"+entry+" in timetable #"+id;
                }
            }
            audit(c,u,action,detail);c.commit();return destination;
        }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
    }
    private static void validatePassword(String p){if(p.length()<8||p.length()>100)throw new IllegalArgumentException("Use a password with 8-100 characters.");}
    private static String salt(){byte[] b=new byte[24];new SecureRandom().nextBytes(b);return Base64.getEncoder().encodeToString(b);}
    private static void password(Connection c,int id,String password)throws Exception{validatePassword(password);String salt=salt();Db.update(c,"UPDATE users SET salt=?,password_hash=? WHERE id=?",salt,Security.hash(password,salt),id);}
}
