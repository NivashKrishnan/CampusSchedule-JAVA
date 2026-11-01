package app;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

/** A bounded, versioned file snapshot. No SQL engine or Java object deserialization. */
public final class FileStore {
    public static final String[] TABLES={"departments","faculty","classes","classrooms","subjects","saved_timetables","timetable_entries","users","activity_log"};
    public static final int LIMIT=4*1024*1024;
    public static final class Data {
        public final Map<String,String> meta=new LinkedHashMap<String,String>();
        public final Map<String,String> settings=new LinkedHashMap<String,String>();
        public final Map<String,List<Map<String,String>>> tables=new LinkedHashMap<String,List<Map<String,String>>>();
        public String revision;
        public Data(){for(String t:TABLES)tables.put(t,new ArrayList<Map<String,String>>());}
        public List<Map<String,String>> rows(String table){List<Map<String,String>> r=tables.get(table);if(r==null)throw new IllegalArgumentException("Unknown record type.");return r;}
        public Map<String,String> find(String table,String id){for(Map<String,String> r:rows(table))if(id.equals(r.get("id")))return r;throw new IllegalArgumentException("The linked record no longer exists.");}
        public Map<String,String> add(String table,Map<String,String> values){int n=Integer.parseInt(meta.getOrDefault("next."+table,"0"))+1;meta.put("next."+table,String.valueOf(n));Map<String,String> row=new LinkedHashMap<String,String>(values);row.put("id",String.valueOf(n));rows(table).add(row);return row;}
    }
    public static Data empty(){Data d=new Data();d.meta.put("signingKey",Security.token()+Security.token());String[] defaults={"institution","CampusSchedule","academic_year","2026-2027","days","6","periods","8","start_time","09:00","period_minutes","50","break_after","4","break_minutes","55"};for(int i=0;i<defaults.length;i+=2)d.settings.put(defaults[i],defaults[i+1]);return d;}
    private static byte[] readBounded(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[8192];for(int n;(n=in.read(a))!=-1;){if(b.size()+n>LIMIT)throw new IOException("Storage limit reached");b.write(a,0,n);}return b.toByteArray();}
    private static void map(DataOutputStream out,Map<String,String> m)throws IOException{out.writeInt(m.size());for(Map.Entry<String,String> e:m.entrySet()){out.writeUTF(e.getKey());out.writeUTF(e.getValue()==null?"":e.getValue());}}
    private static void map(DataInputStream in,Map<String,String> m)throws IOException{int n=in.readInt();if(n<0||n>200)throw new IOException("Invalid snapshot");for(int i=0;i<n;i++)m.put(in.readUTF(),in.readUTF());}
    public static byte[] encode(Data d)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeUTF("CampusSchedule-files-v1");map(o,d.meta);map(o,d.settings);for(String t:TABLES){List<Map<String,String>> rows=d.rows(t);o.writeInt(rows.size());for(Map<String,String> r:rows)map(o,r);}o.close();if(b.size()>LIMIT)throw new IOException("Storage limit reached");return b.toByteArray();}
    public static Data decode(byte[] bytes)throws IOException{Data d=new Data();DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));if(!"CampusSchedule-files-v1".equals(in.readUTF()))throw new IOException("Unknown snapshot format");map(in,d.meta);map(in,d.settings);for(String t:TABLES){int n=in.readInt();if(n<0||n>30000)throw new IOException("Invalid snapshot");for(int i=0;i<n;i++){Map<String,String> r=new LinkedHashMap<String,String>();map(in,r);d.rows(t).add(r);}}if(in.available()!=0)throw new IOException("Invalid trailing data");return d;}
    private static HttpURLConnection connection(HttpServletRequest request,String method)throws IOException{
        // The bridge binds only to loopback; it is never a public data endpoint.
        HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:8181/state").openConnection();c.setConnectTimeout(5000);c.setReadTimeout(20000);c.setRequestMethod(method);String token=request.getHeader("x-vercel-oidc-token");if(token!=null)c.setRequestProperty("x-vercel-oidc-token",token);return c;
    }
    public static Data load(HttpServletRequest request)throws IOException{HttpURLConnection c=connection(request,"GET");try{int status=c.getResponseCode();if(status==404)return empty();if(status!=200)throw new IOException("Shared storage unavailable ("+status+")");Data d;try(InputStream in=c.getInputStream()){d=decode(readBounded(in));}d.revision=c.getHeaderField("ETag");if(d.revision==null)throw new IOException("Missing file version");return d;}finally{c.disconnect();}}
    public static void save(HttpServletRequest request,Data d)throws IOException{byte[] body=encode(d);HttpURLConnection c=connection(request,"PUT");try{c.setDoOutput(true);c.setFixedLengthStreamingMode(body.length);c.setRequestProperty("Content-Type","application/octet-stream");if(d.revision!=null)c.setRequestProperty("If-Match",d.revision);try(OutputStream out=c.getOutputStream()){out.write(body);}int status=c.getResponseCode();if(status==409||status==412)throw new ConcurrentModificationException("Another user saved changes. Reload this page and try again; your changes were not saved.");if(status!=200)throw new IOException("Shared save failed ("+status+")");d.revision=c.getHeaderField("ETag");}finally{c.disconnect();}}
}
