package dk.femto.albumframe;
import org.json.*;
import java.io.IOException;
import java.util.*;

/** Offline fixtures validate outgoing authenticated API requests and URL policy. */
public final class FlickrTest {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        List<Map<String,String>> requests=new ArrayList<>();
        FlickrApi api=new FlickrApi(p->{
            requests.add(new HashMap<>(p));
            switch(p.get("method")) {
                case "flickr.test.login":return "{\"stat\":\"ok\",\"user\":{\"id\":\"123@N01\"}}";
                case "flickr.photosets.getList":return "{\"stat\":\"ok\",\"photosets\":{\"page\":2,\"pages\":2,\"photoset\":[]}}";
                case "flickr.photosets.getPhotos":return "{\"stat\":\"ok\",\"photoset\":{\"total\":201,\"photo\":[{\"id\":\"1\",\"url_k\":\"https://live.staticflickr.com/1/a.jpg\"}]}}";
                case "flickr.photos.getSizes":return "{\"stat\":\"ok\",\"sizes\":{\"size\":[{\"width\":8000,\"height\":6000,\"source\":\"https://live.staticflickr.com/original.jpg\"},{\"width\":2048,\"height\":1536,\"source\":\"https://live.staticflickr.com/large.jpg\"}]}}";
                default:throw new AssertionError(p);
            }
        });
        check(api.albums(2).getJSONArray("photoset").length()==0,"empty albums");
        Map<String,String> list=requests.get(1);
        check("123@N01".equals(list.get("user_id")) && "2".equals(list.get("page")),"owner and album page");
        check("url_q,url_m,url_s,url_t".equals(list.get("primary_photo_extras")),"album cover URLs");
        JSONObject album=new JSONObject().put("primary","5504567858").put("secret","017804c585").put("server","5174");
        check("https://live.staticflickr.com/5174/5504567858_017804c585_q.jpg".equals(FlickrApi.albumCoverUrl(album)),"constructed album cover");
        album.put("url_q","https://live.staticflickr.com/direct.jpg");
        check(FlickrApi.albumCoverUrl(album).endsWith("/direct.jpg"),"direct album cover preferred");
        check(FlickrApi.albumCoverUrl(new JSONObject().put("primary","../bad")).isEmpty(),"invalid cover metadata blocked");
        JSONObject photos=api.photos("private-album",3);
        Map<String,String> p=requests.get(2);
        check("3".equals(p.get("page")) && "100".equals(p.get("per_page")),"photo pagination");
        check(!p.containsKey("privacy_filter") && "photos".equals(p.get("media")),"private images and media");
        check("private-album".equals(p.get("photoset_id")),"album id");
        check(p.get("extras").contains("date_taken") && p.get("extras").contains("date_upload"),"sorting metadata");
        api.photos("private-album",2,500);
        check("500".equals(requests.get(3).get("per_page")) && "2".equals(requests.get(3).get("page")),"sorted album pagination");
        check(api.imageUrl(photos.getJSONArray("photo").getJSONObject(0)).endsWith("/a.jpg"),"direct image");
        check(api.imageUrl(new JSONObject().put("id","1")).endsWith("/large.jpg"),"size fallback");
        for(String url:new String[]{"http://live.staticflickr.com/a","https://staticflickr.com.evil.test/a","https://127.0.0.1/a","https://user@live.staticflickr.com/a","https://live.staticflickr.com:444/a"})
            check(!FlickrApi.allowedImage(url),"blocked url "+url);
        FlickrApi expired=new FlickrApi(q->"{\"stat\":\"fail\",\"code\":98}");
        try{expired.albums(1);throw new AssertionError("expired token accepted");}
        catch(IOException e){check(e.getMessage().contains("Connect Flickr again"),"reconnect message");}
        System.out.println("PASS owner, pagination, covers, privacy, media, URLs, size fallback, empty albums, expired login");
    }
}
