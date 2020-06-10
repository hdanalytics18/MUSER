package edu.usf.sas.afollestad.aesthetic;

import static edu.usf.sas.afollestad.aesthetic.AutoSwitchMode.AUTO;
import static edu.usf.sas.afollestad.aesthetic.AutoSwitchMode.OFF;
import static edu.usf.sas.afollestad.aesthetic.AutoSwitchMode.ON;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@IntDef(value = {OFF, ON, AUTO})
public @interface AutoSwitchMode {
  int OFF = 0;
  int ON = 1;
  int AUTO = 2;
}
