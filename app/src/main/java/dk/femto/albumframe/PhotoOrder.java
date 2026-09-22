package dk.femto.albumframe;

import org.json.JSONObject;
import java.util.*;

/** Orders album metadata locally; Flickr's album-photo API has no sort argument. */
final class PhotoOrder {
    static final String FLICKR="flickr",TAKEN_OLDEST="taken_asc",TAKEN_NEWEST="taken_desc",
        UPLOADED_OLDEST="uploaded_asc",UPLOADED_NEWEST="uploaded_desc";
    static final String[] CHOICES={FLICKR,TAKEN_OLDEST,TAKEN_NEWEST,UPLOADED_OLDEST,UPLOADED_NEWEST};
    private PhotoOrder(){}
    static String normalize(String value){
        for(String choice:CHOICES)if(choice.equals(value))return choice;
        return FLICKR;
    }
    static void sort(List<JSONObject> photos,String preference){
        String order=normalize(preference);
        if(FLICKR.equals(order))return;
        boolean taken=order.startsWith("taken_");
        boolean descending=order.endsWith("desc");
        // List.sort is stable: equal or missing dates retain their Flickr order.
        photos.sort((first,second)->{
            String a=key(first,taken),b=key(second,taken);
            if(a.isEmpty())return b.isEmpty()?0:1;
            if(b.isEmpty())return -1;
            int compare=a.compareTo(b);
            return descending?-compare:compare;
        });
    }
    private static String key(JSONObject photo,boolean taken){
        if(taken){
            String date=photo.optString("datetaken","");
            return date.startsWith("0000-")?"":date;
        }
        long uploaded=photo.optLong("dateupload",0);
        return uploaded>0?String.format(Locale.ROOT,"%020d",uploaded):"";
    }
}
