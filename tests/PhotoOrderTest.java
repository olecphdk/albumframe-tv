package dk.femto.albumframe;

import org.json.JSONObject;
import java.util.*;

/** Dates are sorted across API pages; absent dates follow known dates. */
public final class PhotoOrderTest {
    private static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);}
    private static List<JSONObject> photos(){
        return new ArrayList<>(Arrays.asList(
            new JSONObject().put("id","one").put("datetaken","2021-08-03 10:00:00").put("dateupload","300"),
            new JSONObject().put("id","two").put("datetaken","2018-07-01 11:00:00").put("dateupload","100"),
            new JSONObject().put("id","three").put("datetaken","2021-08-03 10:00:00").put("dateupload","200"),
            new JSONObject().put("id","missing")));
    }
    private static String ids(List<JSONObject> photos){
        StringJoiner ids=new StringJoiner(",");for(JSONObject photo:photos)ids.add(photo.optString("id"));return ids.toString();
    }
    private static void order(String mode,String expected){
        List<JSONObject> photos=photos();PhotoOrder.sort(photos,mode);
        check(expected.equals(ids(photos)),mode+": "+ids(photos));
    }
    public static void main(String[] args){
        order(PhotoOrder.FLICKR,"one,two,three,missing");
        order(PhotoOrder.TAKEN_OLDEST,"two,one,three,missing");
        order(PhotoOrder.TAKEN_NEWEST,"one,three,two,missing");
        order(PhotoOrder.UPLOADED_OLDEST,"two,three,one,missing");
        order(PhotoOrder.UPLOADED_NEWEST,"one,three,two,missing");
        check(PhotoOrder.FLICKR.equals(PhotoOrder.normalize("unexpected")),"unknown saved choice");
        System.out.println("PASS local date ordering, missing dates, stable ties and default");
    }
}
