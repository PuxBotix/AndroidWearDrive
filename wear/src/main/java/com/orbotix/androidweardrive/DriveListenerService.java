package com.orbotix.androidweardrive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Pux0r3 on 9/26/14.
 */
public class DriveListenerService extends WearableListenerService {
	private static final String SPHERO_CONNECTED_EVENT = "/awdh/SpheroConnected";

	@Override
	public void onCreate() {
		super.onCreate();

		LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
		broadcastManager.registerReceiver(mMessageReceiver, new IntentFilter("drive"));
	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
		broadcastManager.unregisterReceiver(mMessageReceiver);

		super.onDestroy();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		if (messageEvent.getPath().equals(SPHERO_CONNECTED_EVENT)) {
			Intent startIntent = new Intent(this, AndroidWearDriveClient.class);
			startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startIntent);
		}
	}

	@Override
	public void onPeerConnected(Node peer) {
		Log.d("AWH", "Peer connected: " + peer);
	}

	@Override
	public void onPeerDisconnected(Node peer) {
		Log.d("AWH", "Peer disconnected: " + peer);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// send drive
		}
	};
}
