package dk.femto.albumframe;

import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.widget.FrameLayout;

/** Android DreamService entry point. Optional system integration; foreground use does not depend on this. */
public final class AlbumFrameDreamService extends DreamService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DemoPhotoView view;
    private PhotoPlayer player;
    private int scene;
    private final Runnable advance = new Runnable() {
        @Override public void run() {
            if (view == null) return;
            view.setScene(++scene);
            handler.postDelayed(this, 8000);
        }
    };

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        String album=getSharedPreferences("album",0).getString("id","");
        if(!album.isEmpty() && !CredentialStore.user(this).isEmpty()) {
            player=new PhotoPlayer(this,album,getSharedPreferences("album",0).getString("title","Album"));
            setContentView(player);player.start();return;
        }
        view = new DemoPhotoView(this);
        FrameLayout demo = new FrameLayout(this);
        demo.addView(view, new FrameLayout.LayoutParams(-1,-1));
        SlideshowDateTimeOverlay.addIfEnabled(demo);
        setContentView(demo);
        handler.postDelayed(advance, 8000);
    }

    @Override public void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        view = null;
        if(player!=null){player.close();player=null;}
        super.onDetachedFromWindow();
    }
}
