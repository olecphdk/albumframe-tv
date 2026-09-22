package dk.femto.albumframe;

import android.graphics.*;
import java.io.*;
import java.net.*;

/** Small bounded Flickr image download used only for album covers. */
final class ThumbnailLoader {
    static Bitmap download(String url) throws Exception{
        byte[] bytes;
        for(int redirect=0;;redirect++){
            if(!FlickrApi.allowedImage(url))throw new IOException("Unexpected Flickr image host");
            HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setInstanceFollowRedirects(false);
            try{
                int status=connection.getResponseCode();
                if(status>=300 && status<400 && redirect<3){url=new URL(new URL(url),connection.getHeaderField("Location")).toString();continue;}
                if(status!=200)throw new IOException("Thumbnail HTTP "+status);
                ByteArrayOutputStream output=new ByteArrayOutputStream();
                try(InputStream input=connection.getInputStream()){
                    byte[] buffer=new byte[4096];int count;
                    while((count=input.read(buffer))!=-1){
                        if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                        if(output.size()+count>4*1024*1024)throw new IOException("Thumbnail too large");
                        output.write(buffer,0,count);
                    }
                }
                bytes=output.toByteArray();break;
            }finally{connection.disconnect();}
        }
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(options.outWidth<=0 || options.outHeight<=0)throw new IOException("Invalid thumbnail");
        options.inSampleSize=1;
        while((long)(options.outWidth/options.inSampleSize)*(options.outHeight/options.inSampleSize)>500000L)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;
        Bitmap result=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(result==null)throw new IOException("Invalid thumbnail");
        return result;
    }
}
