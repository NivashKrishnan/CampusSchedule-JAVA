package app;

import java.util.*;

/** Bounded, per-process limits. Use one application replica or an edge limiter. */
public final class AuthThrottle {
    private static final Map<String,long[]> windows=new HashMap<String,long[]>();
    private AuthThrottle() {}
    public static synchronized boolean allow(String action,String address,long now){
        Iterator<long[]> it=windows.values().iterator();
        while(it.hasNext())if(it.next()[0]<=now)it.remove();
        String key=action+":"+address;
        long[] state=windows.get(key);
        if(state==null){
            if(windows.size()>=10000)return false;
            state=new long[]{now+("register".equals(action)?3600000L:900000L),0};windows.put(key,state);
        }
        int limit="register".equals(action)?5:30;
        if(state[1]>=limit)return false;
        state[1]++;return true;
    }
}
