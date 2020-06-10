package edu.usf.sas.pal.muser.ui.widgets;

import edu.usf.sas.pal.muser.R;

public class WidgetConfigureActivityMedium extends BaseWidgetConfigureActivity {

    private static final String TAG = "WidgetConfigureActivityMedium";

    @Override
    int[] getWidgetLayouts() {
        return new int[] { R.layout.widget_layout_medium, R.layout.widget_layout_medium_alt };
    }

    @Override
    String getLayoutIdString() {
        return WidgetProviderMedium.ARG_MEDIUM_LAYOUT_ID;
    }

    @Override
    String getUpdateCommandString() {
        return WidgetProviderMedium.CMDAPPWIDGETUPDATE;
    }

    @Override
    int getRootViewId() {
        return R.id.widget_layout_medium;
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
