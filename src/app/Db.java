package app;

import java.sql.*;
import java.util.*;

/** JDBC access shared by the servlet and the scheduling service. */
public final class Db {
    private Db() {}
    private static String configuration(String property,String environment) throws SQLException {
        String value=System.getProperty(property);
        if(value==null||value.isEmpty())value=System.getenv(environment);
        if(value==null||value.isEmpty())throw new SQLException("Missing database configuration: "+environment+". Use Start-Local.ps1.");
        return value;
    }
    public static Connection open() throws SQLException {
        try { Class.forName("com.mysql.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("JDBC driver is missing", e); }
        return DriverManager.getConnection(
            configuration("timetable.db.url","TIMETABLE_DB_URL"),
            configuration("timetable.db.user","TIMETABLE_DB_USER"),
            configuration("timetable.db.password","TIMETABLE_DB_PASSWORD"));
    }
    public static List<Map<String,String>> query(Connection c, String sql, Object... params) throws SQLException {
        List<Map<String,String>> rows = new ArrayList<Map<String,String>>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, params);
            try (ResultSet r = p.executeQuery()) {
                ResultSetMetaData m = r.getMetaData();
                while (r.next()) {
                    Map<String,String> row = new LinkedHashMap<String,String>();
                    for (int i=1;i<=m.getColumnCount();i++) row.put(m.getColumnLabel(i), r.getString(i));
                    rows.add(row);
                }
            }
        }
        return rows;
    }
    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { bind(p,params); return p.executeUpdate(); }
    }
    public static int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            bind(p,params); p.executeUpdate();
            try (ResultSet r=p.getGeneratedKeys()) { if(r.next())return r.getInt(1); }
            throw new SQLException("No generated record id");
        }
    }
    private static void bind(PreparedStatement p,Object[] a) throws SQLException {
        for(int i=0;i<a.length;i++)p.setObject(i+1,a[i]);
    }
}
