package dk.femto.albumframe;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import java.net.Inet4Address;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int SETTINGS_REQUEST=1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int scene;
    private boolean slideshow;
    private DemoPhotoView photoView;
    private TextView caption;
    private volatile PairingServer pairing;
    private int pairingGeneration;
    private boolean pairingVisible;
    private void stopPairing() {
        pairingGeneration++;
        pairingVisible=false;
        PairingServer old=pairing; pairing=null;
        if(old!=null) old.close();
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private final Runnable advance = new Runnable() {
        @Override public void run() {
            if (!slideshow) return;
            scene++;
            photoView.setScene(scene);
            caption.setText(UiText.text(MainActivity.this,"Demo album • image ") + (Math.floorMod(scene, 4) + 1) + UiText.text(MainActivity.this," of 4"));
            handler.postDelayed(this, 6000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        showHome();
    }

    private TextView text(String value, float sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setMinWidth(dp(180));
        b.setMinHeight(dp(56));
        b.setPadding(dp(18),dp(10),dp(18),dp(10));
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(true);
        b.setOnClickListener(listener);
        b.setFocusable(true);
        AppAppearance.styleButton(b);
        return b;
    }

    private void showHome() {
        stopPairing();
        slideshow = false;
        handler.removeCallbacks(advance);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(96, 56, 96, 56);
        root.setBackgroundColor(Color.TRANSPARENT);

        TextView eyebrow = text("OPEN SOURCE • ANDROID TV", 16, 0xFF6FE7D2);
        root.addView(eyebrow);
        TextView title = text(UiText.text(MainActivity.this,"AlbumFrame TV\nYour Flickr photos on TV."), 32, Color.WHITE);
        title.setPadding(0, 14, 0, 18);
        root.addView(title);
        String user=CredentialStore.user(this);
        TextView body = text(user.isEmpty()
            ? UiText.text(MainActivity.this,"Open Settings to connect Flickr using your phone.")
            : UiText.text(MainActivity.this,"Flickr login saved: ") + user + UiText.text(MainActivity.this,". Choose an album to start the slideshow."), 20, 0xFFB9C8C3);
        body.setMaxWidth(1050);
        body.setPadding(0, 0, 0, 30);
        root.addView(body);

        Button demo = button(user.isEmpty()?UiText.text(MainActivity.this,"Start demo slideshow"):UiText.text(MainActivity.this,"Choose album"), v -> {
            if(user.isEmpty()) showSlideshow();
            else startActivity(new android.content.Intent(this,AlbumActivity.class));
        });
        root.addView(demo,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(button(UiText.text(this,"Settings"),v->startActivityForResult(new android.content.Intent(this,SettingsActivity.class),SETTINGS_REQUEST)));

        TextView hint = text(UiText.text(MainActivity.this,"Use the arrow keys and OK on your remote"), 16, 0xFF789089);
        hint.setPadding(0, 32, 0, 0);
        root.addView(hint);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(root);
        setContentView(AppAppearance.menu(this,scroll));
        demo.requestFocus();
    }

    @Override protected void onResume(){super.onResume();if(!pairingVisible && !slideshow)showHome();}
    @Override protected void onActivityResult(int requestCode,int resultCode,android.content.Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==SETTINGS_REQUEST && resultCode==SettingsActivity.RESULT_CONNECT_FLICKR)showPairing();
    }
    private void showPairing() {
        stopPairing();
        pairingVisible=true;
        final int generation=pairingGeneration;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(16), dp(24), dp(16));
        root.setBackgroundColor(Color.TRANSPARENT);
        root.addView(text(UiText.text(MainActivity.this,"Connect Flickr"), 28, Color.WHITE));
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView qr=new ImageView(this);
        row.addView(qr,new LinearLayout.LayoutParams(dp(205),dp(205)));
        LinearLayout details=new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(20),0,0,0);
        TextView address=text(UiText.text(MainActivity.this,"Creating local HTTPS page …"),18,0xFF6FE7D2); details.addView(address);
        details.addView(text(UiText.text(MainActivity.this,"1. Scan with your phone on the same network.\n2. Verify the TV address in your browser. Under Advanced, continue to the TV’s local certificate.\n3. Enter your API Key and Secret, then authorize at Flickr.\n\nSetup closes after login or 10 minutes. Use your trusted home network."),17,0xFFB9C8C3));
        row.addView(details,new LinearLayout.LayoutParams(0,-2,1)); root.addView(row);
        TextView status=text("",18,0xFF6FE7D2); root.addView(status);
        Button back = button(UiText.text(MainActivity.this,"Back"), v -> showHome());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, dp(10), 0, 0);
        root.addView(back, bp);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(root);
        setContentView(AppAppearance.menu(this,scroll));
        back.requestFocus();
        new Thread(()->{
            try {
                ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
                LinkProperties links=cm.getLinkProperties(cm.getActiveNetwork());
                String ip=null;
                if(links!=null) for(LinkAddress a:links.getLinkAddresses())
                    if(a.getAddress() instanceof Inet4Address && a.getAddress().isSiteLocalAddress()) { ip=a.getAddress().getHostAddress(); break; }
                if(ip==null) throw new IllegalStateException(UiText.text(MainActivity.this,"No local IPv4 network. Connect Wi-Fi or Ethernet and disable any VPN."));
                PairingServer server=new PairingServer(ip,value->UiText.text(MainActivity.this,value),UiText.language(this),new PairingServer.Listener(){
                    public void status(String message) { handler.post(()->{ if(generation==pairingGeneration) status.setText(message); }); }
                    public void complete(String key,String secret,String token,String tokenSecret,String response) throws Exception {
                        JSONObject json=new JSONObject(response);
                        if(!"ok".equals(json.optString("stat"))) throw new IllegalStateException("Flickr login check failed");
                        JSONObject u=json.getJSONObject("user");
                        String name=u.getJSONObject("username").getString("_content");
                        // Serialize the save on the UI thread, so cancellation cannot race a commit.
                        java.util.concurrent.FutureTask<Void> save=new java.util.concurrent.FutureTask<>(()->{
                            if(generation!=pairingGeneration) throw new IllegalStateException("Session cancelled");
                            CredentialStore.save(MainActivity.this,key,secret,token,tokenSecret,name);
                            status.setText(UiText.text(MainActivity.this,"Connected as ")+name+UiText.text(MainActivity.this,". Login is saved encrypted. Select Back."));
                            return null;
                        });
                        handler.post(save);
                        try { save.get(10,java.util.concurrent.TimeUnit.SECONDS); }
                        catch(Exception e) { save.cancel(false); throw e; }
                    }
                });
                boolean[][] matrix=SmallQr.encode(server.url());
                Bitmap bitmap=Bitmap.createBitmap(328,328,Bitmap.Config.ARGB_8888); bitmap.eraseColor(Color.WHITE);
                for(int y=0;y<33;y++) for(int x=0;x<33;x++) if(matrix[y][x])
                    for(int dy=0;dy<8;dy++) for(int dx=0;dx<8;dx++) bitmap.setPixel((x+4)*8+dx,(y+4)*8+dy,Color.BLACK);
                handler.post(()->{
                    if(generation!=pairingGeneration) { server.close(); return; }
                    pairing=server; qr.setImageBitmap(bitmap); address.setText(server.url());
                    status.setText(UiText.text(MainActivity.this,"Waiting for your phone. The certificate warning applies only to the local TV page, not flickr.com."));
                    server.start();
                });
            } catch(Exception e) {
                handler.post(()->{if(generation==pairingGeneration) status.setText(UiText.text(MainActivity.this,"Could not start: ")+(e instanceof IllegalStateException?e.getMessage():UiText.text(MainActivity.this,"Check your home network and try again.")));});
            }
        },"AlbumFrame-prepare-setup").start();
    }

    private void showSlideshow() {
        slideshow = true;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        photoView = new DemoPhotoView(this);
        photoView.setScene(scene);
        root.addView(photoView, new FrameLayout.LayoutParams(-1, -1));
        SlideshowDateTimeOverlay.addIfEnabled(root);
        caption = text(UiText.text(MainActivity.this,"Demo album • image ") + (Math.floorMod(scene, 4) + 1) + UiText.text(MainActivity.this," of 4"), 18, Color.WHITE);
        caption.setBackgroundColor(0x66000000);
        caption.setPadding(24, 12, 24, 12);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        cp.setMargins(32, 32, 32, 32);
        root.addView(caption, cp);
        setContentView(root);
        hideSystemUi();
        handler.postDelayed(advance, 6000);
    }

    private void hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(5894);
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(pairingVisible && keyCode==KeyEvent.KEYCODE_BACK) { showHome(); return true; }
        if (slideshow) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                handler.removeCallbacks(advance);
                advance.run();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                scene -= 2;
                handler.removeCallbacks(advance);
                advance.run();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                showHome();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() {
        stopPairing();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    @Override protected void onStop() {
        super.onStop();
        if(pairingVisible) { stopPairing(); showHome(); }
    }
}
