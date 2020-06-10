package edu.usf.sas.pal.muser.ui.screens.queue.pager;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import edu.usf.sas.pal.muser.ShuttleApplication;
import edu.usf.sas.pal.muser.playback.MediaManager;
import edu.usf.sas.pal.muser.playback.constants.InternalIntents;
import edu.usf.sas.pal.muser.ui.common.Presenter;
import edu.usf.sas.pal.muser.ui.modelviews.QueuePagerItemView;
import edu.usf.sas.pal.muser.utils.SettingsManager;
import edu.usf.sas.simplecityapps.recycler_adapter.model.ViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;

import java.util.List;
import javax.inject.Inject;

public class QueuePagerPresenter extends Presenter<QueuePagerView> {

    private ShuttleApplication application;

    private RequestManager requestManager;

    private MediaManager mediaManager;

    private SettingsManager settingsManager;

    @Inject
    public QueuePagerPresenter(
            ShuttleApplication application,
            RequestManager requestManager,
            MediaManager mediaManager,
            SettingsManager settingsManager) {
        this.application = application;
        this.requestManager = requestManager;
        this.mediaManager = mediaManager;
        this.settingsManager = settingsManager;
    }

    @Override
    public void bindView(@NonNull QueuePagerView view) {
        super.bindView(view);

        IntentFilter filter = new IntentFilter();
        filter.addAction(InternalIntents.META_CHANGED);
        filter.addAction(InternalIntents.REPEAT_CHANGED);
        filter.addAction(InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(InternalIntents.QUEUE_CHANGED);
        filter.addAction(InternalIntents.SERVICE_CONNECTED);

        addDisposable(RxBroadcast.fromBroadcast(application, filter)
                .startWith(new Intent(InternalIntents.QUEUE_CHANGED))
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> {
                    final String action = intent.getAction();

                    QueuePagerView queuePagerView = getView();
                    if (queuePagerView == null) {
                        return;
                    }

                    if (action != null) {
                        switch (action) {
                            case InternalIntents.META_CHANGED:
                                queuePagerView.updateQueuePosition(mediaManager.getQueuePosition());
                                break;
                            case InternalIntents.REPEAT_CHANGED:
                            case InternalIntents.SHUFFLE_CHANGED:
                            case InternalIntents.QUEUE_CHANGED:
                            case InternalIntents.SERVICE_CONNECTED:

                                List<ViewModel> items = Stream.of(mediaManager.getQueue())
                                        .map(queueItem -> new QueuePagerItemView(queueItem.getSong(), requestManager, settingsManager))
                                        .collect(Collectors.toList());

                                queuePagerView.loadData(items, mediaManager.getQueuePosition());
                                break;
                        }
                    }
                }));
    }
}