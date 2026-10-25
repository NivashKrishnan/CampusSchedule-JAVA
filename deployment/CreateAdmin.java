import app.*;
import java.sql.*;
import java.security.SecureRandom;
import java.util.Base64;

/** One-off cloud bootstrap; credentials come only from private environment variables. */
public class CreateAdmin {
    public static void main(String[] args)throws Exception {
        String name=System.getenv("TIMETABLE_ADMIN_USERNAME"),password=System.getenv("TIMETABLE_ADMIN_PASSWORD");
        if(name==null||!name.matches("[A-Za-z0-9_.-]{3,40}")||password==null||password.length()<12||password.length()>100)
            throw new IllegalArgumentException("Provide a valid admin username and a private 12-100 character password.");
        try(Connection c=Db.open()){
            if(!Db.query(c,"SELECT id FROM users WHERE role='admin'").isEmpty())throw new IllegalStateException("An administrator already exists; nothing changed.");
            byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);String salt=Base64.getEncoder().encodeToString(bytes);
            Db.update(c,"INSERT INTO users(username,salt,password_hash,role,active) VALUES(?,?,?,'admin',1)",name,salt,Security.hash(password,salt));
            System.out.println("Administrator created. Remove the bootstrap password environment variable now.");
        }
    }
}
