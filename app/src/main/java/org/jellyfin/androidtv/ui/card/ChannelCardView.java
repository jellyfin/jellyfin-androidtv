package org.jellyfin.androidtv.ui.card;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.databinding.ViewCardChannelBinding;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.time.Duration;
import java.time.LocalDateTime;

public class ChannelCardView extends FrameLayout {
    private ViewCardChannelBinding binding = ViewCardChannelBinding.inflate(LayoutInflater.from(getContext()), this, true);

    public ChannelCardView(Context context) {
        super(context);
    }

    public void setItem(final BaseItemDto channel) {
        if (channel == null) return;
        if (channel.getNumber() != null) binding.name.setText(channel.getNumber() + " " + channel.getName());
        else binding.name.setText(channel.getName());

        boolean isFavorite = channel.getUserData() != null && channel.getUserData().isFavorite();
        binding.favImage.setVisibility(isFavorite ? View.VISIBLE : View.GONE);

        BaseItemDto program = channel.getCurrentProgram();
        if (program != null) {
            updateDisplay(program);
            updateRecordingIndicator(program);
        } else {
            binding.program.setText(R.string.no_program_data);
            binding.time.setText("");
            binding.progress.setProgress(0);
            binding.recIndicator.setVisibility(View.GONE);
        }
    }

    private void updateRecordingIndicator(BaseItemDto program) {
        if (program.getSeriesTimerId() != null) {
            binding.recIndicator.setImageResource(program.getTimerId() != null
                    ? R.drawable.ic_record_series_red : R.drawable.ic_record_series);
            binding.recIndicator.setVisibility(View.VISIBLE);
        } else if (program.getTimerId() != null) {
            binding.recIndicator.setImageResource(R.drawable.ic_record_red);
            binding.recIndicator.setVisibility(View.VISIBLE);
        } else {
            binding.recIndicator.setVisibility(View.GONE);
        }
    }

    private void updateDisplay(BaseItemDto program) {
        binding.program.setText(program.getName());
        if (program.getStartDate() != null && program.getEndDate() != null) {
            binding.time.setText(new StringBuilder()
                    .append(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(program.getStartDate()))
                    .append("-")
                    .append(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(program.getEndDate()))
            );

            if (program.getStartDate().isBefore(LocalDateTime.now()) && program.getEndDate().isAfter(LocalDateTime.now())) {
                Duration duration = Duration.between(program.getStartDate(), program.getEndDate());
                Duration progress = Duration.between(program.getStartDate(), LocalDateTime.now());
            
                binding.progress.setProgress((int) ((progress.getSeconds() / (double) duration.getSeconds()) * 100));
            } else {
                binding.progress.setProgress(0);
            }          
        } else {
            binding.time.setText("");
            binding.progress.setProgress(0);
        }
    }
}
