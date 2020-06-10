package edu.usf.sas.pal.muser.ui.screens.lyrics;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TypefaceSpan;
import edu.usf.sas.pal.muser.R;
import edu.usf.sas.pal.muser.model.Song;
import edu.usf.sas.pal.muser.utils.ShuttleUtils;
import edu.usf.sas.pal.muser.utils.TypefaceManager;

/**
 * QuickLyric helpers
 */
public class QuickLyricUtils {

    private static final String QUICKLYRIC_URL = "https://d3khd.app.goo.gl/jdF1";

    public static boolean isQLInstalled(Context context) {

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.geecko.QuickLyric", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    static void getLyricsFor(Context context, Song song) {
        Intent intent = new Intent("com.geecko.QuickLyric.getLyrics");
        intent.putExtra("TAGS", new String[] { song.artistName, song.name });
        if (intent.resolveActivity(context.getApplicationContext().getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    static Intent getQuickLyricIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(QUICKLYRIC_URL));
    }

    /**
     * @return true if the Play Store is available, and this QuickLyric can be downloaded.
     */
    static boolean canDownloadQuickLyric(Context context) {
        if (ShuttleUtils.isAmazonBuild()) return false;
        return getQuickLyricIntent().resolveActivity(context.getPackageManager()) != null;
    }

    /**
     * @return a {@link SpannableStringBuilder}, using the correct font as per the QuickLyric branding.
     */
    static Spannable getSpannedString(Context context) {
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(context.getString(R.string.quicklyric)); // "Quick" must use roboto light
        ssBuilder.setSpan(new TypefaceSpan(TypefaceManager.SANS_SERIF_LIGHT), 0, 5, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        return ssBuilder;
    }
}
