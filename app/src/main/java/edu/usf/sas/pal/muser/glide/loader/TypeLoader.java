package edu.usf.sas.pal.muser.glide.loader;

import android.content.Context;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import edu.usf.sas.pal.muser.glide.fetcher.TypeFetcher;
import edu.usf.sas.pal.muser.model.ArtworkProvider;
import java.io.File;
import java.io.InputStream;

public class TypeLoader implements ModelLoader<ArtworkProvider, InputStream> {

    private static final String TAG = "ArtworkModelLoader";

    private Context applicationContext;

    @ArtworkProvider.Type
    private int type;

    private File file;

    public TypeLoader(Context context, @ArtworkProvider.Type int type, File file) {
        applicationContext = context.getApplicationContext();
        this.type = type;
        this.file = file;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(ArtworkProvider model, int width, int height) {
        return new TypeFetcher(applicationContext, model, type, file);
    }
}
