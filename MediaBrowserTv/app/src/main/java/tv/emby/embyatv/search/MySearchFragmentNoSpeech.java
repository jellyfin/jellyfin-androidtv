package tv.emby.embyatv.search;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;

/**
 * Created by Eric on 4/8/2015.
 */
public class MySearchFragmentNoSpeech extends Fragment {

    private static final int SEARCH_DELAY_MS = 1500;

    private RowsFragment mRowsFragment;
    private EditText mSearchField;
    private ArrayObjectAdapter mRowsAdapter;
    private SearchRunnable mDelayedLoad;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_empty_frame, container, false);

        // Inject the RowsFragment in the results container
        if (getChildFragmentManager().findFragmentById(R.id.emptyFrame) == null) {
            mRowsFragment = new RowsFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.emptyFrame, mRowsFragment).commit();
        } else {
            mRowsFragment = (RowsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.emptyFrame);
        }

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mRowsFragment.setAdapter(mRowsAdapter);

        // Setup item selection
        mRowsFragment.setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (!(item instanceof BaseRowItem)) return;
                ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow)row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
            }
        });

        mDelayedLoad = new SearchRunnable(getActivity(), mRowsAdapter, getActivity().getIntent().getBooleanExtra("MusicOnly",false));


        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Setup search field listening
        mSearchField = (EditText) getActivity().findViewById(R.id.searchField);
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    mHandler.removeCallbacks(mDelayedLoad);
                    mDelayedLoad.setQueryString(s.toString());
                    mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);

                } else {
                    mRowsAdapter.clear();
                }
            }
        });

    }
}
