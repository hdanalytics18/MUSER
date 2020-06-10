package edu.usf.sas.pal.muser.utils

import android.os.Bundle
import android.support.v4.app.Fragment

inline fun <T : Fragment> T.withArgs(
    argsBuilder: Bundle.() -> Unit
): T = this.apply { arguments = Bundle().apply(argsBuilder) }