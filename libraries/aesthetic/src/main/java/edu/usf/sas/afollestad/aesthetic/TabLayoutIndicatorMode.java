package edu.usf.sas.afollestad.aesthetic;

import static edu.usf.sas.afollestad.aesthetic.TabLayoutIndicatorMode.ACCENT;
import static edu.usf.sas.afollestad.aesthetic.TabLayoutIndicatorMode.PRIMARY;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@IntDef(value = {PRIMARY, ACCENT})
public @interface TabLayoutIndicatorMode {
  int PRIMARY = 0;
  int ACCENT = 1;
}
