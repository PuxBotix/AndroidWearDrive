package com.orbotix.androidweardrive;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;


public class AndroidWearDriveHost extends Activity
		implements ConnectionCallbacks, OnConnectionFailedListener {

	private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final String DIALOG_ERROR = "dialog_error";

	private GoogleApiClient mGoogleApiClient;
	private boolean mResolvingError = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_android_wear_drive_host);

		mGoogleApiClient = new Builder(this).addApi(Wearable.API).build();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!mResolvingError) {
			mGoogleApiClient.connect();
		}
	}

	@Override
	protected void onStop() {
		mGoogleApiClient.disconnect();
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

	private Collection<String> getNodes() {
		HashSet<String> results = new HashSet<String>();
		NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
		for (Node node : nodes.getNodes()) {
			results.add(node.getId());
			Log.d("AWH", "node " + node.getId() + " found!");
		}
		return results;
	}

	@Override
	public void onConnected(Bundle bundle) {

	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (mResolvingError) {
			return;
		}
		else if (connectionResult.hasResolution()) {
			try {
				mResolvingError = true;
				connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
			}
			catch (IntentSender.SendIntentException e) {
				// try again if we fail to resolve the error
				mGoogleApiClient.connect();
			}
		}
		else {

		}
	}

	// generic error dialog code
	private void showErrorDialog(int errorCode) {
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getFragmentManager(), "errordialog");
	}
}
