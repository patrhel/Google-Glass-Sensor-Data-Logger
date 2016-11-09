package com.example.dataloggerglass;

import com.example.dataloggerglass.ItemView.ItemPosition;
import com.example.dataloggerglass.ThreeFingerGestureDetector.ThreeFingerGesture;
import com.google.android.glass.media.Sounds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import android.widget.RelativeLayout;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

public class LoggerActivity extends Activity implements BeaconConsumer {

	private AlertDialog alertDialog;

	// [tap] --> select item, expand (with delay)
	// [release] --> contract (with delay)
	// Animations are managed by ItemView class.
	// Activities can be changed by modifying res>values>string.xml.

	private ThreeFingerGestureDetector mGestureDetector;

	private RelativeLayout mLayout;
	private volatile ItemView oneTapView;
	private volatile ItemView twoTapView;
	private volatile ItemView threeTapView;

	private volatile ItemView upView;
	private volatile ItemView leftView;
	private volatile ItemView rightView;
	private volatile ItemView downView;

	private WakeLock mWakeLock;
	private WakeLock restartWakelock;

	private boolean mExpanded = false;
	private int mTmpPointCount = 0;
	private int mPointCount = 0;

	public static enum SwipeType {
		Up, Left, Right, Down
	}

	private int DURATION_FOR_ALERT = 1500;
	private int DURATION_FOR_ALERT_STOP_RECORDING = 3000;
	private int EXPAND_DELAY = 0;
	private final Handler mHandler = new Handler();

	private Context mContext;
	private SharedPreferences sharedPreferences;
	private SurfaceView mPreview;
	private SurfaceHolder mPreviewHolder;
	private Camera mCamera;
	Thread cameraThread;

	public static boolean mStopProcess = true;

	LoggerService serviceBinder;
	IRSensorLogger irSensorLogger;

	private SoundPool mSoundPool;
	private int mSoundID;

	private boolean mInPreview = false;

	String logSessionDirectoryPath;
	File logSessionDirectory;
	String imageDirectoryPath;

	private LogFileWriter beaconLogFileWriter;
	private LogFileWriter labelLogFileWriter;

	private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mContext = getBaseContext();

		setContentView(R.layout.main);
		mLayout = (RelativeLayout) findViewById(R.id.layoutBase);

		oneTapView = new ItemView(this, getString(R.string.label_one_category), R.drawable.icon_one, ItemPosition.Left);
		twoTapView = new ItemView(this, getString(R.string.label_two_category), R.drawable.icon_two, ItemPosition.Center);
		threeTapView = new ItemView(this, getString(R.string.label_three_category), R.drawable.icon_three, ItemPosition.Right);

		mLayout.addView(oneTapView);
		mLayout.addView(twoTapView);
		mLayout.addView(threeTapView);

