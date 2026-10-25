package app;

import java.sql.*;
import java.util.*;
import java.security.SecureRandom;

/** Public account requests are inactive until reviewed by an administrator. */
public final class Registration {
    private Registration() {}
    public static Map<String,String> authenticate(Connection c,String username,String password,String role,boolean publicDeployment)throws Exception {
        if(password.length()>100||username.length()>80||!Arrays.asList("admin","teacher","student").contains(role))return null;
        if(publicDeployment&&"Demo@2026".equals(password))return null;
        List<Map<String,String>> users=Db.query(c,"SELECT * FROM users WHERE username=? AND active=1",username);
        if(users.isEmpty())return null;
        Map<String,String> user=users.get(0);
        return role.equals(user.get("role"))&&Security.verify(password,user.get("salt"),user.get("password_hash"))?user:null;
    }
    public static int create(Connection c,String username,String password,String confirmation,String role,int linkedId)throws Exception {
        if(!username.matches("[A-Za-z0-9_.-]{3,40}"))throw new IllegalArgumentException("Username needs 3-40 letters, numbers, dots, underscores or hyphens.");
        if(!Arrays.asList("teacher","student").contains(role))throw new IllegalArgumentException("Only teachers and students can register. Administrators create other admin accounts.");
        if(password.length()<12||password.length()>100)throw new IllegalArgumentException("Use a password with 12-100 characters.");
        if(!password.equals(confirmation))throw new IllegalArgumentException("Passwords do not match.");
        if(linkedId<1)throw new IllegalArgumentException("Select your teacher record or class.");
        if(!Db.query(c,"SELECT id FROM users WHERE username=?",username).isEmpty())throw new IllegalArgumentException("That username is unavailable. Choose another username.");
        String table="teacher".equals(role)?"faculty":"classes";
        if(Db.query(c,"SELECT id FROM "+table+" WHERE id=?",linkedId).isEmpty())throw new IllegalArgumentException("Choose an existing teacher record or class.");
        byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);
        String salt=Base64.getEncoder().encodeToString(bytes);
        int id=Db.insert(c,"INSERT INTO users(username,salt,password_hash,role,faculty_id,class_id,active) VALUES(?,?,?,?,?,?,0)",username,salt,Security.hash(password,salt),role,"teacher".equals(role)?linkedId:null,"student".equals(role)?linkedId:null);
        Db.update(c,"INSERT INTO activity_log(username,action,detail) VALUES(?,?,?)",username,"register","Requested "+role+" access; awaiting administrator approval");
        return id;
    }
}
