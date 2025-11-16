package org.jellyfin.androidtv.ui.itemdetail;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.databinding.FragmentItemListBinding;
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding;
import org.jellyfin.androidtv.ui.ItemListView;
import org.jellyfin.androidtv.ui.ItemRowView;
import org.jellyfin.androidtv.ui.TextUnderButton;
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.playback.AudioEventListener;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.util.PlaybackHelper;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.serializer.UUIDSerializerKt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kotlin.Lazy;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import timber.log.Timber;

public class MusicFavoritesListFragment extends Fragment implements View.OnKeyListener {
    private LinearLayout mButtonRow;
    private ItemListView mItemList;
    private ScrollView mScrollView;
    private ItemRowView mCurrentRow;

    private ItemRowView mCurrentlyPlayingRow;

    private List<BaseItemDto> mItems = new ArrayList<>();

    private int mBottomScrollThreshold;

    private DisplayMetrics mMetrics;

    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private final Lazy<ItemLauncher> itemLauncher = inject(ItemLauncher.class);
    private final Lazy<PlaybackHelper> playbackHelper = inject(PlaybackHelper.class);
    private final Lazy<PlaybackLauncher> playbackLauncher = inject(PlaybackLauncher.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentItemListBinding binding = FragmentItemListBinding.inflate(getLayoutInflater(), container, false);

        ViewRowDetailsBinding detailsBinding = binding.details.getBinding();
        detailsBinding.fdTitle.setText(getString(R.string.lbl_favorites));
        detailsBinding.mainImage.setImageResource(R.drawable.favorites);
        detailsBinding.fdSummaryText.setText(getString(R.string.desc_automatic_fav_songs));
        mButtonRow = detailsBinding.fdButtonRow;
        mItemList = binding.songs;
        mScrollView = binding.scrollView;

        mMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        mBottomScrollThreshold = (int) (mMetrics.heightPixels * .6);

        //Item list listeners
        mItemList.setRowSelectedListener(new ItemRowView.RowSelectedListener() {
            @Override
            public void onRowSelected(ItemRowView row) {
                mCurrentRow = row;
                //Keep selected row in center of screen
                int[] location = new int[]{0, 0};
                row.getLocationOnScreen(location);
                int y = location[1];
                if (y > mBottomScrollThreshold) {
                    // too close to bottom - scroll down
                    mScrollView.smoothScrollBy(0, y - mBottomScrollThreshold);
                }
            }
        });

        mItemList.setRowClickedListener(new ItemRowView.RowClickedListener() {
            @Override
            public void onRowClicked(ItemRowView row) {
                showMenu(row, row.getItem().getType() != BaseItemKind.AUDIO);
            }
        });

        TextUnderButton play = TextUnderButton.create(requireContext(), R.drawable.ic_play, Utils.convertDpToPixel(requireContext(), 35), 2, getString(R.string.lbl_play_all), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(mItems, 0, false);
            }
        });
        play.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) mScrollView.smoothScrollTo(0, 0);
        });
        mButtonRow.addView(play);
        play.requestFocus();

        TextUnderButton shuffle = TextUnderButton.create(requireContext(), R.drawable.ic_shuffle, Utils.convertDpToPixel(requireContext(), 35), 2, getString(R.string.lbl_shuffle_all), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(mItems, 0, true);
            }
        });
        mButtonRow.addView(shuffle);
        shuffle.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) mScrollView.smoothScrollTo(0, 0);
        });

        return binding.getRoot();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;

        if (mediaManager.getValue().isPlayingAudio()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    mediaManager.getValue().togglePlayPause();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    mediaManager.getValue().nextAudioItem();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    mediaManager.getValue().prevAudioItem();
                    return true;
                case KeyEvent.KEYCODE_MENU:
                    showMenu(mCurrentRow, false);
                    return true;
            }
        } else if (mCurrentRow != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MENU:
                    showMenu(mCurrentRow, false);
                    return true;
            }
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        mediaManager.getValue().addAudioEventListener(mAudioEventListener);
        // and fire it to be sure we're updated
        mAudioEventListener.onPlaybackStateChange(mediaManager.getValue().isPlayingAudio() ? PlaybackController.PlaybackState.PLAYING : PlaybackController.PlaybackState.IDLE, mediaManager.getValue().getCurrentAudioItem());

        UUID parentId = UUIDSerializerKt.toUUIDOrNull(getArguments().getString("ParentId"));
        ItemListFragmentHelperKt.getFavoritePlaylist(this, parentId, itemResponse);
    }

    @Override
    public void onPause() {
        super.onPause();
        mediaManager.getValue().removeAudioEventListener(mAudioEventListener);
    }

    private AudioEventListener mAudioEventListener = new AudioEventListener() {
        @Override
        public void onPlaybackStateChange(@NonNull PlaybackController.PlaybackState newState, @Nullable BaseItemDto currentItem) {
            Timber.i("Got playback state change event %s for item %s", newState.toString(), currentItem != null ? currentItem.getName() : "<unknown>");

            if (newState != PlaybackController.PlaybackState.PLAYING || currentItem == null) {
                if (mCurrentlyPlayingRow != null) mCurrentlyPlayingRow.updateCurrentTime(-1);
                mCurrentlyPlayingRow = mItemList.updatePlaying(null);
            } else {
                mCurrentlyPlayingRow = mItemList.updatePlaying(currentItem.getId());
            }
        }

        @Override
        public void onProgress(long pos, long duration) {
            if (mCurrentlyPlayingRow != null) {
                mCurrentlyPlayingRow.updateCurrentTime(pos);
            }
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
        }

        @Override
        public void onQueueReplaced() {
        }
    };

    private void showMenu(final ItemRowView row, boolean showOpen) {
        PopupMenu menu = new PopupMenu(requireContext(), row != null ? row : requireActivity().getCurrentFocus(), Gravity.END);
        int order = 0;
        if (showOpen) {
            MenuItem open = menu.getMenu().add(0, 0, order++, R.string.lbl_open);
            open.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    itemLauncher.getValue().launch(new BaseItemDtoBaseRowItem(row.getItem()), null, requireContext());
                    return true;
                }
            });

        }
        MenuItem playFromHere = menu.getMenu().add(0, 0, order++, R.string.lbl_play_from_here);
        playFromHere.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                play(mItems, row.getIndex(), false);
                return true;
            }
        });
        MenuItem play = menu.getMenu().add(0, 1, order++, R.string.lbl_play);
        play.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                play(mItems.subList(row.getIndex(), row.getIndex() + 1), 0, false);
                return true;
            }
        });
        if (row.getItem().getType() == BaseItemKind.AUDIO) {
            MenuItem queue = menu.getMenu().add(0, 2, order++, R.string.lbl_add_to_queue);
            queue.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mediaManager.getValue().queueAudioItem(row.getItem());
                    return true;
                }
            });

            MenuItem mix = menu.getMenu().add(0, 1, order++, R.string.lbl_instant_mix);
            mix.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    playbackHelper.getValue().playInstantMix(requireContext(), row.getItem());
                    return true;
                }
            });

        }

        menu.show();
    }

    private Function1<List<BaseItemDto>, Unit> itemResponse = (List<BaseItemDto> items) -> {
        if (!items.isEmpty()) {
            mItems = new ArrayList<>();
            int i = 0;
            for (BaseItemDto item : items) {
                mItemList.addItem(item, i++);
                mItems.add(item);
            }
            if (mediaManager.getValue().isPlayingAudio()) {
                //update our status
                mAudioEventListener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, mediaManager.getValue().getCurrentAudioItem());
            }
        }
        return null;
    };

    private void play(List<BaseItemDto> items, int ndx, boolean shuffle) {
        Timber.i("play items: %d, ndx: %d, shuffle: %b", items.size(), ndx, shuffle);

        playbackLauncher.getValue().launch(requireContext(), items, 0, false, ndx, shuffle);
    }
}
