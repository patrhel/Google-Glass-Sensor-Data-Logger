package com.example.dataloggerglass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.google.android.glass.media.Sounds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

public class MainActivity extends Activity {

	private Context mContext;
	private SharedPreferences sharedPreferences;

	LoggerService serviceBinder;
	IRSensorLogger irSensorLogger;

	private WakeLock wakeLock;
	private PowerManager powerManager;
	private SoundPool mSoundPool;
	private int mSoundID;

	private boolean mJustSelected;
	public static boolean mStopProcess = true;
	private boolean isAttached = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		mContext = getBaseContext();
		setContentView(R.layout.activity_main);

		File file = new File("/sys/bus/i2c/devices/4-0035/proxraw");
		file.setExecutable(true, false);
		file.setReadable(true, false);
		file.setWritable(true, false);
		try {
			Runtime.getRuntime().exec(new String[] { "/system/bin/chmod", "664", "/sys/bus/i2c/devices/4-0035/proxraw" });
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// deleteOldFiles
		// deleteRecursive(new File(Environment.getExternalStorageDirectory() + "/GlassLogger/"));

	}

	void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles()) {
				deleteRecursive(child);
			}
		fileOrDirectory.delete();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		this.isAttached = true;
		openOptionsMenu();
	}

	@Override
	protected void onResume() {
		super.onResume();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		mSoundID = mSoundPool.load(getApplicationContext(), R.raw.finished, 0);
		if (this.isAttached)
			openOptionsMenu();
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "DataLoggerGlass");
		wakeLock.acquire();

		// Hint: influences the hole system timeout
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 10000);

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.start_recording:
			mStopProcess = false;
			LoggerService.isLogging = true;
			startActivity(new Intent(MainActivity.this, LoggerActivity.class));
			mJustSelected = false;
			return false;
		case R.id.monitoring:
			mStopProcess = false;
			startActivity(new Intent(MainActivity.this, MonitoringActivity.class));
			mJustSelected = false;
			return false;
		case R.id.calibration:
			mStopProcess = false;
			startActivity(new Intent(MainActivity.this, CalibrationActivity.class));
			mJustSelected = false;
			return false;
		case R.id.clear_calibration:
			clearCalibration();
			mJustSelected = true;
			return false;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		if (mJustSelected) {
			Handler h = new Handler(Looper.getMainLooper());
			h.post(new Runnable() {
				@Override
				public void run() {
					openOptionsMenu();
				}
			});
			mJustSelected = false;
		} else {
			if (mStopProcess) {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}
	}

	@Override
	protected void onPause() {

		// influences the hole system timeout
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 10000);

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mSoundPool.release();
		super.onPause();
		wakeLock.release();

	}

	private void clearCalibration() {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putFloat("threshold", 4.0f);
		editor.commit();
	}

	public boolean onKeyDown(int keycode, KeyEvent event) {
		Log.d("KeyCode", "KeyCode:" + keycode);
		if (keycode == KeyEvent.KEYCODE_CAMERA) {
			return true;
		}
		return true;
	}

}