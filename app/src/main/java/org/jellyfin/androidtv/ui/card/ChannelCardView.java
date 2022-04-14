package org.jellyfin.androidtv.ui.card;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.databinding.ViewCardChannelBinding;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.koin.java.KoinJavaComponent;

public class ChannelCardView extends FrameLayout {
    private ViewCardChannelBinding binding = ViewCardChannelBinding.inflate(LayoutInflater.from(getContext()), this, true);

    public ChannelCardView(Context context) {
        super(context);
    }

    public void setItem(final ChannelInfoDto channel) {
        binding.name.setText(channel.getNumber() + " " + channel.getName());
        BaseItemDto program = channel.getCurrentProgram();
        if (program != null) {
            if (program.getEndDate() != null && System.currentTimeMillis() > TimeUtils.convertToLocalDate(program.getEndDate()).getTime()) {
                //need to update program
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(channel.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (response.getCurrentProgram() != null)
                            updateDisplay(response.getCurrentProgram());
                        channel.setCurrentProgram(response.getCurrentProgram());
                    }
                });
            } else {
                updateDisplay(program);
            }
        } else {
            binding.program.setText(R.string.no_program_data);
            binding.time.setText("");
            binding.progress.setProgress(0);
        }

    }

    private void updateDisplay(BaseItemDto program) {
        binding.program.setText(program.getName());
        if (program.getStartDate() != null && program.getEndDate() != null) {
            binding.time.setText(android.text.format.DateFormat.getTimeFormat(getContext()).format(TimeUtils.convertToLocalDate(program.getStartDate()))
                    + "-" + android.text.format.DateFormat.getTimeFormat(getContext()).format(TimeUtils.convertToLocalDate(program.getEndDate())));
            long start = TimeUtils.convertToLocalDate(program.getStartDate()).getTime();
            long current = System.currentTimeMillis() - start;
            if (current > 0) {
                long duration = TimeUtils.convertToLocalDate(program.getEndDate()).getTime() - start;
                binding.progress.setProgress((int) ((current * 100.0 / duration)));
            } else {
                binding.progress.setProgress(0);
            }

        } else {
            binding.time.setText("");
            binding.progress.setProgress(0);
        }

    }

}
