package com.orbotix.androidweardrive;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;

/**
* Created by Pux0r3 on 9/26/14.
*/
public class AndroidWearServiceHandler
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private GoogleApiClient mGoogleApiClient;
	private AndroidWearDriveHost mHost;
	private boolean mResolvingError = false;

	public AndroidWearServiceHandler(AndroidWearDriveHost host, Bundle savedInstanceState) {
		mHost = host;

		// request a google api client for the wearable api
		mGoogleApiClient = new GoogleApiClient.Builder(mHost)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Wearable.API)
				.build();

		mResolvingError = savedInstanceState != null
				&& savedInstanceState.getBoolean(AndroidWearDriveHost.STATE_RESOLVING_ERROR, false);
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

	public void onStart() {
		if (!mResolvingError) {
			// try to connect, unless we're returning from an error resolution
			mGoogleApiClient.connect();
		}
	}

	public void onStop() {
		mGoogleApiClient.disconnect();
	}

	public void onSaveInstance(Bundle outState) {
		outState.putBoolean(AndroidWearDriveHost.STATE_RESOLVING_ERROR, mResolvingError);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// we're invoked with REQUEST_RESOLVE_ERROR when resolving a Google API error
		if (requestCode == AndroidWearDriveHost.REQUEST_RESOLVE_ERROR) {
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

	public void errorDialogDismissed() {
		mResolvingError = false;
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d("AWH", "Connected to wear API!");
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
				connectionResult.startResolutionForResult(mHost, AndroidWearDriveHost.REQUEST_RESOLVE_ERROR);
			}
			catch (IntentSender.SendIntentException e) {
				// try again if we fail to resolve the error
				mGoogleApiClient.connect();
			}
		}
		else {
			mHost.showErrorDialog(connectionResult.getErrorCode());
			mResolvingError = true;
		}
	}
}
