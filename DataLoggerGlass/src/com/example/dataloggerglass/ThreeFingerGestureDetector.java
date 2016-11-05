package com.example.dataloggerglass;

import android.content.Context;
import android.view.MotionEvent;

import com.google.android.glass.touchpad.GestureDetector;

public class ThreeFingerGestureDetector extends GestureDetector {
	public static enum ThreeFingerGesture {
		OneTap, SwipeLeft, SwipeRight, SwipeUp, SwipeDown, TwoTap, ThreeTap, Release
	}

	public interface ThreeFingerGestureListener {
		public void onThreeFingerGesture(ThreeFingerGesture gesture);
	}

	private int[] mPointerIndexes = new int[3]; // pointerId => pointerIndex
	private float[] mPointerPositionsX = new float[3]; // pointerId =>
														// pointerPosition
	private float[] mPointerPositionsY = new float[3]; // pointerId =>
														// pointerPosition

	private boolean mCanBegin = true;
	private boolean mHandling = false;

	private double mThreshold = 0.0;

	private ThreeFingerGestureListener mListener = null;

	public ThreeFingerGestureDetector(Context context, double threshold) {
		super(context);

		mThreshold = threshold;

		this.setFingerListener(new GestureDetector.FingerListener() {
			@Override
			public void onFingerCountChanged(int previousCount, int currentCount) {
				if (currentCount == 0) {
					mListener.onThreeFingerGesture(ThreeFingerGesture.Release);
					reset();
				} else if (currentCount > previousCount) {
					// Tap detection
					ThreeFingerGesture type = null;
					switch (currentCount) {
					case 1:
						type = ThreeFingerGesture.OneTap;
						break;
					case 2:
						type = ThreeFingerGesture.TwoTap;
						break;
					case 3:
						type = ThreeFingerGesture.ThreeTap;
						break;
					}
					mListener.onThreeFingerGesture(type);
				}
			}
		});
	}

	public ThreeFingerGestureDetector(Context context) {
		this(context, 200.0);
	}

	public void setListener(ThreeFingerGestureListener listener) {
		mListener = listener;
	}

	public void setThreshold(float threshold) {
		mThreshold = threshold;
	}

	public double getThreshold() {
		return mThreshold;
	}

	public boolean onMotionEvent(MotionEvent event) {
		int pointerCount = event.getPointerCount();

		if (!mHandling && mCanBegin && pointerCount > 0) {
			// Initialize
			mCanBegin = false;
			mHandling = true;

			for (int i = 0; i < pointerCount; i++) {
				int pointerId = event.getPointerId(i);
				mPointerIndexes[pointerId] = i;
				mPointerPositionsX[pointerId] = event.getX(i);
				mPointerPositionsY[pointerId] = event.getY(i);
			}
		} else {
			// Check the pointer up or down
			int action = event.getAction();
			int actionEvent = (action & MotionEvent.ACTION_MASK);
			int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			int pointerId = event.getPointerId(pointerIndex);

			switch (actionEvent) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				mPointerIndexes[pointerId] = pointerIndex;
				mPointerPositionsX[pointerId] = event.getX(pointerIndex);
				mPointerPositionsY[pointerId] = event.getY(pointerIndex);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				mPointerIndexes[pointerId] = -1;
				break;
			}

			// Check whether the movement exceeds the threshold
			boolean recognized = false;
			ThreeFingerGesture type = ThreeFingerGesture.Release;

			for (int i = 0; i < pointerCount; i++) {
				if (mPointerIndexes[i] != -1 && event.findPointerIndex(mPointerIndexes[i]) == i) {

					float previousX = mPointerPositionsX[i];
					float currentX = event.getX(i);
					float diffX = previousX - currentX;

					float previousY = mPointerPositionsY[i];
					float currentY = event.getY(i);
					float diffY = previousY - currentY;

					if (diffX < -mThreshold) {
						recognized = true;
						type = ThreeFingerGesture.SwipeLeft;
					} else if (mThreshold < diffX) {
						recognized = true;
						type = ThreeFingerGesture.SwipeRight;
					} else if (diffY < -mThreshold * 0.1) {
						recognized = true;
						type = ThreeFingerGesture.SwipeDown;
					} else if (mThreshold * 0.1 < diffY) {
						recognized = true;
						type = ThreeFingerGesture.SwipeUp;
					}

					if (recognized) {
						break;
					}
				}
			}

			// When recognized...
			if (mHandling && recognized) {
				if (mListener != null) {
					mListener.onThreeFingerGesture(type);
				}

				mHandling = false;
			}
		}

		return super.onMotionEvent(event);
	}

	public void reset() {
		mCanBegin = true;
	}
}
