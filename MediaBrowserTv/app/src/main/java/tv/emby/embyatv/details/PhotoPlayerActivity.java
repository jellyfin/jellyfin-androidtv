package tv.emby.embyatv.details;

import android.graphics.Point;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 10/22/2015.
 */
public class PhotoPlayerActivity extends BaseActivity {
    BaseItemDto currentPhoto;
    String currentId;
    String currentTag;

    ImageView mainImage;
    int displayWidth;
    int displayHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_player);
        mainImage = (ImageView) findViewById(R.id.mainImage);
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        currentId = getIntent().getStringExtra("Id");
        currentTag = getIntent().getStringExtra("Tag");
        load();
    }

    private void load() {
        if (currentId != null) {
            TvApp.getApplication().getApiClient().GetItemAsync(currentId, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto response) {
                    currentPhoto = response;
                    loadMainImage();
                }
            });
        }
    }

    private void loadMainImage() {
        if (currentPhoto != null) {
            Picasso.with(this)
                    .load(Utils.getPrimaryImageUrl(currentPhoto, displayWidth))
                    .resize(displayWidth, displayHeight)
                    .centerInside()
                    .skipMemoryCache()
                    .error(R.drawable.photo)
                    .into(mainImage);
        }
    }
}
