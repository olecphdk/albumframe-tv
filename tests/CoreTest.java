package dk.femto.albumframe;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Desktop tests, no real Flickr credentials and no external network calls. */
public final class CoreTest {
    private static void check(boolean b,String label) { if(!b) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        Map<String,String> p=new HashMap<>();
        p.put("file","vacation.jpg"); p.put("size","original"); p.put("oauth_consumer_key","dpf43f3p2l4k3l03");
        p.put("oauth_token","nnch734d00sl2jdk"); p.put("oauth_nonce","kllo9940pd9333jh");
        p.put("oauth_timestamp","1191242096"); p.put("oauth_signature_method","HMAC-SHA1"); p.put("oauth_version","1.0");
        check(OAuth.signature("http://photos.example.net/photos",p,"kd94hf93k423kf44","pfkkdhi9sl3r4s00").equals("tR3+Ty81lMeYAr/Fid0kMTYa/WM="),"RFC OAuth signature");
        check(OAuth.enc("a +*/~").equals("a%20%2B%2A%2F~"),"RFC3986 encoding");
        check(OAuth.parse("token=a%2Bb&x=c+d").get("token").equals("a+b"),"form decoding");
        boolean duplicate=false; try { OAuth.parse("a=1&a=2"); } catch(IOException e) { duplicate=true; }
        check(duplicate,"duplicate rejection");
        String qr="https://192.168.100.222:54321/abcdefghijklmnopqrstuv";
        writeQr(qr,"qr-test.png");
        writeQr("a".repeat(78),"qr-max.png");
        boolean tooLong=false; try { SmallQr.encode("a".repeat(79)); } catch(IllegalArgumentException e) { tooLong=true; }
        check(tooLong,"QR capacity");
        // Test-only trust manager: verify self-signature and validity, not a public CA.
        SSLContext client=SSLContext.getInstance("TLSv1.2");
        client.init(null,new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
            public void checkClientTrusted(X509Certificate[] c,String a){}
            public void checkServerTrusted(X509Certificate[] c,String a) throws java.security.cert.CertificateException {
                try { c[0].checkValidity(); c[0].verify(c[0].getPublicKey()); }
                catch(Exception e){throw new java.security.cert.CertificateException(e);}
            }
        }},null);
        PairingServer server=new PairingServer("127.0.0.1",new PairingServer.Listener(){
            public void status(String m){}
            public void complete(String k,String s,String t,String ts,String u){throw new AssertionError("Unexpected login");}
        });
        server.start();
        URI base=URI.create(server.url());
        String page=request(client,base,"GET",base.getPath(),null);
        check(page.startsWith("HTTP/1.1 200"),"TLS GET form");
        check(page.contains("name='secret' type='password'"),"password input");
        check(page.contains("Cache-Control: no-store"),"no caching");
        check(page.contains("Pragma: no-cache\r\n") && page.contains("Expires: 0\r\n"),"legacy cache prevention");
        check(page.contains("Referrer-Policy: same-origin\r\n"),"form preserves same-origin POST Origin");
        check(page.contains("X-Frame-Options: DENY\r\n"),"legacy framing protection");
        check(page.contains("Cross-Origin-Opener-Policy: same-origin\r\n"),"opener isolation");
        check(page.contains("Cross-Origin-Resource-Policy: same-origin\r\n"),"resource isolation");
        check(page.contains("Permissions-Policy: camera=(), microphone=(), geolocation=()\r\n"),"browser capability denial");
        check(page.contains("frame-ancestors 'none'"),"clickjacking protection");
        check(request(client,base,"GET","/wrong",null).startsWith("HTTP/1.1 404"),"unguessable session path");
        check(request(client,base,"POST",base.getPath(),"https://evil.example").startsWith("HTTP/1.1 403"),"CSRF rejection");
        check(request(client,base,"POST",base.getPath(),"null").contains("F01:"),"opaque origin remains rejected");
        check(request(client,base,"POST",base.getPath(),null).contains("F01:"),"missing origin remains rejected");
        // Deliberately invalid keys stop at field validation, proving that a
        // legitimate form submission passes Origin checks without contacting Flickr.
        String local=base.getScheme()+"://"+base.getAuthority();
        String form=request(client,base,"POST",base.getPath(),local,"key=x&secret=y","application/x-www-form-urlencoded");
        check(form.startsWith("HTTP/1.1 400") && form.contains("Check API Key"),"same-origin form reaches credential validation");
        check(!form.contains("F01:"),"no false Origin rejection");
        check(request(client,base,"POST",base.getPath(),local,"{}","application/json").contains("F03:"),"distinct invalid content type");
        check(request(client,base,"GET",base.getPath()+"?oauth_token=wrong&oauth_verifier=wrong",null).startsWith("HTTP/1.1 400"),"callback token binding");
        check(request(client,base,"PUT",base.getPath(),null).startsWith("HTTP/1.1 405"),"method allowlist");
        server.close();
        boolean stopped=false;
        for(int attempt=0;attempt<20 && !stopped;attempt++) {
            try(Socket s=new Socket("127.0.0.1",base.getPort())) { Thread.sleep(10); }
            catch(IOException e){stopped=true;}
        }
        check(stopped,"listener closed");
        System.out.println("PASS: OAuth RFC vector, encoding, duplicate rejection, QR bounds, self-signed TLS, form, headers, CSRF, callback binding, method allowlist, shutdown.");
    }
    private static String request(SSLContext context,URI base,String method,String path,String origin) throws Exception {
        return request(context,base,method,path,origin,"",null);
    }
    private static String request(SSLContext context,URI base,String method,String path,String origin,String body,String contentType) throws Exception {
        try(SSLSocket s=(SSLSocket)context.getSocketFactory().createSocket("127.0.0.1",base.getPort())) {
            s.setSoTimeout(5000);
            String r=method+" "+path+" HTTP/1.1\r\nHost: "+base.getAuthority()+"\r\n"+(origin==null?"":"Origin: "+origin+"\r\n")
                +(contentType==null?"":"Content-Type: "+contentType+"\r\n")+"Content-Length: "+body.length()+"\r\n\r\n"+body;
            s.getOutputStream().write(r.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return new String(s.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    private static void writeQr(String text,String file) throws Exception {
        boolean[][] matrix=SmallQr.encode(text);
        BufferedImage img=new BufferedImage(410,410,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<410;y++) for(int x=0;x<410;x++) {
            int a=x/10-4,b=y/10-4;
            boolean dark=a>=0&&b>=0&&a<33&&b<33&&matrix[b][a];
            img.setRGB(x,y,dark?0:0xffffff);
        }
        ImageIO.write(img,"png",new File(file));
    }
}
