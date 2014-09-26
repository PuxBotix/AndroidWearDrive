package com.orbotix.androidweardrive;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.google.android.gms.common.GooglePlayServicesUtil;

import orbotix.sphero.Sphero;


public class AndroidWearDriveHost extends Activity {

	private static final String SPHERO_CONNECTED_EVENT = "/awdh/SpheroConnected";

	public static final String STATE_RESOLVING_ERROR = "resolving_error";

	public static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final String DIALOG_ERROR = "dialog_error";

	private AndroidWearServiceHandler mAndroidWearServiceHandler;

	private Sphero mRobot;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_android_wear_drive_host);

		mAndroidWearServiceHandler = new AndroidWearServiceHandler(this, savedInstanceState);

		// at least for testing - no sleepy!
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mAndroidWearServiceHandler.onStart();
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
		mAndroidWearServiceHandler.onStop();

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
		mAndroidWearServiceHandler.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mAndroidWearServiceHandler.onSaveInstance(outState);
	}

	private void drive(int heading, float speed) {
		if (mRobot != null) {
			mRobot.drive(heading, speed);
		}
	}

	// generic error dialog code
	public void showErrorDialog(int errorCode) {
		// an error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getFragmentManager(), "errordialog");
	}

	public void onDialogDismissed() {
		mAndroidWearServiceHandler.errorDialogDismissed();
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

}
