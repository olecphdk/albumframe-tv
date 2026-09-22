package dk.femto.albumframe;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import java.util.Locale;

/** A small clock that uses the TV's locale, time zone and 12/24-hour setting. */
final class SlideshowDateTimeOverlay {
    private SlideshowDateTimeOverlay() {}

    static void addIfEnabled(FrameLayout parent) {
        Context context = parent.getContext();
        if (!context.getSharedPreferences("slideshow", 0).getBoolean("show_date_time", true)) return;

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.END);
        panel.setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 10));
        panel.setFocusable(false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x99000000);
        background.setCornerRadius(dp(context, 12));
        panel.setBackground(background);

        TextClock time = new TextClock(context);
        time.setTextColor(Color.WHITE);
        time.setTextSize(30);
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        time.setGravity(Gravity.END);
        // TextClock's default format follows the TV's 12/24-hour preference.
        panel.addView(time);

        TextClock date = new TextClock(context);
        String datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE d MMM");
        date.setFormat12Hour(datePattern);
        date.setFormat24Hour(datePattern);
        date.setTextColor(0xFFE2E8E6);
        date.setTextSize(17);
        date.setGravity(Gravity.END);
        panel.addView(date);

        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        layout.setMargins(dp(context, 32), dp(context, 28), dp(context, 32), dp(context, 28));
        parent.addView(panel, layout);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
