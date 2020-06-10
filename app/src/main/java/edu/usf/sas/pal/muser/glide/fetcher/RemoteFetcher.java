package edu.usf.sas.pal.muser.glide.fetcher;

import com.bumptech.glide.load.data.HttpUrlFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import edu.usf.sas.pal.muser.model.ArtworkProvider;

public class RemoteFetcher extends HttpUrlFetcher {

    String TAG = "RemoteFetcher";

    public RemoteFetcher(ArtworkProvider artworkProvider) {
        super(new GlideUrl(artworkProvider.getRemoteArtworkUrl()));
    }
}