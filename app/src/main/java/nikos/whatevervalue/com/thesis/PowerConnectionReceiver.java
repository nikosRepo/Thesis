package nikos.whatevervalue.com.thesis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class PowerConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null,ifilter);

            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharger = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

            if (usbCharge || acCharger){
            Intent i = new Intent(context,MyService.class);
            context.startService(i);
            }
            else if (!usbCharge || !acCharger ){
            Intent j = new Intent(context,MyService.class);
            context.stopService(j);

        }
        }
}

