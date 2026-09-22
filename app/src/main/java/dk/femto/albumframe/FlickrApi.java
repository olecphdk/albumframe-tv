package dk.femto.albumframe;

import android.content.Context;
import org.json.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/** Signed calls for the authenticated owner, including private photos. */
final class FlickrApi {
    interface Transport { String get(Map<String,String> params) throws Exception; }
    private final Transport transport;
    private String owner;
    FlickrApi(Context context) throws Exception {
        JSONObject c=CredentialStore.load(context);
        transport=p->OAuth.get("rest",c.getString("key"),c.getString("secret"),c.getString("token"),c.getString("tokenSecret"),p);
    }
    FlickrApi(Transport transport) { this.transport=transport; }
    private JSONObject call(String method,Map<String,String> args) throws Exception {
        if(Thread.currentThread().isInterrupted()) throw new InterruptedException();
        Map<String,String> p=new HashMap<>(args);
        p.put("method",method); p.put("format","json"); p.put("nojsoncallback","1");
        JSONObject reply=new JSONObject(transport.get(p));
        if(!"ok".equals(reply.optString("stat"))) {
            int code=reply.optInt("code");
            if(code==98 || code==99 || code==100) throw new IOException("Flickr login or API key is no longer valid. Connect Flickr again ("+code+").");
            throw new IOException("Flickr error "+code+". Try again later.");
        }
        return reply;
    }
    private String owner() throws Exception {
        if(owner==null) owner=call("flickr.test.login",Collections.emptyMap()).getJSONObject("user").getString("id");
        return owner;
    }
    JSONObject albums(int page) throws Exception {
        return call("flickr.photosets.getList",OAuth.params("user_id",owner(),"page",Integer.toString(page),"per_page","50",
            "primary_photo_extras","url_q,url_m,url_s,url_t")).getJSONObject("photosets");
    }
    JSONObject photos(String album,int page) throws Exception {
        return call("flickr.photosets.getPhotos",OAuth.params("user_id",owner(),"photoset_id",album,
            "page",Integer.toString(page),"per_page","100","media","photos",
            "extras","url_k,url_h,url_l,url_c,url_m,media")).getJSONObject("photoset");
    }
    String imageUrl(JSONObject photo) throws Exception {
        for(String size:new String[]{"url_k","url_h","url_l","url_c","url_m"}) {
            String u=photo.optString(size);
            if(!u.isEmpty() && allowedImage(u)) return u;
        }
        JSONArray sizes=call("flickr.photos.getSizes",OAuth.params("photo_id",photo.getString("id"))).getJSONObject("sizes").getJSONArray("size");
        String best=""; long bestArea=0,smallest=Long.MAX_VALUE; String fallback="";
        for(int i=0;i<sizes.length();i++) {
            JSONObject s=sizes.getJSONObject(i);
            if(!"photo".equals(s.optString("media","photo"))) continue;
            String url=s.optString("source"); if(!allowedImage(url)) continue;
            int w=s.optInt("width"),h=s.optInt("height"); long area=(long)w*h;
            if(area<=0) continue;
            if(area<smallest) { smallest=area; fallback=url; }
            if(Math.max(w,h)<=3840 && area>bestArea) {bestArea=area; best=url;}
        }
        if(best.isEmpty()) best=fallback;
        if(best.isEmpty()) throw new IOException("Flickr returned no available photo address.");
        return best;
    }
    static String albumCoverUrl(JSONObject album) {
        for(String size:new String[]{"url_q","url_m","url_s","url_t"}) {
            String url=album.optString(size);
            if(allowedImage(url))return url;
        }
        String server=album.optString("server"),photo=album.optString("primary"),secret=album.optString("secret");
        if(!server.matches("[0-9]+") || !photo.matches("[0-9]+") || !secret.matches("[A-Za-z0-9]+"))return "";
        String constructed="https://live.staticflickr.com/"+server+"/"+photo+"_"+secret+"_q.jpg";
        return allowedImage(constructed)?constructed:"";
    }
    static boolean allowedImage(String url) {
        try {
            URI u=URI.create(url); String h=u.getHost();
            return "https".equals(u.getScheme()) && h!=null && (h.equals("staticflickr.com") || h.endsWith(".staticflickr.com"))
                && u.getUserInfo()==null && (u.getPort()==-1 || u.getPort()==443);
        } catch(IllegalArgumentException e) {return false;}
    }
}
