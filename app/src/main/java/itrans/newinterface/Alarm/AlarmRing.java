package itrans.newinterface.Alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;

import java.util.Timer;
import java.util.TimerTask;

import itrans.newinterface.R;

public class AlarmRing extends AppCompatActivity {

    private Ringtone ringtone;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_ring);

        SeekBar alarm_cancel_seekbar = (SeekBar) findViewById(R.id.alarm_cancel_seekbar);

        SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean vibrationBoolean = myPrefs.getBoolean("vibrationCheckBox", true);
        boolean playMusic = myPrefs.getBoolean("musicCheckBox", true);
        String selectedRingTone = myPrefs.getString("selectedRingTone", RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_ALARM).toString());
        Uri uri = Uri.parse(selectedRingTone);

        if (vibrationBoolean) {
            long[] pattern = {0, 750, 500};
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(pattern, 0);
        }

        if (uri != null) {
            if (playMusic) {
                ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
                Log.e("RINGTONE", ringtone.getTitle(this));
                ringtone.play();
            }
        }

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (ringtone != null) {
                    ringtone.stop();
                }
                if (vibrator != null) {
                    vibrator.cancel();
                }
                AlarmRing.this.finish();
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 10000);

        alarm_cancel_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChange;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChange = progress;
                if (fromUser) {
                    if (progress >= 95 || progress <= 5) {
                        seekBar.setThumb(null);
                        if (ringtone != null) {
                            ringtone.stop();
                        }
                        if (vibrator != null) {
                            vibrator.cancel();
                        }
                        AlarmRing.this.finish();
                    }
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!(progressChange >= 95 || progressChange <= 5)) {
                    seekBar.setProgress(50);
                }
            }
        });
    }
}
