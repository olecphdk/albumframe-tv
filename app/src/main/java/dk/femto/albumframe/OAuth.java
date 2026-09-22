package dk.femto.albumframe;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Flickr OAuth 1.0a. No credentials or response bodies are logged. */
final class OAuth {
    static Map<String,String> params(String... pairs) {
        Map<String,String> result=new HashMap<>();
        for(int i=0;i<pairs.length;i+=2) result.put(pairs[i],pairs[i+1]);
        return result;
    }
    static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~"); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    static Map<String,String> parse(String s) throws IOException {
        Map<String,String> m = new HashMap<>();
        if (s.isEmpty()) return m;
        for (String p : s.split("&")) {
            String[] kv = p.split("=", 2);
            String key = URLDecoder.decode(kv[0], "UTF-8");
            if (m.containsKey(key)) throw new IOException("Duplicate parameter");
            m.put(key, kv.length == 2 ? URLDecoder.decode(kv[1], "UTF-8") : "");
        }
        return m;
    }
    static String query(Map<String,String> p) {
        TreeMap<String,String> sorted = new TreeMap<>();
        for (Map.Entry<String,String> e : p.entrySet()) sorted.put(enc(e.getKey()), enc(e.getValue()));
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String,String> e : sorted.entrySet()) {
            if (b.length() > 0) b.append('&');
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        return b.toString();
    }
    static String signature(String url, Map<String,String> params, String secret, String tokenSecret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((enc(secret) + "&" + enc(tokenSecret)).getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(("GET&" + enc(url) + "&" + enc(query(params))).getBytes(StandardCharsets.UTF_8)));
    }
    static String get(String endpoint, String key, String secret, String token, String tokenSecret, Map<String,String> extra) throws Exception {
        String url = "https://www.flickr.com/services/" + endpoint;
        Map<String,String> p = new HashMap<>(extra);
        p.put("oauth_consumer_key", key);
        p.put("oauth_nonce", UUID.randomUUID().toString());
        p.put("oauth_timestamp", Long.toString(System.currentTimeMillis()/1000));
        p.put("oauth_signature_method", "HMAC-SHA1");
        p.put("oauth_version", "1.0");
        if (!token.isEmpty()) p.put("oauth_token", token);
        p.put("oauth_signature", signature(url, p, secret, tokenSecret));
        HttpURLConnection c = (HttpURLConnection)new URL(url + "?" + query(p)).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(15000); c.setReadTimeout(15000);
        try {
            int status = c.getResponseCode();
            if (status != 200) throw new IOException("Flickr HTTP " + status + ". Check the API key, secret, TV clock and callback.");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[4096]; int n;
                while ((n = in.read(buf)) != -1) {
                    if (out.size() + n > 262144) throw new IOException("Response is too large");
                    out.write(buf, 0, n);
                }
            }
            return out.toString("UTF-8");
        } finally { c.disconnect(); }
    }
}
