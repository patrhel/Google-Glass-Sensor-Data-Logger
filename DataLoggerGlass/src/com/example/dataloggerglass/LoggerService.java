package com.example.dataloggerglass;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class LoggerService extends Service implements SensorEventListener {

	private SharedPreferences sharedPreferences;
	private SensorManager sensorManager;

	private Sensor accSensor;
	private LogFileWriter accLogFileWriter;

	private Sensor rvSensor;
	private LogFileWriter rotationLogFileWriter;

	private Sensor gsSensor;
	private LogFileWriter gyroLogFileWriter;

	private Sensor mgSensor;
	private LogFileWriter mgLogFileWriter;

	private Sensor liSensor;
	private LogFileWriter lightSensorLogFileWriter;

	private Thread irThread;
	private IRSensorLogger mIRSensorLogger;
	private LogFileWriter irLogFileWriter;

	private String mLogSessionDirectoryPath;

	public static boolean isLogging = false;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// if the display wakes up, there is no intent

		mLogSessionDirectoryPath = intent.getAction();

		isLogging = true;
		foregroundProcessing();
		super.onStartCommand(intent, flags, startId);

		return START_REDELIVER_INTENT; // START_STICKY;
	}

	private void foregroundProcessing() {

		// accelerometer
		if (sharedPreferences.getBoolean("preference_accelerometer", true)) {
			accLogFileWriter = new LogFileWriter(mLogSessionDirectoryPath + "acc.txt");
			accSensor = (Sensor) sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
			sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}

		// rotation vector
		if (sharedPreferences.getBoolean("preference_rotation", true)) {
			rotationLogFileWriter = new LogFileWriter(mLogSessionDirectoryPath + "rotation.txt");
			// quaternionLogFileWriter = new LogFileWriter(mLogSessionDirectoryPath + "quaternion.txt");
			rvSensor = (Sensor) sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
			sensorManager.registerListener(this, rvSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}

		// gyroscope
		if (sharedPreferences.getBoolean("preference_gyroscope", true)) {
			gyroLogFileWriter = new LogFileWriter(mLogSessionDirectoryPath + "gyro.txt");
			gsSensor = (Sensor) sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
			sensorManager.registerListener(this, gsSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}

		// magnetic sensor
		if (sharedPreferences.getBoolean("preference_magnetic_sensor", false)) {
			mgLogFileWriter = new LogFileWriter(mLogSessionDirectoryPath + "magnetic.txt");
			mgSensor = (Sensor) sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).get(0);
			sensorManager.registerListener(this, mgSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}

		// light sensor
		if (sharedPreferences.getBoolean("preference_light_sensor", false)) {
			lightSensorLogFileWriter = new LogFileWriter(mLogSessionDirectoryPath + "light.txt");
			liSensor = (Sensor) sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
			sensorManager.registerListener(this, liSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}

		// proximity sensor
		if (sharedPreferences.getBoolean("preference_proximity_sensor", true)) {
			irLogFileWriter = new LogFileWriter(mLogSessionDirectoryPath + "proximity.txt");
			mIRSensorLogger = new IRSensorLogger();
			irThread = new Thread() {
				public void run() {
					while (isLogging) {
						try {
							Float logData = mIRSensorLogger.getIRSensorData();
							if (logData > 0.0f) {
								// DOCUMENT error code:
								// -1.0: permission denied.
								// -2.0: thread has just stopped.
								irLogFileWriter.writeIRSensorData(System.currentTimeMillis(), logData);
								Log.v("LoggerService", "IR:" + logData);
							}
						} catch (Exception e) {
							Log.v("LoggerService", "IRLogger has some error...");
						}
					}
				}
			};
			irThread.start();
		}

	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		long timestamp = System.currentTimeMillis();
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			accLogFileWriter.writeACCdata(timestamp, event.accuracy, event.values[0], event.values[1], event.values[2]);
			break;
		case Sensor.TYPE_ROTATION_VECTOR:
			rotationLogFileWriter.writeRotationVectorData(timestamp, event.accuracy, event.values[0], event.values[1],
					event.values[2]);
			break;
		case Sensor.TYPE_GYROSCOPE:
			gyroLogFileWriter.writeGyroscopeData(timestamp, event.accuracy, event.values[0], event.values[1], event.values[2]);
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mgLogFileWriter.writeMagneticSensorData(timestamp, event.accuracy, event.values[0], event.values[1], event.values[2]);
			break;
		case Sensor.TYPE_LIGHT:
			lightSensorLogFileWriter.writeLightSensorData(timestamp, event.accuracy, event.values[0]);
			break;
		default:
		}
	}

	@Override
	public void onDestroy() {

		// Stop logging
		sensorManager.unregisterListener(this);
		isLogging = false;

		if (accSensor != null) {
			accLogFileWriter.closeWriter();
			accSensor = null;
		}
		if (rvSensor != null) {
			rotationLogFileWriter.closeWriter();
			rvSensor = null;
		}
		if (gsSensor != null) {
			gyroLogFileWriter.closeWriter();
			gsSensor = null;
		}
		if (mgSensor != null) {
			mgLogFileWriter.closeWriter();
			mgSensor = null;
		}
		if (liSensor != null) {
			lightSensorLogFileWriter.closeWriter();
			liSensor = null;
		}
		if (mIRSensorLogger != null) {
			irLogFileWriter.closeWriter();
			irThread.interrupt();
			mIRSensorLogger = null;
		}

	}

}
