package com.orbotix.androidweardrive;

import android.util.Log;

import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;

/**
* Created by Pux0r3 on 9/26/14.
*/
public class SpheroHandler implements ConnectionListener {

	private AndroidWearDriveHost mHost;
	private ISpheroHandlerResponses mResponseHandler;

	public SpheroHandler(AndroidWearDriveHost host) {
		mHost = host;
		RobotProvider.getDefaultProvider().addConnectionListener(this);
	}

	public void setResponseHandler(ISpheroHandlerResponses handler) {
		mResponseHandler = handler;
	}

	public void findRobots() {
		RobotProvider provider = RobotProvider.getDefaultProvider();

		provider.startDiscovery(mHost);
		provider.initiateConnection("");
	}

	@Override
	public void onConnected(Robot robot) {
		if (mResponseHandler != null) {
			mResponseHandler.SpheroConnected((Sphero)robot);
		}
		Log.d("AWH", "Connected to robot!");
	}

	@Override
	public void onConnectionFailed(Robot robot) {
		Log.d("AWH", "Failed to connect to robot!");
	}

	@Override
	public void onDisconnected(Robot robot) {
		Log.d("AWH", "Disconnected from robot!");
		if(mResponseHandler != null) {
			mResponseHandler.SpheroDisconnected((Sphero) robot);
		}
		findRobots();
	}

	public interface ISpheroHandlerResponses {
		void SpheroConnected(Sphero sphero);
		void SpheroDisconnected(Sphero sphero);
	}
}
