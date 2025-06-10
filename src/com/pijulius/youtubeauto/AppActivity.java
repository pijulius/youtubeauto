package com.pijulius.youtubeauto;

import android.app.Activity;
import android.os.Bundle;

public class AppActivity extends Activity {
	private BaseActivity baseActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		baseActivity = new BaseActivity();
		baseActivity.context = getBaseContext();
		baseActivity.window = getWindow();

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