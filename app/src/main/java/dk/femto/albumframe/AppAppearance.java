package dk.femto.albumframe;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import android.util.AtomicFile;
import java.io.*;

/** App-only background; the user's selected image stays in private no-backup storage. */
final class AppAppearance {
    private static AtomicFile background(Context context){return new AtomicFile(new File(context.getNoBackupFilesDir(),"menu-background.jpg"));}
    static boolean hasBackground(Context context){return background(context).getBaseFile().exists();}
    static void removeBackground(Context context){background(context).delete();}
    static void saveBackground(Context context,Bitmap source) throws IOException {
        float scale=Math.min(1f,1920f/Math.max(source.getWidth(),source.getHeight()));
        Bitmap resized=Bitmap.createScaledBitmap(source,Math.max(1,Math.round(source.getWidth()*scale)),Math.max(1,Math.round(source.getHeight()*scale)),true);
        AtomicFile file=background(context);FileOutputStream output=null;
        try{
            output=file.startWrite();
            if(!resized.compress(Bitmap.CompressFormat.JPEG,90,output))throw new IOException("Could not save background");
            file.finishWrite(output);
        }catch(IOException error){if(output!=null)file.failWrite(output);throw error;}
        finally{if(resized!=source)resized.recycle();}
    }
    static View menu(Context context,View content) {
        FrameLayout frame=new FrameLayout(context);frame.setBackgroundColor(0xFF07120F);
        Bitmap bitmap=null;
        try(InputStream input=background(context).openRead()){bitmap=BitmapFactory.decodeStream(input);}catch(IOException ignored){}
        if(bitmap!=null){
            ImageView image=new ImageView(context);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImageBitmap(bitmap);
            frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
            View shade=new View(context);shade.setBackgroundColor(0xB8000000);frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        }
        frame.addView(content,new FrameLayout.LayoutParams(-1,-1));return frame;
    }
    static android.app.AlertDialog.Builder dialog(Context context) {
        return new android.app.AlertDialog.Builder(context) {
            @Override public android.app.AlertDialog show() {
                android.app.AlertDialog dialog=super.show();
                for(int which:new int[]{-1,-2,-3}){
                    Button button=dialog.getButton(which);if(button!=null)styleButton(button);
                }
                if(dialog.getListView()!=null){
                    int resource=context.getResources().getIdentifier("menu_focus","drawable",context.getPackageName());
                    dialog.getListView().setSelector(resource);
                }
                return dialog;
            }
        };
    }
    static void styleButton(Button button) {
        float density=button.getResources().getDisplayMetrics().density;
        StateListDrawable backgrounds=new StateListDrawable();
        backgrounds.addState(new int[]{android.R.attr.state_pressed},shape(0xFF2B9F91,Color.WHITE,density));
        backgrounds.addState(new int[]{android.R.attr.state_focused},shape(0xFF146B65,0xFF8FFFF0,density));
        backgrounds.addState(new int[]{},shape(0xE622302E,0xFF51625E,density));
        button.setBackground(backgrounds);button.setBackgroundTintList(null);button.setTextColor(Color.WHITE);
        button.setTypeface(null,Typeface.BOLD);
        button.setOnFocusChangeListener((view,focused)->{view.setElevation(focused?8*density:0);});
    }
    private static GradientDrawable shape(int fill,int border,float density){
        GradientDrawable shape=new GradientDrawable();shape.setColor(fill);shape.setCornerRadius(8*density);shape.setStroke(Math.round(3*density),border);return shape;
    }
}
