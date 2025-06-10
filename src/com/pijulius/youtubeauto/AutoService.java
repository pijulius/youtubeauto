package com.pijulius.youtubeauto;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

public class AutoService extends CarActivityService {
	@Override
	public Class<? extends CarActivity> getCarActivity() {
		return AutoActivity.class;
	}
}
