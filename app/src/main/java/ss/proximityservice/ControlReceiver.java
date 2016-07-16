package ss.proximityservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ControlReceiver extends BroadcastReceiver {

    public static final String ACTION_STOP = "ss.proximityservice.intent.action.STOP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_STOP)) {
            context.stopService(new Intent(context, ProximityService.class));
        }
    }
}
