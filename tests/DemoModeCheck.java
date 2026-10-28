package app;
import java.lang.reflect.*;
import javax.servlet.http.*;
public class DemoModeCheck {
 public static void main(String[] args)throws Exception{
  final boolean demo="true".equalsIgnoreCase(System.getenv("TIMETABLE_DEMO_MODE"));
  final String[] redirect={null}; final int[] error={0}; final boolean[] session={false};
  HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(DemoModeCheck.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
   if(m.getName().equals("getContextPath"))return "/Timetable";
   if(m.getName().equals("getSession")){session[0]=true;throw new IllegalStateException("AUTH_BOUNDARY");}
   throw new AssertionError("Unexpected request access: "+m.getName());
  });
  HttpServletResponse res=(HttpServletResponse)Proxy.newProxyInstance(DemoModeCheck.class.getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
   if(m.getName().equals("sendRedirect"))redirect[0]=(String)a[0];
   if(m.getName().equals("sendError"))error[0]=(Integer)a[0];
   return null;
  });
  AppServlet app=new AppServlet();
  if(demo){
   app.doGet(req,res); app.doPost(req,res);
   if(!"/Timetable/demo.html".equals(redirect[0])||error[0]!=403||session[0])throw new AssertionError("Demo route not isolated");
  }else{
   try{app.doGet(req,res);throw new AssertionError("Expected normal auth boundary");}
   catch(IllegalStateException e){if(!"AUTH_BOUNDARY".equals(e.getMessage()))throw e;}
   if(redirect[0]!=null||!session[0])throw new AssertionError("Normal login bypassed");
  }
  System.out.println(demo?"PASS: demo GET without session/database; POST blocked":"PASS: normal mode retains authentication path");
 }
}
