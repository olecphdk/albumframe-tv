package dk.femto.albumframe;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Central D-pad friendly display and language preferences. */
public final class SettingsActivity extends Activity {
    static final int RESULT_CONNECT_FLICKR=1;
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;}
    private Button button(String label,Runnable action){
        Button button=new Button(this);button.setText(label);button.setTextSize(18);button.setAllCaps(false);button.setMinHeight(dp(58));
        button.setPadding(dp(18),dp(10),dp(18),dp(10));button.setOnClickListener(view->action.run());AppAppearance.styleButton(button);return button;
    }
    @Override protected void onCreate(Bundle state){super.onCreate(state);showSettings();}
    private void showSettings(){showSettings(false);}
    private void showSettings(boolean focusPhotoOrder){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(48),dp(32),dp(48),dp(32));
        TextView title=text(UiText.text(this,"Settings"),30,Color.WHITE);title.setPadding(0,0,0,dp(16));root.addView(title);
        Button back=button(UiText.text(this,"Back"),this::finish);root.addView(back);
        String user=CredentialStore.user(this);
        root.addView(button(UiText.text(this,user.isEmpty()?"Connect Flickr":"Reconnect Flickr"),()->{
            setResult(RESULT_CONNECT_FLICKR);finish();
        }));
        if(!user.isEmpty())root.addView(button(UiText.text(this,"Delete local login"),()->AppAppearance.dialog(this)
            .setMessage(UiText.text(this,"Delete keys and login from this TV? Flickr access is not automatically revoked."))
            .setNegativeButton(UiText.text(this,"Cancel"),null)
            .setPositiveButton(UiText.text(this,"Delete"),(dialog,which)->{
                try{CredentialStore.clear(this);showSettings();}
                catch(Exception error){AppAppearance.dialog(this).setMessage(UiText.text(this,"Could not delete login.")).setPositiveButton("OK",null).show();}
            }).show()));
        String order=PhotoOrder.normalize(getSharedPreferences("slideshow",0).getString("photo_order",PhotoOrder.FLICKR));
        Button orderButton=button(UiText.text(this,"Photo order")+": "+orderLabel(order),this::choosePhotoOrder);
        root.addView(orderButton);
        int seconds=getSharedPreferences("slideshow",0).getInt("seconds",4);
        root.addView(button(UiText.text(this,"Time between photos")+": "+seconds+UiText.text(this," seconds"),this::chooseInterval));
        int transition=getSharedPreferences("slideshow",0).getInt("transition_ms",2500);
        root.addView(button(UiText.text(this,"Photo transition")+": "+transitionLabel(transition),this::chooseTransition));
        String language=getSharedPreferences("appearance",0).getString("language","system");
        root.addView(button(UiText.text(this,"Language")+": "+languageLabel(language),this::chooseLanguage));
        if(AppAppearance.hasBackground(this))root.addView(button(UiText.text(this,"Remove background"),()->{
            AppAppearance.removeBackground(this);Toast.makeText(this,UiText.text(this,"Background removed"),Toast.LENGTH_SHORT).show();showSettings();
        }));
        root.addView(button(UiText.text(this,"Restart albums from first photo"),()->{
            getSharedPreferences("positions",0).edit().clear().apply();Toast.makeText(this,UiText.text(this,"Saved positions cleared"),Toast.LENGTH_SHORT).show();
        }));
        TextView note=text(UiText.text(this,"The information bar hides automatically after four seconds. Press any slideshow key to show it again."),16,0xFFB9C8C3);
        note.setPadding(0,dp(20),0,0);root.addView(note);
        TextView privacy=text(UiText.text(this,"Privacy: The app connects directly to Flickr. The developer receives no credentials, photos, analytics or crash reports. Credentials are encrypted on this TV. Photos stay in memory, except a background you explicitly save."),14,0xFFB9C8C3);
        privacy.setPadding(0,dp(18),0,0);root.addView(privacy);
        TextView attribution=text("This product uses the Flickr API but is not endorsed or certified by SmugMug, Inc.",14,0xFFB9C8C3);
        attribution.setPadding(0,dp(12),0,dp(8));root.addView(attribution);
        TextView version=text("AlbumFrame TV 0.7.0",14,0xFF83918D);
        version.setPadding(0,0,0,dp(18));root.addView(version);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(AppAppearance.menu(this,scroll));
        (focusPhotoOrder?orderButton:back).requestFocus();
    }
    private void choosePhotoOrder(){
        String current=PhotoOrder.normalize(getSharedPreferences("slideshow",0).getString("photo_order",PhotoOrder.FLICKR));
        String[] labels=new String[PhotoOrder.CHOICES.length];int selected=0;
        for(int index=0;index<labels.length;index++){
            labels[index]=orderLabel(PhotoOrder.CHOICES[index]);
            if(current.equals(PhotoOrder.CHOICES[index]))selected=index;
        }
        AppAppearance.dialog(this).setTitle(UiText.text(this,"Photo order")).setSingleChoiceItems(labels,selected,(dialog,which)->{
            String choice=PhotoOrder.CHOICES[which];
            if(!choice.equals(current)){
                getSharedPreferences("slideshow",0).edit().putString("photo_order",choice).apply();
                getSharedPreferences("positions",0).edit().clear().apply();
                Toast.makeText(this,UiText.text(this,"Saved photo positions reset for the new order"),Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();showSettings(true);
        }).setNegativeButton(UiText.text(this,"Back"),null).show();
    }
    private String orderLabel(String order){
        switch(order){
            case PhotoOrder.TAKEN_OLDEST:return UiText.text(this,"Date taken: oldest first");
            case PhotoOrder.TAKEN_NEWEST:return UiText.text(this,"Date taken: newest first");
            case PhotoOrder.UPLOADED_OLDEST:return UiText.text(this,"Date uploaded: oldest first");
            case PhotoOrder.UPLOADED_NEWEST:return UiText.text(this,"Date uploaded: newest first");
            default:return UiText.text(this,"Flickr album order");
        }
    }
    private void chooseInterval(){
        int[] values={1,2,4,8,16,32};String[] labels=new String[values.length];int current=getSharedPreferences("slideshow",0).getInt("seconds",4),selected=2;
        for(int index=0;index<values.length;index++){labels[index]=values[index]+UiText.text(this," seconds");if(values[index]==current)selected=index;}
        AppAppearance.dialog(this).setTitle(UiText.text(this,"Time between photos")).setSingleChoiceItems(labels,selected,(dialog,which)->{
            getSharedPreferences("slideshow",0).edit().putInt("seconds",values[which]).apply();dialog.dismiss();showSettings();
        }).setNegativeButton(UiText.text(this,"Back"),null).show();
    }
    private void chooseTransition(){
        int[] values={700,1400,2500};String[] labels={UiText.text(this,"Quick (0.7 seconds)"),UiText.text(this,"Soft (1.4 seconds)"),UiText.text(this,"Slow (2.5 seconds)")};
        int current=getSharedPreferences("slideshow",0).getInt("transition_ms",2500),selected=current==700?0:current==1400?1:2;
        AppAppearance.dialog(this).setTitle(UiText.text(this,"Photo transition")).setSingleChoiceItems(labels,selected,(dialog,which)->{
            getSharedPreferences("slideshow",0).edit().putInt("transition_ms",values[which]).apply();dialog.dismiss();showSettings();
        }).setNegativeButton(UiText.text(this,"Back"),null).show();
    }
    private void chooseLanguage(){
        String[] values={"system","da","en"};String current=getSharedPreferences("appearance",0).getString("language","system");int selected=Math.max(0,Arrays.asList(values).indexOf(current));
        AppAppearance.dialog(this).setTitle("Sprog / Language").setSingleChoiceItems(new String[]{UiText.text(this,"Follow TV language"),"Dansk","English"},selected,(dialog,which)->{
            getSharedPreferences("appearance",0).edit().putString("language",values[which]).apply();dialog.dismiss();showSettings();
        }).setNegativeButton(UiText.text(this,"Back"),null).show();
    }
    private String transitionLabel(int milliseconds){return milliseconds==700?UiText.text(this,"Quick"):milliseconds==2500?UiText.text(this,"Slow"):UiText.text(this,"Soft");}
    private String languageLabel(String language){return "da".equals(language)?"Dansk":"en".equals(language)?"English":UiText.text(this,"Follow TV language");}
}
