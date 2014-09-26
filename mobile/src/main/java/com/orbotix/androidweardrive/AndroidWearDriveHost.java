package com.orbotix.androidweardrive;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;

import orbotix.sphero.Sphero;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;


public class AndroidWearDriveHost extends Activity {

	private static final String STATE_RESOLVING_ERROR = "resolving_error";

	private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final String DIALOG_ERROR = "dialog_error";

	private GoogleApiClient mGoogleApiClient;
	private boolean mResolvingError = false;

	private Sphero mRobot;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_android_wear_drive_host);

		mResolvingError = savedInstanceState != null
				&& savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

		GooglePlayServicesHandler playServicesHandler = new GooglePlayServicesHandler(this);

		// request a google api client for the wearable api
		mGoogleApiClient = new Builder(this)
				.addConnectionCallbacks(playServicesHandler)
				.addOnConnectionFailedListener(playServicesHandler)
				.addApi(Wearable.API)
				.build();

	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!mResolvingError) {
			// try to connect, unless we're returning from an error resolution
			mGoogleApiClient.connect();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		SpheroHandler connectionHandler = new SpheroHandler(this);
		connectionHandler.setResponseHandler(new SpheroHandler.ISpheroHandlerResponses() {
			@Override
			public void SpheroConnected(Sphero sphero) {
				mRobot = sphero;
			}

			@Override
			public void SpheroDisconnected(Sphero sphero) {
				mRobot = null;
			}
		});
		connectionHandler.findRobots();
	}

	@Override
	protected void onStop() {
		// disconnect from all the things
		mGoogleApiClient.disconnect();

		if (mRobot != null) {
			mRobot.disconnect();
		}
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.android_wear_drive_host, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// we're invoked with REQUEST_RESOLVE_ERROR when resolving a Google API error
		if (requestCode == REQUEST_RESOLVE_ERROR) {
			mResolvingError = false;
			if (resultCode == RESULT_OK) {
				// retry connection if we aren't already
				if (!mGoogleApiClient.isConnecting()
						&& !mGoogleApiClient.isConnected()) {
					mGoogleApiClient.connect();
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
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

	// generic error dialog code
	private void showErrorDialog(int errorCode) {
		// an error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getFragmentManager(), "errordialog");
	}

	public void onDialogDismissed() {
		mResolvingError = false;
	}

	public static class ErrorDialogFragment extends DialogFragment {
		public ErrorDialogFragment() {}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode, this.getActivity(),
					REQUEST_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			((AndroidWearDriveHost)getActivity()).onDialogDismissed();
		}
	}

	private class GooglePlayServicesHandler
			implements ConnectionCallbacks, OnConnectionFailedListener {
		AndroidWearDriveHost mHost;

		public GooglePlayServicesHandler(AndroidWearDriveHost host) {
			mHost = host;
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
	}

}
