package com.pijulius.youtubeauto;

import com.google.android.apps.auto.sdk.CarActivity;

import android.os.Bundle;

public class AutoActivity extends CarActivity {
	private BaseActivity baseActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		baseActivity = new BaseActivity();
		baseActivity.context = getBaseContext();
		baseActivity.window = c();
		baseActivity.carUiController = getCarUiController();
		baseActivity.carInputManager = a();

		baseActivity.onCreate();
	}

	@Override
	public void onStart() {
		super.onStart();
		baseActivity.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		baseActivity.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		baseActivity.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		baseActivity.onStop();
	}
}