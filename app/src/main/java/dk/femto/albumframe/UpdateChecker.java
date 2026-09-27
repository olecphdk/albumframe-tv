package dk.femto.albumframe;

import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/** Checks the public stable release and hands an APK to Android's installer. */
final class UpdateChecker {
    private static final String RELEASE="https://api.github.com/repos/olecphdk/albumframe-tv/releases/latest";
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final Handler UI=new Handler(Looper.getMainLooper());
    interface Callback { void complete(Release release,Exception error); }
    static final class Release {
        final String version,url,notes;
        Release(String version,String url,String notes){this.version=version;this.url=url;this.notes=notes;}
    }
    static void check(Callback callback){WORKER.execute(()->{
        try{
            JSONObject json=new JSONObject(new String(read(RELEASE,1024*1024),java.nio.charset.StandardCharsets.UTF_8));
            String tag=json.getString("tag_name");
            if(!tag.matches("v?[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IOException("Unexpected release version");
            JSONArray assets=json.getJSONArray("assets");String url=null;
            for(int i=0;i<assets.length();i++){
                JSONObject asset=assets.getJSONObject(i);
                if("AlbumFrame-TV.apk".equals(asset.optString("name"))){url=asset.getString("browser_download_url");break;}
            }
            if(url==null)throw new IOException("No signed APK in latest release");
            if(!url.startsWith("https://github.com/olecphdk/albumframe-tv/releases/download/"))throw new IOException("Unexpected APK address");
            Release release=new Release(tag,url,json.optString("body",""));UI.post(()->callback.complete(release,null));
        }catch(Exception error){UI.post(()->callback.complete(null,error));}
    });}
    static boolean newer(String candidate,String installed){
        String[] a=candidate.replaceFirst("^v","").split("\\.");String[] b=installed.replaceFirst("^v","").split("\\.");
        if(a.length!=3||b.length!=3)return false;
        try{for(int i=0;i<3;i++){int d=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i]));if(d!=0)return d>0;}}catch(NumberFormatException error){return false;}
        return false;
    }
    static void install(Activity activity,Release release,Callback callback){WORKER.execute(()->{
        try{
            File folder=new File(activity.getCacheDir(),"updates");if(!folder.exists()&&!folder.mkdirs())throw new IOException("Cannot create update directory");
            File apk=new File(folder,"AlbumFrame-TV.apk"),temporary=new File(folder,"AlbumFrame-TV.part");
            try(FileOutputStream output=new FileOutputStream(temporary)){download(release.url,output,80*1024*1024);}
            try(FileInputStream input=new FileInputStream(temporary)){byte[] header=new byte[4];if(input.read(header)!=4||header[0]!='P'||header[1]!='K')throw new IOException("Invalid APK download");}
            if(!temporary.renameTo(apk))throw new IOException("Cannot save update");
            UI.post(()->{
                try{
                    Uri uri=Uri.parse("content://"+activity.getPackageName()+".updates/apk");
                    Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if(android.os.Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
                        activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));
                        callback.complete(null,new IOException("Allow installation for AlbumFrame, then choose Update again."));
                    }else activity.startActivity(intent);
                }catch(Exception error){callback.complete(null,error);}
            });
        }catch(Exception error){UI.post(()->callback.complete(null,error));}
    });}
    private static byte[] read(String address,int limit) throws Exception{
        ByteArrayOutputStream output=new ByteArrayOutputStream();download(address,output,limit);return output.toByteArray();
    }
    private static void download(String address,OutputStream output,int limit) throws Exception{
        URL url=new URL(address);if(!"https".equals(url.getProtocol()))throw new IOException("HTTPS required");
        URLConnection connection=url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent","AlbumFrame-TV");
        try(InputStream input=connection.getInputStream()){
            byte[] buffer=new byte[8192];int n;long total=0;while((n=input.read(buffer))!=-1){total+=n;if(total>limit)throw new IOException("Download too large");output.write(buffer,0,n);}
        }
    }
}
