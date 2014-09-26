package com.orbotix.androidweardrive;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;

/**
* Created by Pux0r3 on 9/26/14.
*/
public class AndroidWearServiceHandler
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
			NodeApi.NodeListener, MessageApi.MessageListener {

	private static final String DIALOG_ERROR = "dialog_error";

	private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final String SPHERO_CONNECTED_EVENT = "/awdh/SpheroConnected";

	private GoogleApiClient mGoogleApiClient;
	private AndroidWearDriveHost mHost;
	private boolean mResolvingError = false;

	private boolean mSpheroConnected;

	private HashSet<String> mConnectedNodes;

	public AndroidWearServiceHandler(AndroidWearDriveHost host, Bundle savedInstanceState) {
		mHost = host;
		mConnectedNodes = new HashSet<String>();

		// request a google api client for the wearable api
		mGoogleApiClient = new GoogleApiClient.Builder(mHost)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Wearable.API)
				.build();

		mResolvingError = savedInstanceState != null
				&& savedInstanceState.getBoolean(AndroidWearDriveHost.STATE_RESOLVING_ERROR, false);
	}

	public void notifySpheroConnected(boolean connected) {
		mSpheroConnected = connected;
		if (connected) {
			notifyAllNodesSpheroConnected();
		}
	}

	private void notifyAllNodesSpheroConnected() {
		synchronized (this) {
			for (String node : mConnectedNodes) {
				notifyNodeSpheroConnected(node);
			}
		}
	}

	private void notifyNodeSpheroConnected(String node) {
		Wearable.MessageApi.sendMessage(mGoogleApiClient, node, SPHERO_CONNECTED_EVENT, null);
	}

	public void onStart() {
		if (!mResolvingError) {
			// try to connect, unless we're returning from an error resolution
			mGoogleApiClient.connect();
		}
	}

	public void onStop() {
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			Wearable.NodeApi.removeListener(mGoogleApiClient, this);
			Wearable.MessageApi.removeListener(mGoogleApiClient, this);
			mGoogleApiClient.disconnect();
		}
	}

	public void onSaveInstance(Bundle outState) {
		outState.putBoolean(AndroidWearDriveHost.STATE_RESOLVING_ERROR, mResolvingError);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// we're invoked with REQUEST_RESOLVE_ERROR when resolving a Google API error
		if (requestCode == REQUEST_RESOLVE_ERROR) {
			mResolvingError = false;
			if (resultCode == Activity.RESULT_OK) {
				// retry connection if we aren't already
				if (!mGoogleApiClient.isConnecting()
						&& !mGoogleApiClient.isConnected()) {
					mGoogleApiClient.connect();
				}
			}
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d("AWH", "Connected to wear API!");
		Wearable.NodeApi.addListener(mGoogleApiClient, this);
		Wearable.MessageApi.addListener(mGoogleApiClient, this);

		new Thread(new Runnable() {
			@Override
			public void run() {
				Collection<String> connectedNodes = getNodes();
				synchronized (AndroidWearServiceHandler.this) {
					mConnectedNodes.clear();
					mConnectedNodes.addAll(connectedNodes);
					if (mSpheroConnected) {
						notifyAllNodesSpheroConnected();
					}
				}
			}
		}).start();
	}

	private Collection<String> getNodes() {
		HashSet<String> results = new HashSet<String>();
		NodeApi.GetConnectedNodesResult nodes
				= Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
		for (Node node : nodes.getNodes()) {
			results.add(node.getId());
			Log.d("AWH", "node " + node.getId() + " found!");
		}
		return results;
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.d("AWH", "Connection Suspended!");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (mResolvingError) {
			return;
		}

		// we failed to connect to the google api service, try to resolve
		else if (connectionResult.hasResolution()) {
			try {
				mResolvingError = true;
				connectionResult.startResolutionForResult(mHost, REQUEST_RESOLVE_ERROR);
			}
			catch (IntentSender.SendIntentException e) {
				// try again if we fail to resolve the error
				mGoogleApiClient.connect();
			}
		}
		else {
			showErrorDialog(connectionResult.getErrorCode());
			mResolvingError = true;
		}
	}

	@Override
	public void onPeerConnected(Node node) {
		Log.d("AWH", "onPeerConnected(" + node + ")");
		synchronized (this) {
			mConnectedNodes.add(node.getId());
			notifyNodeSpheroConnected(node.getId());
		}
	}

	@Override
	public void onPeerDisconnected(Node node) {
		Log.d("AWH", "onPeerDisconnected(" + node + ")");
		synchronized (this) {
			mConnectedNodes.remove(node.getId());
		}
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.d("AWH", "onMessageReceived");
	}

	public void errorDialogDismissed() {
		mResolvingError = false;
	}

	// generic error dialog code
	public void showErrorDialog(int errorCode) {
		// an error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		dialogFragment.setServiceHandler(this);
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(mHost.getFragmentManager(), "errordialog");
	}

	public static class ErrorDialogFragment extends DialogFragment {
		private AndroidWearServiceHandler mHandler;

		public ErrorDialogFragment() {}

		public void setServiceHandler(AndroidWearServiceHandler handler) {
			mHandler = handler;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode, this.getActivity(),
					REQUEST_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			mHandler.errorDialogDismissed();
		}
	}
}
