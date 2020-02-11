package nl.tudelft.ipv8.android.demo.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CancelIpv8Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, Ipv8Service.class);
        context.stopService(serviceIntent);
    }
}
