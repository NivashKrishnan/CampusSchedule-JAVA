package com.campusschedule.timetable;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.graphics.Color;
import android.view.View;
import android.webkit.*;
import android.widget.*;

/** Android client for the existing CampusSchedule Java server. */
public final class MainActivity extends Activity {
    private WebView web;
    private String server;
    private LinearLayout root;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        server=getPreferences(MODE_PRIVATE).getString("server","");
        if(valid(server))openServer();else setup();
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private void layout(){
        if(web!=null){web.stopLoading();web.destroy();web=null;}
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setFitsSystemWindows(true);
        root.setBackgroundColor(Color.rgb(245,248,253));setContentView(root);
    }
    private TextView text(String value,int size){TextView label=new TextView(this);label.setText(value);label.setTextSize(size);label.setTextColor(Color.rgb(23,45,73));label.setPadding(dp(16),dp(12),dp(16),dp(12));root.addView(label);return label;}
    private boolean valid(String value){
        if(value==null)return false;
        Uri uri=Uri.parse(value);String host=uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme())&&host!=null&&!host.isEmpty()&&uri.getUserInfo()==null&&uri.getQuery()==null&&uri.getFragment()==null;
    }
    private boolean sameServer(Uri uri){
        Uri base=Uri.parse(server);
        return "https".equalsIgnoreCase(uri.getScheme())&&uri.getUserInfo()==null&&base.getHost().equalsIgnoreCase(uri.getHost())&&(base.getPort()==-1?443:base.getPort())==(uri.getPort()==-1?443:uri.getPort());
    }
    private void setup(){
        layout();text("CampusSchedule",28);text("Connect your college timetable",20);
        text("Enter the website address provided by your administrator. Your own account gives you access to your class or teaching timetable.",16);
        final EditText address=new EditText(this);address.setSingleLine(true);address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setHint("https://your-website/Timetable/");address.setText(server);address.setPadding(dp(16),dp(12),dp(16),dp(12));root.addView(address);
        Button connect=new Button(this);connect.setText("Connect");root.addView(connect);
        connect.setOnClickListener(v->{String value=address.getText().toString().trim();if(!valid(value)){address.setError("Enter a valid HTTPS website address.");return;}
            final String next=value.endsWith("/")?value:value+"/";
            CookieManager.getInstance().removeAllCookies(removed->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();server=next;getPreferences(MODE_PRIVATE).edit().putString("server",server).apply();openServer();});
        });
        text("The website must be hosted before this app can connect. A localhost address from your laptop does not work on your phone.",14);
    }
    private void openServer(){
        layout();
        LinearLayout bar=new LinearLayout(this);root.addView(bar);
        Button reload=new Button(this);reload.setText("Reload");bar.addView(reload);
        Button settings=new Button(this);settings.setText("Server settings");bar.addView(settings);
        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        WebSettings options=web.getSettings();options.setJavaScriptEnabled(true);options.setDomStorageEnabled(true);
        options.setAllowFileAccess(false);options.setAllowContentAccess(false);options.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        options.setJavaScriptCanOpenWindowsAutomatically(false);options.setSupportMultipleWindows(false);options.setSafeBrowsingEnabled(true);
        WebView.setWebContentsDebuggingEnabled(false);CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                if(sameServer(request.getUrl()))return false;
                Toast.makeText(MainActivity.this,"Only your configured timetable website can open here.",Toast.LENGTH_SHORT).show();return true;
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
                if(request.isForMainFrame())Toast.makeText(MainActivity.this,"Cannot reach the timetable server. Check your internet and server address, then tap Reload.",Toast.LENGTH_LONG).show();
            }
        });
        reload.setOnClickListener(v->web.reload());settings.setOnClickListener(v->setup());web.loadUrl(server);
    }
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onPause(){if(web!=null)web.onPause();CookieManager.getInstance().flush();super.onPause();}
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
    @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}
