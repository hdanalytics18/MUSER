package edu.usf.sas.pal.muser.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import edu.usf.sas.afollestad.aesthetic.Aesthetic;
import edu.usf.sas.afollestad.aesthetic.ColorIsDarkState;
import edu.usf.sas.afollestad.aesthetic.Rx;
import edu.usf.sas.afollestad.aesthetic.Util;
import edu.usf.sas.pal.muser.R;
import edu.usf.sas.pal.muser.utils.ResourceUtils;
import io.reactivex.disposables.Disposable;

public class SuggestedHeaderButton extends android.support.v7.widget.AppCompatTextView {

    private Disposable aestheticDisposable;

    private GradientDrawable backgroundDrawable;

    public SuggestedHeaderButton(Context context) {
        super(context);

        init();
    }

    public SuggestedHeaderButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public SuggestedHeaderButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    void init() {
        backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setCornerRadius(ResourceUtils.toPixels(2));
        setBackground(backgroundDrawable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        aestheticDisposable = Aesthetic.get(getContext()).colorAccent()
                .map(colorAccent -> new ColorIsDarkState(colorAccent, !Util.isColorLight(colorAccent)))
                .compose(Rx.distinctToMainThread())
                .subscribe(colorIsDarkState -> {
                    backgroundDrawable.setColor(colorIsDarkState.color());
                    setTextColor(colorIsDarkState.isDark() ? Color.WHITE : Color.BLACK);
                });

        if (isInEditMode()) {
            backgroundDrawable.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        aestheticDisposable.dispose();
        super.onDetachedFromWindow();
    }
}