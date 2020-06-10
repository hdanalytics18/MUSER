package edu.usf.sas.pal.muser.ui.modelviews;

import android.view.View;
import android.view.ViewGroup;
import edu.usf.sas.simplecityapps.recycler_adapter.model.BaseViewModel;
import edu.usf.sas.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import static edu.usf.sas.pal.muser.R.layout.list_item_loading;
import static edu.usf.sas.pal.muser.ui.adapters.ViewType.LOADING;

public class LoadingView extends BaseViewModel<LoadingView.ViewHolder> {

    @Override
    public int getViewType() {
        return LOADING;
    }

    @Override
    public int getLayoutResId() {
        return list_item_loading;
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    public static class ViewHolder extends BaseViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public String toString() {
            return "LoadingView.ViewHolder";
        }
    }
}
