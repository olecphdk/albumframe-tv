package dk.femto.albumframe;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Encrypted app-private storage. Android backup is disabled in the manifest. */
final class CredentialStore {
    private static final String ALIAS="albumframe-flickr-v1";
    private static javax.crypto.SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if(!ks.containsAlias(ALIAS)) {
            KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            gen.generateKey();
        }
        return (javax.crypto.SecretKey)ks.getKey(ALIAS,null);
    }
    static void save(Context context,String apiKey,String secret,String token,String tokenSecret,String user) throws Exception {
        JSONObject json=new JSONObject().put("key",apiKey).put("secret",secret).put("token",token).put("tokenSecret",tokenSecret).put("user",user);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key());
        String encrypted=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if(!context.getSharedPreferences("flickr",0).edit().putString("encrypted",encrypted).commit()) throw new java.io.IOException("Could not save credentials");
    }
    static JSONObject load(Context context) throws Exception {
        String raw=context.getSharedPreferences("flickr",0).getString("encrypted","");
        if(raw.isEmpty()) throw new java.io.IOException("Connect Flickr first.");
        String[] parts=raw.split(":",2);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
        return new JSONObject(new String(c.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8));
    }
    static String user(Context context) {
        try { return load(context).getString("user"); } catch(Exception e) { return ""; }
    }
    static void clear(Context context) throws Exception {
        if(!context.getSharedPreferences("flickr",0).edit().clear().commit()) throw new java.io.IOException("Could not clear");
        context.getSharedPreferences("album",0).edit().clear().apply();
        context.getSharedPreferences("positions",0).edit().clear().apply();
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null); ks.deleteEntry(ALIAS);
    }
}
