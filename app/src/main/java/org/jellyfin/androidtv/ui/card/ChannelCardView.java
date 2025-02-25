package org.jellyfin.androidtv.ui.card;

import android.content.Context;
import android.view.LayoutInflater;
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
        if (channel.getNumber() != null) binding.name.setText(channel.getNumber() + " " + channel.getName());
        else binding.name.setText(channel.getName());

        BaseItemDto program = channel.getCurrentProgram();
        if (program != null) {
            updateDisplay(program);
        } else {
            binding.program.setText(R.string.no_program_data);
            binding.time.setText("");
            binding.progress.setProgress(0);
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
