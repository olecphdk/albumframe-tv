package dk.femto.albumframe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/** Draws local test scenes so slideshow behaviour can be tested without Flickr credentials. */
public final class DemoPhotoView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int scene;

    public DemoPhotoView(Context context) {
        super(context);
    }

    public void setScene(int scene) {
        this.scene = Math.floorMod(scene, 4);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int[][] palettes = {
                {0xFF071B2C, 0xFF22577A, 0xFF80ED99},
                {0xFF2A1638, 0xFF7B2CBF, 0xFFFFC8DD},
                {0xFF14281D, 0xFF31572C, 0xFFE9C46A},
                {0xFF231942, 0xFF5E548E, 0xFF9F86C0}
        };
        int[] p = palettes[scene];
        paint.setShader(new LinearGradient(0, 0, w, h, p[0], p[1], Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
        paint.setColor(p[2]);
        paint.setAlpha(210);
        canvas.drawCircle(w * .76f, h * .28f, h * .105f, paint);
        Path hills = new Path();
        hills.moveTo(0, h);
        hills.lineTo(0, h * .73f);
        hills.lineTo(w * .20f, h * .48f);
        hills.lineTo(w * .37f, h * .69f);
        hills.lineTo(w * .55f, h * .38f);
        hills.lineTo(w * .77f, h * .67f);
        hills.lineTo(w, h * .49f);
        hills.lineTo(w, h);
        hills.close();
        paint.setColor(Color.argb(225, 7, 22, 19));
        canvas.drawPath(hills, paint);
    }
}
