import app.*;
import java.sql.*;
import java.util.*;

/** Runs against the existing schema; every test mutation is rolled back. */
public class RegistrationCheck {
    static int checks=0;
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    interface Attempt{void run()throws Exception;}
    static void rejects(Attempt action,String label)throws Exception{try{action.run();}catch(IllegalArgumentException e){check(true,label);return;}throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        try(final Connection c=Db.open()){
            c.setAutoCommit(false);
            try{
                final int group=Integer.parseInt(Db.query(c,"SELECT id FROM classes ORDER BY id DESC LIMIT 1").get(0).get("id"));
                final int teacher=Integer.parseInt(Db.query(c,"SELECT id FROM faculty ORDER BY id LIMIT 1").get(0).get("id"));
                final String name="check_"+System.currentTimeMillis(),pwd="Registration-test-42!";
                int student=Registration.create(c,name,pwd,pwd,"student",group);
                Map<String,String> row=Db.query(c,"SELECT * FROM users WHERE id=?",student).get(0);
                check("0".equals(row.get("active"))&&row.get("faculty_id")==null&&String.valueOf(group).equals(row.get("class_id")),"student request is inactive and linked only to chosen class");
                check(!pwd.equals(row.get("password_hash"))&&Security.verify(pwd,row.get("salt"),row.get("password_hash")),"password stored as salted hash");
                check(Registration.authenticate(c,name,pwd,"student",false)==null,"pending account cannot sign in");
                Db.update(c,"UPDATE users SET active=1 WHERE id=?",student);
                Map<String,String> studentUser=Registration.authenticate(c,name,pwd,"student",true);
                check(studentUser!=null,"approved student can sign in");
                check(Registration.authenticate(c,name,pwd,"admin",false)==null,"student cannot sign in as admin");
                check(Registration.authenticate(c,name,"incorrect","student",false)==null,"incorrect password rejected");
                check(!Portal.allowed(studentUser,"users")&&!Portal.allowed(studentUser,"generate"),"student cannot access admin or generation pages");
                List<Map<String,String>> runs=Db.query(c,"SELECT DISTINCT timetable_id FROM timetable_entries WHERE class_id=?",group);
                if(!runs.isEmpty()){
                    int run=Integer.parseInt(runs.get(0).get("timetable_id"));
                    Db.update(c,"UPDATE saved_timetables SET status='published' WHERE id=?",run);
                    Portal.run(c,studentUser,run,false);
                    List<Map<String,String>> visible=Portal.entries(c,studentUser,run);
                    check(!visible.isEmpty(),"approved student sees published class timetable");
                    for(Map<String,String> entry:visible)if(!String.valueOf(group).equals(entry.get("class_id")))throw new AssertionError("Another class leaked");
                    check(true,"student entries exclude all other classes");
                    Db.update(c,"UPDATE saved_timetables SET status='draft' WHERE id=?",run);
                    boolean denied=false;try{Portal.run(c,studentUser,run,false);}catch(SecurityException e){denied=true;}
                    check(denied,"draft timetable is hidden from student");
                }
                int staff=Registration.create(c,name+"t",pwd,pwd,"teacher",teacher);
                Map<String,String> staffRow=Db.query(c,"SELECT * FROM users WHERE id=?",staff).get(0);
                check(staffRow.get("class_id")==null&&String.valueOf(teacher).equals(staffRow.get("faculty_id")),"teacher request linked only to chosen teacher");
                check(!row.get("salt").equals(staffRow.get("salt")),"each account has a different salt");
                check(Registration.authenticate(c,name+"t",pwd,"teacher",false)==null,"pending teacher cannot sign in");
                Db.update(c,"UPDATE users SET active=1 WHERE id=?",staff);
                Map<String,String> staffUser=Registration.authenticate(c,name+"t",pwd,"teacher",true);
                check(staffUser!=null,"approved teacher can sign in");
                for(Map<String,String> run:Db.query(c,"SELECT id FROM saved_timetables"))for(Map<String,String> entry:Portal.entries(c,staffUser,Integer.parseInt(run.get("id"))))if(!String.valueOf(teacher).equals(entry.get("faculty_id")))throw new AssertionError("Another teacher leaked");
                check(true,"teacher entries exclude other teachers");
                rejects(()->Registration.create(c,name,pwd,pwd,"student",group),"duplicate username rejected");
                rejects(()->Registration.create(c,name+"a",pwd,pwd,"admin",group),"public admin registration rejected");
                rejects(()->Registration.create(c,name+"b","short","short","student",group),"short password rejected");
                rejects(()->Registration.create(c,name+"c",pwd,"different","student",group),"password mismatch rejected");
                rejects(()->Registration.create(c,name+"d",pwd,pwd,"student",Integer.MAX_VALUE),"nonexistent class rejected");
                rejects(()->Registration.create(c,"bad'name",pwd,pwd,"student",group),"invalid username rejected");
                Db.update(c,"UPDATE users SET active=0 WHERE id=?",student);
                check(Registration.authenticate(c,name,pwd,"student",false)==null,"disabled account cannot sign in");
                check(Registration.authenticate(c,"admin","Demo@2026","admin",true)==null,"demo password blocked in public deployment");
            }finally{c.rollback();c.setAutoCommit(true);}
        }
        long now=System.currentTimeMillis();
        for(int i=0;i<5;i++)check(AuthThrottle.allow("register","test-registration",now),"registration allowance "+(i+1));
        check(!AuthThrottle.allow("register","test-registration",now),"registration rate limit enforced");
        check(AuthThrottle.allow("register","test-registration",now+3600001L),"registration limit expires");
        System.out.println("TOTAL "+checks+" registration checks passed; database test changes rolled back.");
    }
}
