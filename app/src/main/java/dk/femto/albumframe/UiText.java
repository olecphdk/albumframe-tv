package dk.femto.albumframe;

import android.content.Context;
import android.content.res.Resources;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;

/** English source strings with Danish translations kept in a UTF-8 asset. */
final class UiText {
    private static Map<String,String> danish;
    static String language(Context context) {
        String selection=context.getSharedPreferences("appearance",0).getString("language","system");
        String system=Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();
        return resolveLanguage(selection,system);
    }
    static String resolveLanguage(String selection,String system) {
        return "da".equals("system".equals(selection)?system:selection)?"da":"en";
    }
    private static synchronized Map<String,String> translations(Context context) {
        if(danish==null){
            Map<String,String> result=new HashMap<>();
            try(InputStream input=context.getAssets().open("da.json")){
                ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
                while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
                JSONObject json=new JSONObject(output.toString("UTF-8"));
                Iterator<String> keys=json.keys();while(keys.hasNext()){String key=keys.next();result.put(key,json.getString(key));}
            }catch(Exception ignored){}
            danish=result;
        }
        return danish;
    }
    static String text(Context context,String english) {
        return "da".equals(language(context))?translations(context).getOrDefault(english,english):english;
    }
    /** Only exception messages pass here; never translate user album titles. */
    static String error(Context context,String message) {
        if(!"da".equals(language(context)))return message;
        List<String> keys=new ArrayList<>(translations(context).keySet());
        keys.sort((first,second)->Integer.compare(second.length(),first.length()));
        for(String key:keys)if(message.startsWith(key))return text(context,key)+message.substring(key.length());
        return message;
    }
}
