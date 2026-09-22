package dk.femto.albumframe;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.Bitmap;
import android.graphics.Color;
import org.json.*;
import java.util.concurrent.*;

/** D-pad friendly paged album picker, retaining Flickr's own album ordering. */
public final class AlbumActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService thumbnailWorker=Executors.newFixedThreadPool(3);
    private FlickrApi api;
    private int generation,page=1;
    private String selectedAlbumId="";
    private boolean stopped;
    private PhotoPlayer player;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if(b!=null){page=b.getInt("page",1);selectedAlbumId=b.getString("selectedAlbumId","");}
    }
    @Override protected void onSaveInstanceState(Bundle state){
        state.putInt("page",page);state.putString("selectedAlbumId",selectedAlbumId);super.onSaveInstanceState(state);
    }
    @Override protected void onStart() {super.onStart();stopped=false;showAlbums(page);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private Button button(String text,Runnable action) {
        Button b=new Button(this);b.setText(text);b.setTextSize(18);b.setAllCaps(false);
        b.setMinHeight(dp(56)); b.setPadding(dp(16),dp(10),dp(16),dp(10));
        b.setOnClickListener(v->action.run());AppAppearance.styleButton(b);return b;
    }
    private TextView text(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(20);t.setTextColor(Color.WHITE);return t;}
    private Button albumRow(LinearLayout list,JSONObject album,String id,String title,int current) {
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setImageResource(dk.femto.albumframe.R.drawable.albumframe_icon);
        LinearLayout.LayoutParams coverParams=new LinearLayout.LayoutParams(dp(120),dp(76));coverParams.setMargins(0,dp(4),dp(14),dp(4));row.addView(cover,coverParams);
        Button item=button(title+"  •  "+album.optInt("photos")+UiText.text(this," photos"),()->open(id,title));
        row.addView(item,new LinearLayout.LayoutParams(0,dp(68),1));
        String url=FlickrApi.albumCoverUrl(album);
        if(!url.isEmpty())thumbnailWorker.submit(()->{
            try{
                Bitmap bitmap=ThumbnailLoader.download(url);
                runOnUiThread(()->{if(stopped || current!=generation)bitmap.recycle();else cover.setImageBitmap(bitmap);});
            }catch(Exception ignored){}
        });
        list.addView(row);
        return item;
    }
    private void showAlbums(int requested) {
        if(player!=null){player.close();player=null;}
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        page=requested;int current=++generation;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(32),dp(24),dp(32),dp(24));root.setBackgroundColor(Color.TRANSPARENT);
        root.addView(text(UiText.text(this,"Choose Flickr album • page ")+page));
        TextView status=text(UiText.text(this,"Loading your albums …"));root.addView(status);
        Button back=button(UiText.text(this,"Back"),this::finish);root.addView(back);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(AppAppearance.menu(this,scroll));back.requestFocus();
        worker.submit(()->{
            try {
                if(api==null) api=new FlickrApi(this);
                JSONObject result=api.albums(requested);JSONArray albums=result.getJSONArray("photoset");
                runOnUiThread(()->{
                    if(stopped || current!=generation)return;
                    status.setText(albums.length()==0?UiText.text(this,"No albums found. Create an album on Flickr first."):UiText.text(this,"Choose with arrows and OK. Photos are shown inside the app."));
                    try {
                        Button first=null,selected=null;
                        for(int i=0;i<albums.length();i++) {
                            JSONObject a=albums.getJSONObject(i);String id=a.getString("id"),title=a.getJSONObject("title").optString("_content","Album");
                            Button item=albumRow(list,a,id,title,current);
                            if(first==null)first=item;
                            if(id.equals(selectedAlbumId))selected=item;
                        }
                        Button focus=selected!=null?selected:first;
                        if(focus!=null)focus.post(()->{if(!stopped && current==generation)focus.requestFocus();});
                        if(requested>1) list.addView(button(UiText.text(this,"Previous page"),()->showAlbums(requested-1)));
                        if(requested<result.optInt("pages",1)) list.addView(button(UiText.text(this,"Next page"),()->showAlbums(requested+1)));
                    } catch(JSONException e){status.setText(UiText.text(this,"Could not read the Flickr album list."));}
                });
            }catch(Exception e){runOnUiThread(()->{if(!stopped && current==generation){status.setText(UiText.error(this,PhotoPlayer.message(e)));list.addView(button(UiText.text(this,"Try again"),()->showAlbums(requested)));}});}
        });
    }
    private void open(String id,String title) {
        generation++;
        selectedAlbumId=id;
        getSharedPreferences("album",0).edit().putString("id",id).putString("title",title).apply();
        player=new PhotoPlayer(this,id,title);setContentView(player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        player.start();
    }
    private int pressedKey=-1;
    private boolean wasPaused;
    private android.graphics.Bitmap heldBitmap;
    private void chooseBackground() {
        final PhotoPlayer target=player;
        final android.graphics.Bitmap bitmap=heldBitmap;
        if(bitmap==null){target.setPaused(wasPaused);Toast.makeText(this,UiText.text(this,"Wait for the photo to finish loading"),Toast.LENGTH_SHORT).show();return;}
        AppAppearance.dialog(this).setMessage(UiText.text(this,"Use this photo as the app background?"))
            .setPositiveButton(UiText.text(this,"Use background"),(dialog,which)->{
                worker.submit(()->{
                    String result;
                    try{AppAppearance.saveBackground(getApplicationContext(),bitmap);result="Background saved";}
                    catch(Exception error){result="Could not save background";}
                    final String message=result;
                    runOnUiThread(()->{if(!stopped)Toast.makeText(this,UiText.text(this,message),Toast.LENGTH_SHORT).show();});
                });
            }).setNegativeButton(UiText.text(this,"Cancel"),null)
            .setOnDismissListener(dialog->{if(player==target)target.setPaused(wasPaused);}).show();
    }
    private void chooseInterval() {
        final PhotoPlayer target=player;
        if(target==null)return;
        final int[] values={1,2,4,8,16,32};
        String[] labels=new String[values.length];int selected=2;
        for(int i=0;i<values.length;i++){labels[i]=values[i]+UiText.text(this," seconds");if(values[i]==target.interval())selected=i;}
        AppAppearance.dialog(this).setTitle(UiText.text(this,"Time between photos"))
            .setSingleChoiceItems(labels,selected,(dialog,which)->{
                if(player==target)target.setInterval(values[which]);dialog.dismiss();
            }).setNegativeButton(UiText.text(this,"Back"),null).show();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code=event.getKeyCode();
        if(player!=null && (code==KeyEvent.KEYCODE_BACK || code==KeyEvent.KEYCODE_DPAD_RIGHT ||
            code==KeyEvent.KEYCODE_DPAD_LEFT || code==KeyEvent.KEYCODE_DPAD_CENTER ||
            code==KeyEvent.KEYCODE_ENTER || code==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
            code==KeyEvent.KEYCODE_MEDIA_PLAY || code==KeyEvent.KEYCODE_MEDIA_PAUSE ||
            code==KeyEvent.KEYCODE_DPAD_UP || code==KeyEvent.KEYCODE_MENU)) {
            player.showControls();
            // Act once per complete press. The release that opens an album belongs
            // to its button and must not immediately pause the new slideshow.
            if(event.getAction()==KeyEvent.ACTION_DOWN && event.getRepeatCount()==0){
                pressedKey=code;
                if(code==KeyEvent.KEYCODE_DPAD_CENTER || code==KeyEvent.KEYCODE_ENTER){wasPaused=player.isPaused();heldBitmap=player.currentBitmap();player.setPaused(true);}
            }
            if(event.getAction()==KeyEvent.ACTION_UP){
                boolean act=pressedKey==code && !event.isCanceled();pressedKey=-1;
                if(!act && (code==KeyEvent.KEYCODE_DPAD_CENTER || code==KeyEvent.KEYCODE_ENTER))player.setPaused(wasPaused);
                if(act){
                    if(code==KeyEvent.KEYCODE_BACK)showAlbums(page);
                    else if(code==KeyEvent.KEYCODE_DPAD_RIGHT)player.move(1);
                    else if(code==KeyEvent.KEYCODE_DPAD_LEFT)player.move(-1);
                    else if(code==KeyEvent.KEYCODE_DPAD_UP || code==KeyEvent.KEYCODE_MENU)chooseInterval();
                    else if(code==KeyEvent.KEYCODE_MEDIA_PLAY)player.setPaused(false);
                    else if(code==KeyEvent.KEYCODE_MEDIA_PAUSE)player.setPaused(true);
                    else if(code==KeyEvent.KEYCODE_DPAD_CENTER || code==KeyEvent.KEYCODE_ENTER){
                        if(event.getEventTime()-event.getDownTime()>=650)chooseBackground();
                        else player.setPaused(!wasPaused);
                    }else player.togglePause();
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override protected void onStop(){stopped=true;generation++;if(player!=null){player.close();player=null;}super.onStop();}
    @Override protected void onDestroy(){worker.shutdownNow();thumbnailWorker.shutdownNow();super.onDestroy();}
}
