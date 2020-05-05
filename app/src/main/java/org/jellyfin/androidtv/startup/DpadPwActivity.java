package org.jellyfin.androidtv.startup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.repository.SerializerRepository;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;
import org.jellyfin.apiclient.model.dto.UserDto;

import androidx.fragment.app.FragmentActivity;

import timber.log.Timber;

public class DpadPwActivity extends FragmentActivity {

    private long lastKeyDown = Long.MAX_VALUE;
    private long lastKeyUp = 0;
    private boolean keyUpDetected = true;
    private boolean processed = false;
    private int lastKey;
    private int longPressSensitivity = 600;
    private int doubleClickSensitivity = 350;
    private String password = "";

    TextView title;
    TextView pwField;
    UserDto user;
    String directItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dpad_pw);

        title = (TextView)findViewById(R.id.dpad_pw_text);
        pwField = (TextView)findViewById(R.id.dpad_pw_display);

        user = SerializerRepository.INSTANCE.getSerializer().DeserializeFromString(getIntent().getStringExtra("User"), UserDto.class);
        directItemId = getIntent().getStringExtra("ItemId");

        title.setText(title.getText() + " for "+ user.getName());
        pwField.setText(password);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        keyUpDetected = true;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && System.currentTimeMillis() - lastKeyDown > longPressSensitivity) {
                    if (processed) return true; //some controllers appear to double send the up on a long press
                    Timber.d("Password finished");
                    Utils.makeTone(ToneGenerator.TONE_CDMA_ANSWER, 200);
                    processed = true;
                    AuthenticationHelper.loginUser(user.getName(), password, TvApp.getApplication().getLoginApiClient(), this, directItemId);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && System.currentTimeMillis() - lastKeyDown > longPressSensitivity) {
                    Timber.d("Password clear");
                    password = "";
                    pwField.setText(password);
                    processed = false;
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && System.currentTimeMillis() - lastKeyDown > longPressSensitivity) {
                    Timber.d("Requesting dialog...");
                    final EditText password = new EditText(this);
                    final Activity activity = this;
                    password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    new AlertDialog.Builder(this)
                            .setTitle("Enter Password")
                            .setMessage("Please enter password for " + user.getName())
                            .setView(password)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String pw = password.getText().toString();
                                    AuthenticationHelper.loginUser(user.getName(), pw, TvApp.getApplication().getLoginApiClient(), activity, directItemId);
                                }
                            }).show();
                    return true;
                }
                lastKeyDown = Long.MAX_VALUE;
                if (lastKey == keyCode && System.currentTimeMillis() - lastKeyUp <= doubleClickSensitivity) {
                    lastKeyUp = 0;
                    Utils.beep(300);
                    //Remove the single-click value
                    password = password.substring(0,password.length()-1);
                    processKey(keyCode, true, event);
                } else {
                    lastKeyUp = System.currentTimeMillis();
                    Utils.beep();
                    processKey(keyCode, false, event);
                }
                processed = false;
                return true;

            default:
                processed = false;
                return super.onKeyUp(keyCode, event);

        }
    }

    private void processKey(int keyCode, boolean doubleClick, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                password += doubleClick ? 6 : 1;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                password += doubleClick ? 7 : 2;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                password += doubleClick ? 8 : 3;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                password += doubleClick ? 9 : 4;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                password += doubleClick ? 5 : 0;
                break;
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
                password += (char)event.getUnicodeChar();
                break;
        }
        //TvApp.getApplication().getLogger().Debug(password);
        pwField.setText(password);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!keyUpDetected) return false;
                lastKeyDown = System.currentTimeMillis();
                lastKey = keyCode;
                keyUpDetected = false;
                return true;
            default:
                return super.onKeyDown(keyCode, event);

        }
    }

}
