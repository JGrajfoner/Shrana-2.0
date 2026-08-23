package si.uni_lj.fe.tnuv.shrana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "STOP_ALARM".equals(intent.getAction())) {
            RepozitorijCasovnikov.prekiniAlarm();
            RepozitorijCasovnikov.notificationManager.cancel(RepozitorijCasovnikov.NOTIFICATION_ID);
        }
    }
}
