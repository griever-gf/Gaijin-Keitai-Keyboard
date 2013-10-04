package com.daicon.griever.gaijinkeitaikeyboard;

import com.daicon.griever.gaijinkeitaikeyboard.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ImeSettingsActivity extends PreferenceActivity {
	  @Override
	  protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.pref);
	  }
}
