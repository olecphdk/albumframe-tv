package dk.femto.albumframe;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.*;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Foreground slideshow with one-photo memory prefetch and no automatic disk cache. */
final class PhotoPlayer extends FrameLayout {
    private static final int PAGE_SIZE=100;
    private static final int CONTROLS_VISIBLE_MS=4000;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new LinkedBlockingQueue<>());
    private final ImageView currentImage;
    private final ImageView previousImage;
    private final TextView controls;
    private final SlideshowClock clock;
    private final String albumId,albumTitle;
    private final String photoOrder;
    private final Map<Integer,JSONObject> pageCache=new HashMap<>();
    private List<JSONObject> orderedPhotos;
    private FlickrApi api;
    private int total,index,generation,failures;
    private int prefetchedIndex=-1;
    private Bitmap prefetchedBitmap;
    private volatile HttpURLConnection connection;
    private boolean closed,loading;
    private final Runnable advance=()->moveInternal(1,false);
    private final Runnable hideControls;

    private static final class LoadedPhoto {
        final int index,total;
        final Bitmap bitmap;
        LoadedPhoto(int index,int total,Bitmap bitmap){this.index=index;this.total=total;this.bitmap=bitmap;}
    }

    PhotoPlayer(Context context,String albumId,String albumTitle) {
        super(context);this.albumId=albumId;this.albumTitle=albumTitle;
        setBackgroundColor(Color.BLACK);
        photoOrder=PhotoOrder.normalize(context.getSharedPreferences("slideshow",0).getString("photo_order",PhotoOrder.FLICKR));
        index=Math.max(0,context.getSharedPreferences("positions",0).getInt(albumId,0));
        clock=new SlideshowClock(new SlideshowClock.Scheduler(){
            public void cancel(Runnable runnable){ui.removeCallbacks(runnable);}
            public void after(Runnable runnable,long milliseconds){ui.postDelayed(runnable,milliseconds);}
        },advance,context.getSharedPreferences("slideshow",0).getInt("seconds",4));
        previousImage=imageView(context);addView(previousImage,new FrameLayout.LayoutParams(-1,-1));
        currentImage=imageView(context);addView(currentImage,new FrameLayout.LayoutParams(-1,-1));
        controls=new TextView(context);controls.setTextColor(Color.WHITE);controls.setTextSize(17);
        controls.setPadding(24,12,24,12);controls.setBackgroundColor(0xB8000000);
        addView(controls,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        hideControls=()->controls.animate().alpha(0f).setDuration(350).start();
    }

    private static ImageView imageView(Context context){
        ImageView view=new ImageView(context);view.setBackgroundColor(Color.BLACK);view.setScaleType(ImageView.ScaleType.FIT_CENTER);return view;
    }
    void start(){load(true);}
    void move(int delta){
        moveInternal(delta,true);
    }
    private void moveInternal(int delta,boolean revealControls){
        if(closed)return;
        index=total>0?Math.floorMod(index+delta,total):Math.max(0,index+delta);
        if(revealControls)showControls();
        load(revealControls);
    }
    boolean isPaused(){return clock.paused();}
    Bitmap currentBitmap(){return currentImage.getDrawable() instanceof BitmapDrawable?((BitmapDrawable)currentImage.getDrawable()).getBitmap():null;}
    void togglePause(){setPaused(!clock.paused());}
    void setPaused(boolean value){clock.setPaused(value);updateControls();}
    int interval(){return clock.seconds();}
    void setInterval(int seconds){
        clock.setSeconds(seconds);
        getContext().getSharedPreferences("slideshow",0).edit().putInt("seconds",clock.seconds()).apply();
        updateControls();
    }
    void showControls(){
        ui.removeCallbacks(hideControls);controls.animate().cancel();controls.setVisibility(View.VISIBLE);controls.setAlpha(1f);
        if(!clock.paused() && !loading)ui.postDelayed(hideControls,CONTROLS_VISIBLE_MS);
    }
    private void updateControls(){updateControls(true);}
    private void updateControls(boolean reveal){
        controls.setText(albumTitle+" • "+(index+1)+" / "+total+
            (clock.paused()?UiText.text(getContext()," • PAUSED – OK: resume"):UiText.text(getContext()," • Playing • ")+clock.seconds()+UiText.text(getContext()," sec. • OK: pause"))+
            UiText.text(getContext()," • Up: speed • Hold OK: background"));
        if(reveal)showControls();
    }
    private void loadingStatus(boolean reveal){
        loading=true;clock.loading();controls.setText(albumTitle+UiText.text(getContext()," • Loading photo …"));if(reveal)showControls();
    }
    private void load(boolean revealControls){
        loadingStatus(revealControls);final int requestGeneration=++generation,requested=index;
        worker.getQueue().clear();
        if(prefetchedBitmap!=null && prefetchedIndex==requested){
            Bitmap ready=prefetchedBitmap;prefetchedBitmap=null;prefetchedIndex=-1;
            showLoaded(new LoadedPhoto(requested,total,ready),requestGeneration,revealControls);return;
        }
        worker.execute(()->{
            try{LoadedPhoto photo=loadPhoto(requested);ui.post(()->showLoaded(photo,requestGeneration,revealControls));}
            catch(Exception error){ui.post(()->showError(error,requestGeneration));}
        });
    }
    private LoadedPhoto loadPhoto(int requested) throws Exception{
        if(api==null)api=new FlickrApi(getContext());
        if(!PhotoOrder.FLICKR.equals(photoOrder)){
            if(orderedPhotos==null)loadOrderedPhotos();
            int count=orderedPhotos.size();
            if(count==0)throw new IOException(UiText.text(getContext(),"This album has no photos. Videos are not displayed."));
            int safeIndex=requested>=count?0:requested;
            Bitmap bitmap=download(api.imageUrl(orderedPhotos.get(safeIndex)),20*1024*1024,8300000L);
            return new LoadedPhoto(safeIndex,count,bitmap);
        }
        int requestedPage=requested/PAGE_SIZE+1;
        JSONObject pageData=page(requestedPage);
        int available=pageData.getInt("total");
        if(available<=0)throw new IOException(UiText.text(getContext(),"This album has no photos. Videos are not displayed."));
        int safeIndex=requested>=available?0:requested;
        JSONArray photos;
        if(safeIndex/PAGE_SIZE+1==requestedPage)photos=pageData.getJSONArray("photo");
        else{JSONObject first=page(1);photos=first.getJSONArray("photo");available=first.getInt("total");}
        int offset=safeIndex%PAGE_SIZE;
        if(offset>=photos.length())throw new IOException(UiText.text(getContext(),"The album has changed. Open it again from the album list."));
        Bitmap bitmap=download(api.imageUrl(photos.getJSONObject(offset)),20*1024*1024,8300000L);
        return new LoadedPhoto(safeIndex,available,bitmap);
    }
    private void loadOrderedPhotos() throws Exception {
        List<JSONObject> photos=new ArrayList<>();
        JSONObject first=api.photos(albumId,1,PAGE_SIZE);
        int pages=first.optInt("pages",1);
        for(int number=1;number<=pages;number++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            JSONObject result=number==1?first:api.photos(albumId,number,PAGE_SIZE);
            JSONArray pagePhotos=result.getJSONArray("photo");
            for(int offset=0;offset<pagePhotos.length();offset++)photos.add(pagePhotos.getJSONObject(offset));
        }
        PhotoOrder.sort(photos,photoOrder);
        orderedPhotos=photos;
    }
    private JSONObject page(int number) throws Exception{
        JSONObject result=pageCache.get(number);
        if(result==null){result=api.photos(albumId,number);pageCache.put(number,result);}
        return result;
    }
    private void showLoaded(LoadedPhoto photo,int requestGeneration,boolean revealControls){
        if(closed || requestGeneration!=generation){photo.bitmap.recycle();return;}
        index=photo.index;total=photo.total;loading=false;failures=0;
        getContext().getSharedPreferences("positions",0).edit().putInt(albumId,index).apply();
        crossfade(photo.bitmap,requestGeneration,revealControls);updateControls(false);preloadNext(requestGeneration);
    }
    private void crossfade(Bitmap bitmap,int requestGeneration,boolean revealControls){
        int transition=getContext().getSharedPreferences("slideshow",0).getInt("transition_ms",2500);
        currentImage.animate().cancel();previousImage.animate().cancel();
        previousImage.setImageDrawable(currentImage.getDrawable());previousImage.setAlpha(1f);
        currentImage.setImageBitmap(bitmap);
        boolean hasPrevious=previousImage.getDrawable()!=null;
        currentImage.setAlpha(hasPrevious?0f:1f);
        if(hasPrevious)previousImage.animate().alpha(0f).setDuration(transition).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        currentImage.animate().alpha(1f).setDuration(hasPrevious?transition:0).setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(()->{
                previousImage.setImageDrawable(null);previousImage.setAlpha(1f);
                if(!closed && requestGeneration==generation){clock.displayed();if(revealControls)showControls();}
            }).start();
    }
    private void preloadNext(int requestGeneration){
        if(total<2)return;
        final int next=Math.floorMod(index+1,total);
        worker.execute(()->{
            try{
                LoadedPhoto loaded=loadPhoto(next);
                ui.post(()->{
                    if(closed || requestGeneration!=generation || next==index){loaded.bitmap.recycle();return;}
                    recyclePrefetch();prefetchedBitmap=loaded.bitmap;prefetchedIndex=next;
                });
            }catch(Exception ignored){}
        });
    }
    private void showError(Exception error,int requestGeneration){
        if(closed || requestGeneration!=generation)return;
        loading=false;failures++;
        controls.setText(UiText.error(getContext(),message(error))+UiText.text(getContext(),"  Press Right to try the next photo, or Back."));showControls();
        if(!clock.paused() && failures<3 && total>1)ui.postDelayed(advance,5000);
    }
    static String message(Exception error){
        if(error instanceof SocketTimeoutException)return "The connection to Flickr timed out.";
        if(error instanceof IOException && error.getMessage()!=null && !error.getMessage().contains("://"))return error.getMessage();
        return "Could not load from Flickr. Check your network and try again.";
    }
    private Bitmap download(String url,int maximumBytes,long maximumPixels) throws Exception{
        byte[] bytes;
        for(int redirect=0;;redirect++){
            if(!FlickrApi.allowedImage(url))throw new IOException(UiText.text(getContext(),"Unexpected photo address from Flickr."));
            HttpURLConnection current=(HttpURLConnection)new URL(url).openConnection();connection=current;
            current.setConnectTimeout(15000);current.setReadTimeout(15000);current.setInstanceFollowRedirects(false);
            try{
                int status=current.getResponseCode();
                if(status>=300 && status<400 && redirect<3){url=new URL(new URL(url),current.getHeaderField("Location")).toString();continue;}
                if(status!=200)throw new IOException(UiText.text(getContext(),"Could not download the photo (HTTP ")+status+").");
                ByteArrayOutputStream output=new ByteArrayOutputStream();
                try(InputStream input=current.getInputStream()){
                    byte[] buffer=new byte[8192];int count;
                    while((count=input.read(buffer))!=-1){
                        if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                        if(output.size()+count>maximumBytes)throw new IOException(UiText.text(getContext(),"The photo file is too large (over 20 MB)."));
                        output.write(buffer,0,count);
                    }
                }
                bytes=output.toByteArray();break;
            }finally{current.disconnect();connection=null;}
        }
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(options.outWidth<=0 || options.outHeight<=0)throw new IOException(UiText.text(getContext(),"Could not read the photo format."));
        options.inSampleSize=1;
        while((long)(options.outWidth/options.inSampleSize)*(options.outHeight/options.inSampleSize)>maximumPixels)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;
        Bitmap result=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(result==null)throw new IOException(UiText.text(getContext(),"Could not decode the photo."));
        return result;
    }
    private void recyclePrefetch(){if(prefetchedBitmap!=null){prefetchedBitmap.recycle();prefetchedBitmap=null;prefetchedIndex=-1;}}
    void close(){
        clock.close();closed=true;generation++;ui.removeCallbacksAndMessages(null);worker.shutdownNow();
        currentImage.animate().cancel();previousImage.animate().cancel();recyclePrefetch();
        HttpURLConnection current=connection;if(current!=null)current.disconnect();
        currentImage.setImageDrawable(null);previousImage.setImageDrawable(null);pageCache.clear();orderedPhotos=null;
    }
}