		foregroundProcessing();
		startRecording();

	}

	@Override
	protected void onResume() {
		super.onResume();

		MainActivity.mStopProcess = false;

		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 10000);

		/*
		 * Hint: If audio capture is started the proximity sensor provides always the same value (XE18)
		 * mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0); mSoundID = mSoundPool.load(getApplicationContext(),
		 * R.raw.finished, 0); mWakeLock = ((PowerManager)
		 * getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK , "DataLoggerGlass");
		 * mWakeLock.acquire();
		 */
	}

	private void foregroundProcessing() {

		// prepare camera preview

		mPreview = (SurfaceView) findViewById(R.id.surfaceView1);
		mPreviewHolder = mPreview.getHolder();
		mPreviewHolder.addCallback(surfaceCallback);

		if (mCamera == null) {
			mCamera = getCameraInstance();
			if (mCamera != null) {
				Log.v("MainActivity", "mCamera!=null");
				if (mCamera != null && mPreviewHolder.getSurface() != null) {
					try {
						mCamera.setPreviewDisplay(mPreviewHolder);
					} catch (IOException e) {
						Toast.makeText(LoggerActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
					}
				}

				startPreview();
			} else
				Log.v("LoggerActivity", "mCamera==null");
		}

		// prepare beacon detection

		beaconManager.getBeaconParsers().set(0, new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
		beaconManager.setForegroundScanPeriod(1100);
		beaconManager.setForegroundBetweenScanPeriod(2000);// (5000);
		beaconManager.setBackgroundScanPeriod(1100);
		beaconManager.setBackgroundBetweenScanPeriod(0);
		beaconManager.setBackgroundMode(false);
		beaconManager.bind(this);

		// Create gesture detector mGestureDetector

		mGestureDetector = new ThreeFingerGestureDetector(this, 450);

		mGestureDetector.setListener(new ThreeFingerGestureDetector.ThreeFingerGestureListener() {

			@Override
			public void onThreeFingerGesture(ThreeFingerGesture gesture) {
				switch (gesture) {
				case OneTap:
					mTmpPointCount = 1;
					mHandler.removeCallbacks(expandWithDelay);
					mHandler.postDelayed(expandWithDelay, EXPAND_DELAY);
					break;
				case TwoTap:
					mTmpPointCount = 2;
					mHandler.removeCallbacks(expandWithDelay);
					mHandler.postDelayed(expandWithDelay, EXPAND_DELAY);
					break;
				case ThreeTap:
					mTmpPointCount = 3;
					mHandler.removeCallbacks(expandWithDelay);
					mHandler.postDelayed(expandWithDelay, EXPAND_DELAY);
					break;
				case SwipeLeft:
					selectItem(SwipeType.Left);
					break;
				case SwipeRight:
					selectItem(SwipeType.Right);
					break;
				case SwipeUp:
					selectItem(SwipeType.Up);
					break;
				case SwipeDown:
					selectItem(SwipeType.Down);
					break;
				case Release:
					mTmpPointCount = 0;
					mHandler.removeCallbacks(contractWithDelay);
					mHandler.postDelayed(contractWithDelay, 100);
					break;
				}
			}
		});

		// set up folder

		GregorianCalendar now = new GregorianCalendar();

		String logSessionIdentifier = now.get(GregorianCalendar.YEAR) + "-" + (now.get(GregorianCalendar.MONTH) + 1) + "-"
				+ now.get(GregorianCalendar.DAY_OF_MONTH) + "_" + now.get(GregorianCalendar.HOUR_OF_DAY) + "-"
				+ now.get(GregorianCalendar.MINUTE) + "-" + now.get(GregorianCalendar.SECOND) + "-"
				+ now.get(GregorianCalendar.MILLISECOND);

		logSessionDirectoryPath = Environment.getExternalStorageDirectory() + "/GlassLogger/" + logSessionIdentifier + "/";
		logSessionDirectory = new File(logSessionDirectoryPath);

		try {
			logSessionDirectory.mkdirs();
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
		}

		imageDirectoryPath = logSessionDirectoryPath + "camera/";
		File imageDirectory = new File(imageDirectoryPath);
		if (!imageDirectory.exists()) {
			try {
				imageDirectory.mkdirs();
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
			}
		}

		beaconLogFileWriter = new LogFileWriter(logSessionDirectoryPath + "beacon.txt");
		labelLogFileWriter = new LogFileWriter(logSessionDirectoryPath + "label.txt");

	}

	private void startRecording() {

		if (isInstallationFinished()) {
			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {

				Intent bindIndent = new Intent(LoggerActivity.this, LoggerService.class);
				bindIndent.setAction(logSessionDirectoryPath);
				mContext.startService(bindIndent);

				LoggerService.isLogging = true;

				// Hint: If audio capture is started the proximity sensor provides always the same value (XE18)
				// Intent mServiceIntent = new Intent(MainActivity.this, AudioRecording.class);
				// bindIndent.putExtra("logSessionDirectoryPath", logSessionDirectoryPath);
				// mContext.startService(mServiceIntent);

			} else {
				Toast.makeText(getBaseContext(), "Error: Storage does not have enough space.", Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(getBaseContext(), "Error: permission error.", Toast.LENGTH_SHORT).show();
		}
	}

	private void stopRecording() {
		if (isServiceRunning()) {

			labelLogFileWriter.writeLabel(getString(R.string.label_stop_recording));

			final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

			alertDialogBuilder.setTitle("Stopped recording");
			alertDialogBuilder.setIcon(resourceID(R.string.icon_stop));
			alertDialog = alertDialogBuilder.create();
			Runnable dismissModal = new Runnable() {
				public void run() {
					alertDialog.dismiss();
				}
			};
			alertDialog.show();
			alertDialog.getWindow().setLayout(600, 400);
			mHandler.removeCallbacks(dismissModal);
			mHandler.postDelayed(dismissModal, DURATION_FOR_ALERT_STOP_RECORDING);

			Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 10000);

			Intent bindIndent = new Intent(LoggerActivity.this, LoggerService.class);
			mContext.stopService(bindIndent);

			AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
			audio.playSoundEffect(Sounds.DISMISSED);
			finish();

		}
	}

	private boolean isServiceRunning() {
		return LoggerService.isLogging;
	}

	private boolean isInstallationFinished() {
		// TODO Check whether App has root permission.
		return true;
	}


	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
			Log.e("MainActivity", "Camera is not available");
		}
		return c;
	}

	private void startPreview() {
		Log.v("MainActivity", "entering startPreview");

		if (mCamera != null) {
			Log.v("MainActivity", "before calling mCamera.startPreview");

			mCamera.startPreview();
			cameraThread = new Thread() {
				public void run() {
					Log.v("MainActivity", "in Thread");
					while (LoggerService.isLogging) {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							Log.v("MainActivity", "startPreview() has been interrupted.");
						}
						try {
							mCamera.takePicture(null, null, pictureCallback);
						} catch (Exception e) {
							Log.v("LoggerActivity", "Tried to take a picture after release()");
						}
					}
				}
			};
			cameraThread.start();

		}
	}

	private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {

			if (data == null || mCamera == null) {
				return;
			}

			String imagePath = imageDirectoryPath + System.currentTimeMillis() + ".jpg";
			FileOutputStream fileOutputStream;
			try {
				fileOutputStream = new FileOutputStream(imagePath, true);
				fileOutputStream.write(data);
				fileOutputStream.close();
			} catch (Exception e) {
			}
			fileOutputStream = null;
			Log.v("MainActivity", "A picture has been taken.");

		}
	};

	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {
			Log.v("MainActivity", "surfaceCreated");
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.v("MainActivity", "surfaceChanged=" + width + "," + height);

			if (mCamera != null && mPreviewHolder.getSurface() != null) {
				try {
					mCamera.setPreviewDisplay(mPreviewHolder);
				} catch (IOException e) {
					Toast.makeText(LoggerActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.v("MainActivity", "surfaceDestroyed");
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		}
	};

	@Override
	public void onBeaconServiceConnect() {
		beaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
				if (beacons.size() > 0) {

					Log.v("LoggerActivity", beacons.size() + " beacons.");

					for (Iterator<Beacon> beaconsTmp = beacons.iterator(); beaconsTmp.hasNext();) {

						Beacon beaconNext = beaconsTmp.next();

						Log.v("LoggerActivity",
								"Beacon " + beaconNext.getId1().toString() + " is about " + beaconNext.getDistance()
										+ " meters away.");

						double distance = beaconNext.getDistance();
						String uuid = beaconNext.getId1().toString();
						int rssi = beaconNext.getRssi();

						beaconLogFileWriter.writeBeaconData(System.currentTimeMillis(), uuid, distance, rssi);
					}

				}
			}
		});

		try {
			beaconManager.startRangingBeaconsInRegion(new Region("Test", null, null, null));

		} catch (RemoteException e) {
		}
	}

	private void selectItem(SwipeType type) {
		if (upView == null || mTmpPointCount == 0) {
			return;
		}

		final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		switch (type) {
		case Up:
			upView.focus();
			switch (mPointCount) {
			case 1:
				labelLogFileWriter.writeLabel(getString(R.string.label_one_up));
				getString(R.string.label_one_up);
				alertDialogBuilder.setTitle(R.string.label_one_up);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_one_up));
				break;

			case 2:
				labelLogFileWriter.writeLabel(getString(R.string.label_two_up));
				getString(R.string.label_two_up);
				alertDialogBuilder.setTitle(R.string.label_two_up);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_two_up));
				break;
			case 3:
				labelLogFileWriter.writeLabel(getString(R.string.label_three_up));
				getString(R.string.label_three_up);
				alertDialogBuilder.setTitle(R.string.label_three_up);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_three_up));
				break;
			}
			break;

		case Left:
			leftView.focus();
			switch (mPointCount) {
			case 1:
				labelLogFileWriter.writeLabel(getString(R.string.label_one_left));
				getString(R.string.label_one_left);
				alertDialogBuilder.setTitle(R.string.label_one_left);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_one_left));
				break;

			case 2:
				labelLogFileWriter.writeLabel(getString(R.string.label_two_left));
				getString(R.string.label_two_left);
				alertDialogBuilder.setTitle(R.string.label_two_left);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_two_left));
				break;
			case 3:
				labelLogFileWriter.writeLabel(getString(R.string.label_three_left));
				getString(R.string.label_three_left);
				alertDialogBuilder.setTitle(R.string.label_three_left);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_three_left));
				break;
			}
			break;

		case Right:
			rightView.focus();
			switch (mPointCount) {
			case 1:
				labelLogFileWriter.writeLabel(getString(R.string.label_one_right));
				getString(R.string.label_one_right);
				alertDialogBuilder.setTitle(R.string.label_one_right);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_one_right));
				break;

			case 2:
				labelLogFileWriter.writeLabel(getString(R.string.label_two_right));
				getString(R.string.label_two_right);
				alertDialogBuilder.setTitle(R.string.label_two_right);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_two_right));
				break;
			case 3:
				labelLogFileWriter.writeLabel(getString(R.string.label_three_right));
				getString(R.string.label_three_right);
				alertDialogBuilder.setTitle(R.string.label_three_right);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_three_right));
				break;
			}
			break;

		case Down:
			downView.focus();
			switch (mPointCount) {
			case 1:
				labelLogFileWriter.writeLabel(getString(R.string.label_one_down));
				getString(R.string.label_one_down);
				alertDialogBuilder.setTitle(R.string.label_one_down);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_one_down));
				break;

			case 2: // stop button
				labelLogFileWriter.writeLabel(getString(R.string.label_two_down));
				getString(R.string.label_two_down);


				stopRecording();

				break;
			case 3:
				labelLogFileWriter.writeLabel(getString(R.string.label_three_down));
				getString(R.string.label_three_down);
				alertDialogBuilder.setTitle(R.string.label_three_down);
				alertDialogBuilder.setIcon(resourceID(R.string.icon_three_down));
				break;
			}
			break;

		}

		alertDialog = alertDialogBuilder.create();
		Runnable dismissModal = new Runnable() {
			public void run() {
				alertDialog.dismiss();

				// turn off the screen
				Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0);
			}
		};
		alertDialog.show();
		alertDialog.getWindow().setLayout(600, 400);
		mHandler.removeCallbacks(dismissModal);
		mHandler.postDelayed(dismissModal, DURATION_FOR_ALERT);

		AudioManager audio = (AudioManager) this.getSystemService(this.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);

		// if a label was set flush the BufferedWriter just to be save
		labelLogFileWriter.discreteFlush();
		beaconLogFileWriter.discreteFlush();

		// label was set
		// Intent loggerIntent = new Intent(LabelActivity.this, LoggerActivity.class);
		// startActivity(loggerIntent);
	}

	private final Runnable expandWithDelay = new Runnable() {
		// duration for multi-tap detecting
		@Override
		public void run() {
			expand();
		}
	};

	private void expand() {
		if (mExpanded) {
			return;
		}
		mExpanded = true;

		// Point count should be fixed during long-tap
		mPointCount = mTmpPointCount;

		oneTapView.animateContract();
		twoTapView.animateContract();
		threeTapView.animateContract();

		switch (mPointCount) {
		case 1:
			oneTapView.focus();
			upView = new ItemView(this, getString(R.string.label_one_up), resourceID(R.string.icon_one_up), ItemPosition.Up);
			leftView = new ItemView(this, getString(R.string.label_one_left), resourceID(R.string.icon_one_left),
					ItemPosition.Left);
			rightView = new ItemView(this, getString(R.string.label_one_right), resourceID(R.string.icon_one_right),
					ItemPosition.Right);
			downView = new ItemView(this, getString(R.string.label_one_down), resourceID(R.string.icon_one_down),
					ItemPosition.Down);
			break;
		case 2:
			twoTapView.focus();
			upView = new ItemView(this, getString(R.string.label_two_up), resourceID(R.string.icon_two_up), ItemPosition.Up);
			leftView = new ItemView(this, getString(R.string.label_two_left), resourceID(R.string.icon_two_left),
					ItemPosition.Left);
			rightView = new ItemView(this, getString(R.string.label_two_right), resourceID(R.string.icon_two_right),
					ItemPosition.Right);
			downView = new ItemView(this, getString(R.string.label_two_down), resourceID(R.string.icon_two_down),
					ItemPosition.Down);
			break;
		case 3:
			threeTapView.focus();
			upView = new ItemView(this, getString(R.string.label_three_up), resourceID(R.string.icon_three_up), ItemPosition.Up);
			leftView = new ItemView(this, getString(R.string.label_three_left), resourceID(R.string.icon_three_left),
					ItemPosition.Left);
			rightView = new ItemView(this, getString(R.string.label_three_right), resourceID(R.string.icon_three_right),
					ItemPosition.Right);
			downView = new ItemView(this, getString(R.string.label_three_down), resourceID(R.string.icon_three_down),
					ItemPosition.Down);
			threeTapView.bringToFront();
			break;
		}
		if (upView != null) {
			mLayout.addView(upView);
		}
		if (leftView != null) {
			mLayout.addView(leftView);
		}

		if (rightView != null) {
			mLayout.addView(rightView);
		}

		if (downView != null) {
			mLayout.addView(downView);
		}

		// Change the stacking order
		threeTapView.bringToFront();
		oneTapView.bringToFront();
		twoTapView.bringToFront();
		switch (mPointCount) {
		case 1:
			oneTapView.bringToFront();
			break;
		case 2:
			twoTapView.bringToFront();
			break;
		case 3:
			threeTapView.bringToFront();
			break;
		}

	}

	private final Runnable contractWithDelay = new Runnable() {
		@Override
		public void run() {
			contract();
		}
	};

	private void contract() {
		if (!mExpanded) {
			return;
		}
		mExpanded = false;

		if (upView != null) {

			upView.animateContract();
		}

		if (leftView != null) {
			leftView.animateContract();
		}
		if (rightView != null) {
			rightView.animateContract();
		}

		if (downView != null) {
			downView.animateContract();
		}

		mLayout.removeView(upView);
		mLayout.removeView(leftView);
		mLayout.removeView(rightView);
		mLayout.removeView(downView);
		upView = null;
		leftView = null;
		rightView = null;
		downView = null;

		oneTapView.animateExpand();
		twoTapView.animateExpand();
		threeTapView.animateExpand();

		oneTapView.unFocus();
		twoTapView.unFocus();
		threeTapView.unFocus();
	}

	public int resourceID(int resource) {
		return getResources().getIdentifier(getString(resource), "drawable", this.getPackageName());
	}

	@Override
	protected void onPause() {
		super.onPause();

		// influences the hole system timeout
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 10000);
	}

	@Override
	protected void onDestroy() {
		if (alertDialog != null) {
			alertDialog.dismiss();
		}

		if (beaconLogFileWriter != null) {
			beaconLogFileWriter.closeWriter();
		}

		if (labelLogFileWriter != null) {

			labelLogFileWriter.closeWriter();

		}

		LoggerService.isLogging = false;

		if (cameraThread != null) {
			cameraThread.interrupt();
		}

		if (beaconManager != null) {
			beaconManager.unbind(this);
		}

		if (mInPreview) {
			Log.v("MainActivity", "mInPreview is true");
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
			mInPreview = false;
		}

		MainActivity.mStopProcess = true;
		super.onDestroy();
	}


	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}

	// Re-map key events
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		Log.d("KeyCode", "KeyCode:" + keycode);
		if (keycode == KeyEvent.KEYCODE_CAMERA) {
			stopRecording();
			return true;
		}
		return true;
	}

}
