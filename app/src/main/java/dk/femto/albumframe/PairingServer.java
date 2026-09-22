package dk.femto.albumframe;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import javax.net.ssl.*;

/** Bounded, single-session HTTPS listener. Exists only while the pairing screen is open. */
final class PairingServer implements AutoCloseable {
    interface Listener {
        void status(String message);
        void complete(String key,String secret,String token,String tokenSecret,String user) throws Exception;
    }
    private final Listener listener;
    private final java.util.function.UnaryOperator<String> translate;
    private final String language;
    private final SSLServerSocket socket;
    private final String path, origin;
    private final long deadline = System.nanoTime()+600_000_000_000L;
    private volatile boolean closed;
    private volatile Socket active;
    private String key="", secret="", requestToken="", requestSecret="";
    private boolean attempted;
    private final java.util.concurrent.ScheduledExecutorService expiry=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    String url() { return origin+path; }
    PairingServer(String ip, Listener listener) throws Exception {
        this(ip,value->value,"en",listener);
    }
    PairingServer(String ip,java.util.function.UnaryOperator<String> translate,String language,Listener listener) throws Exception {
        this.translate=translate;this.language=language;
        this.listener=listener;
        socket=(SSLServerSocket)LocalTls.context().getServerSocketFactory().createServerSocket(0,4,InetAddress.getByName(ip));
        socket.setSoTimeout(1000);
        LocalTls.enableModernProtocols(socket);
        byte[] id=new byte[16]; new SecureRandom().nextBytes(id);
        path="/"+Base64.getUrlEncoder().withoutPadding().encodeToString(id);
        origin="https://"+ip+":"+socket.getLocalPort();
    }
    void start() {
        expiry.schedule(()->{
            if(!closed) { close(); listener.status(translate.apply("Setup expired. Select Back and Connect Flickr again.")); }
        },600,java.util.concurrent.TimeUnit.SECONDS);
        new Thread(this::run,"AlbumFrame-local-setup").start();
    }
    private void run() {
        try {
            while(!closed && System.nanoTime()<deadline) {
                try(Socket client=socket.accept()) {
                    active=client; client.setSoTimeout(5000);
                    handle(client);
                } catch(SocketTimeoutException ignored) {
                } catch(Exception ignored) {
                    // Browser TLS rejection and untrusted network input must not expose credentials.
                } finally { active=null; }
            }
        } finally {
            boolean expired=!closed;
            close();
            if(expired) listener.status(translate.apply("Setup expired. Select Back and Connect Flickr again."));
        }
    }
    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream(); int c;
        while((c=in.read())!=-1 && c!='\n') { if(b.size()>4096) throw new IOException("Header too long"); b.write(c); }
        if(c==-1 && b.size()==0) throw new EOFException();
        return b.toString("US-ASCII").replace("\r","");
    }
    private void handle(Socket client) throws Exception {
        InputStream in=client.getInputStream();
        String[] first=line(in).split(" ");
        if(first.length!=3) return;
        Map<String,String> headers=new HashMap<>();
        int total=0;
        for(String l;(l=line(in)).length()>0;) {
            total+=l.length(); if(total>8192) return;
            int sep=l.indexOf(':'); if(sep<1) return;
            String h=l.substring(0,sep).toLowerCase(Locale.ROOT);
            if(headers.put(h,l.substring(sep+1).trim())!=null) return;
        }
        if(!origin.substring(8).equals(headers.get("host")) || headers.containsKey("transfer-encoding")) { reply(client,400,translate.apply("Invalid request"),null); return; }
        URI uri=new URI(first[1]);
        if(uri.isAbsolute() || !path.equals(uri.getPath())) { reply(client,404,translate.apply("Not found"),null); return; }
        if(closed || System.nanoTime()>=deadline) { reply(client,410,translate.apply("Session expired"),null); return; }
        if(first[0].equals("GET")) {
            Map<String,String> p=OAuth.parse(uri.getRawQuery()==null?"":uri.getRawQuery());
            if(p.containsKey("oauth_token") || p.containsKey("oauth_verifier")) {
                if(requestToken.isEmpty() || !constantTimeEquals(requestToken,p.get("oauth_token")) || p.getOrDefault("oauth_verifier","").isEmpty()) {
                    reply(client,400,translate.apply("Authorization does not match this session."),null); return;
                }
                listener.status(translate.apply("Callback received. Testing Flickr access …"));
                try {
                    Map<String,String> access=OAuth.parse(OAuth.get("oauth/access_token",key,secret,requestToken,requestSecret,OAuth.params("oauth_verifier",p.get("oauth_verifier"))));
                    require(access,"oauth_token"); require(access,"oauth_token_secret");
                    String check=OAuth.get("rest",key,secret,access.get("oauth_token"),access.get("oauth_token_secret"),OAuth.params("method","flickr.test.login","format","json","nojsoncallback","1"));
                    // Android listener parses JSON and commits only after stat=ok.
                    listener.complete(key,secret,access.get("oauth_token"),access.get("oauth_token_secret"),check);
                    reply(client,200,translate.apply("<h1>Connected!</h1><p>You can close this page. Login is saved on the TV. The local server is closing.</p>"),null);
                    close();
                } catch(Exception e) {
                    requestToken=""; requestSecret="";
                    listener.status(translate.apply("Could not complete login. Start setup again."));
                    reply(client,400,translate.apply("<h1>Login failed</h1><p>Go back on the TV and start again. Do not share the callback address or keys.</p>"),null);
                }
            } else {
                reply(client,200,translate.apply("<h1>AlbumFrame TV</h1><p>Local setup. Keys are sent only to your TV over HTTPS. Never enter your Flickr password here.</p>")
                    +"<form method='post' action='"+path+"'><label>API Key<input name='key' required maxlength='128' autocomplete='off' autocapitalize='none' spellcheck='false'></label>"
                    +translate.apply("<label>API Secret<input name='secret' type='password' required maxlength='128' autocomplete='off'></label><button>Continue to Flickr</button></form>")
                    +translate.apply("<p>After authorization, Flickr redirects your browser to the TV. Stay on the same home network.</p>"),null);
            }
        } else if(first[0].equals("POST")) {
            if(!origin.equals(headers.get("origin"))) {
                listener.status(translate.apply("Form rejected (F01): browser origin does not match the TV page. Scan the QR code again."));
                reply(client,403,translate.apply("F01: Browser origin does not match the TV page. Open the current QR code in a regular browser tab and try again."),null); return;
            }
            if(attempted) {
                reply(client,409,translate.apply("F02: Form already submitted. Continue in the Flickr tab or start a new session on the TV."),null); return;
            }
            if(!headers.getOrDefault("content-type","").startsWith("application/x-www-form-urlencoded")) {
                reply(client,415,translate.apply("F03: Unsupported form format. Open the QR code in the browser again."),null); return;
            }
            int length=Integer.parseInt(headers.getOrDefault("content-length","0"));
            if(length<1 || length>2048) { reply(client,400,translate.apply("Invalid fields"),null); return; }
            byte[] body=new byte[length]; int n=0,r;
            while(n<length && (r=in.read(body,n,length-n))!=-1) n+=r;
            if(n!=length) return;
            Map<String,String> p=OAuth.parse(new String(body,StandardCharsets.UTF_8)); Arrays.fill(body,(byte)0);
            key=p.getOrDefault("key","").trim(); secret=p.getOrDefault("secret","").trim(); p.clear();
            if(!key.matches("[A-Za-z0-9]{8,128}") || !secret.matches("[A-Za-z0-9]{8,128}")) { reply(client,400,translate.apply("Check API Key and Secret."),null); return; }
            attempted=true;
            listener.status(translate.apply("Keys received. Testing local HTTPS callback with Flickr …"));
            try {
                Map<String,String> token=OAuth.parse(OAuth.get("oauth/request_token",key,secret,"","",OAuth.params("oauth_callback",url())));
                require(token,"oauth_token"); require(token,"oauth_token_secret");
                if(!"true".equals(token.get("oauth_callback_confirmed"))) throw new IOException("Callback not confirmed");
                requestToken=token.get("oauth_token"); requestSecret=token.get("oauth_token_secret");
                listener.status(translate.apply("Flickr accepted the callback. Authorize on your phone."));
                reply(client,303,translate.apply("Continue to Flickr"), "https://www.flickr.com/services/oauth/authorize?perms=read&oauth_token="+OAuth.enc(requestToken));
            } catch(Exception e) {
                key=""; secret="";
                listener.status(translate.apply("Flickr rejected the request or the network failed. Check the keys and TV clock, then start again."));
                reply(client,400,translate.apply("<h1>Request failed</h1><p>Check your keys and the TV date/time. Flickr may have rejected the local callback. Start a new session on the TV.</p>"),null);
            }
        } else reply(client,405,translate.apply("Unsupported method"),null);
    }
    private static boolean constantTimeEquals(String expected,String supplied) {
        if(supplied==null)return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8));
    }
    private static void require(Map<String,String> p,String key) throws IOException { if(p.getOrDefault(key,"").isEmpty()) throw new IOException("Missing OAuth field"); }
    private void reply(Socket s,int status,String body,String location) throws IOException {
        byte[] content=("<!doctype html><html lang='"+language+"'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
            +"<title>AlbumFrame TV</title><style>body{font:18px system-ui;max-width:600px;margin:32px auto;padding:20px;background:#07120f;color:#eee}input,button{display:block;box-sizing:border-box;width:100%;padding:14px;margin:12px 0 24px;font:inherit}button{background:#6fe7d2}</style>"+body+"</html>").getBytes(StandardCharsets.UTF_8);
        // no-referrer makes native form POSTs send Origin: null. Keep the real
        // Origin for local forms without leaking the session path to other hosts.
        // The outbound OAuth redirect still explicitly suppresses its referrer.
        String referrerPolicy=location==null ? "same-origin" : "no-referrer";
        String h="HTTP/1.1 "+status+" Response\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+content.length
            +"\r\nCache-Control: no-store\r\nPragma: no-cache\r\nExpires: 0\r\nReferrer-Policy: "+referrerPolicy+"\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nCross-Origin-Opener-Policy: same-origin\r\nCross-Origin-Resource-Policy: same-origin\r\nPermissions-Policy: camera=(), microphone=(), geolocation=()\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self' https://www.flickr.com; frame-ancestors 'none'; base-uri 'none'\r\nConnection: close\r\n"
            +(location==null?"":"Location: "+location+"\r\n")+"\r\n";
        OutputStream out=s.getOutputStream(); out.write(h.getBytes(StandardCharsets.US_ASCII)); out.write(content); out.flush();
    }
    @Override public void close() {
        closed=true;
        expiry.shutdownNow();
        try { socket.close(); } catch(IOException ignored) {}
        Socket current=active; if(current!=null) try { current.close(); } catch(IOException ignored) {}
        key=""; secret=""; requestToken=""; requestSecret="";
    }
}
