package edu.usf.sas.pal.muser.ui.screens.queue.pager;

import edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel;
import java.util.List;

public interface QueuePagerView {

    void loadData(List<ViewModel> items, int position);

    void updateQueuePosition(int position);
}