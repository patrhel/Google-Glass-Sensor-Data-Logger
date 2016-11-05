package com.example.dataloggerglass;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	protected void onPause() {
		MainActivity.mStopProcess = true;
		super.onPause();
	}
}
