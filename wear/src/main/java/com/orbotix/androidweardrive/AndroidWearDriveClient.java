package com.orbotix.androidweardrive;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AndroidWearDriveClient extends Activity {

	private TextView mTextView;

	private Button mBtnFL;
	private Button mBtnF;
	private Button mBtnFR;
	private Button mBtnL;
	private Button mBtnR;
	private Button mBtnBL;
	private Button mBtnB;
	private Button mBtnBR;

	private Button mBtnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_wear_drive_client);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);

				mBtnFL = (Button) stub.findViewById(R.id.btn_fl);
				mBtnFL.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(315, 1.0f);
					}
				});

				mBtnF = (Button) stub.findViewById(R.id.btn_f);
				mBtnF.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(0, 1.0f);
					}
				});

				mBtnFR = (Button) stub.findViewById(R.id.btn_fr);
				mBtnFR.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(45, 1.0f);
					}
				});

				mBtnL = (Button) stub.findViewById(R.id.btn_l);
				mBtnL.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(270, 1.0f);
					}
				});

				mBtnR = (Button) stub.findViewById(R.id.btn_r);
				mBtnR.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(90, 1.0f);
					}
				});

				mBtnBL = (Button) stub.findViewById(R.id.btn_bl);
				mBtnBL.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(225, 1.0f);
					}
				});

				mBtnB = (Button) stub.findViewById(R.id.btn_b);
				mBtnB.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(180, 1.0f);
					}
				});

				mBtnBR = (Button) stub.findViewById(R.id.btn_br);
				mBtnBR.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(135, 1.0f);
					}
				});

				mBtnStop = (Button) stub.findViewById(R.id.btn_stop);
				mBtnStop.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Drive(0, 0f);
					}
				});
            }
        });
    }

	private void Drive(int heading, float speed)
	{

	}
}
