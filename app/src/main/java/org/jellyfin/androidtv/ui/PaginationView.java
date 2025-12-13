package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.model.PlaylistPaginationState;

/**
 * A reusable pagination component that displays current page information
 * and provides navigation controls for moving between pages.
 */
public class PaginationView extends LinearLayout {

    private TextView paginationInfo;
    private LinearLayout paginationControls;
    private TextView previousButton;
    private TextView nextButton;

    private OnPaginationListener listener;

    public interface OnPaginationListener {
        void onPreviousPage();
        void onNextPage();
    }

    public PaginationView(Context context) {
        super(context);
        init(context);
    }

    public PaginationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PaginationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_pagination, this, true);

        paginationInfo = findViewById(R.id.paginationInfo);
        paginationControls = findViewById(R.id.paginationControls);
        previousButton = findViewById(R.id.previousPageBtn);
        nextButton = findViewById(R.id.nextPageBtn);

        setupClickListeners();
    }

    private void setupClickListeners() {
        previousButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPreviousPage();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNextPage();
            }
        });
    }

    /**
     * Updates the pagination UI based on the current pagination state.
     * @param state The current pagination state
     */
    public void updatePaginationState(PlaylistPaginationState state) {
        if (state == null) {
            setVisibility(View.GONE);
            return;
        }

        setVisibility(View.VISIBLE);

        if (state.getIsLoading()) {
            paginationInfo.setText("Loading...");
            // Disable all controls during loading
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
        } else {
            paginationInfo.setText(state.getPageDisplayText());
            // Enable/disable navigation buttons based on availability
            previousButton.setEnabled(state.hasPreviousPage());
            nextButton.setEnabled(state.hasNextPage());
        }
    }

    /**
     * Sets the pagination listener to handle navigation events.
     * @param listener The listener to handle pagination events
     */
    public void setOnPaginationListener(OnPaginationListener listener) {
        this.listener = listener;
    }

    /**
     * Shows or hides the pagination view.
     * @param visible True to show, false to hide
     */
    public void setPaginationVisible(boolean visible) {
        setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}