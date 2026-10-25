package app;

import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Security {
    public static String token() { byte[] b=new byte[24];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    public static String hash(String password,String salt) throws Exception {
        PBEKeySpec s=new PBEKeySpec(password.toCharArray(),Base64.getDecoder().decode(salt),120000,256);
        try { return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(s).getEncoded()); }
        finally { s.clearPassword(); }
    }
    public static boolean verify(String password,String salt,String expected) throws Exception {
        return MessageDigest.isEqual(hash(password,salt).getBytes("UTF-8"),expected.getBytes("UTF-8"));
    }
    public static String html(Object value) {
        return value==null?"":value.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
}
