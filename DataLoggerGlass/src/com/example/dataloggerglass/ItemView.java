package com.example.dataloggerglass;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ItemView extends LinearLayout {
	public static enum ItemPosition {
		Up, Left, Center, Right, Down
	}

	public ItemPosition mPosition;
	private View layout;

	public ItemView(Context context, String labelName, int resourceID, ItemPosition position) {
		super(context);

		if (labelName == context.getString(R.string.label_one_category)) {
			layout = LayoutInflater.from(context).inflate(R.layout.category2, this);
		} else if (labelName == context.getString(R.string.label_two_category)
				|| labelName == context.getString(R.string.label_three_category)) {
			layout = LayoutInflater.from(context).inflate(R.layout.category, this);
		} else {
			layout = LayoutInflater.from(context).inflate(R.layout.item, this);
		}

		TextView labelView = (TextView) layout.findViewById(R.id.labelView);
		labelView.setText(labelName);
		ImageView iconView = (ImageView) layout.findViewById(R.id.iconView);
		iconView.setImageResource(resourceID);

		int WC = LinearLayout.LayoutParams.WRAP_CONTENT;
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(WC, WC);
		params.setMargins(210, 120, 0, 0);
		layout.setLayoutParams(params);

		mPosition = position;

		unFocus();
		animateExpand();
	}

	public void animateExpand() {
		ObjectAnimator objectAnimator = null;
		switch (mPosition) {
		case Up:
			objectAnimator = ObjectAnimator.ofFloat(this, "y", 0f);
			break;
		case Left:
			objectAnimator = ObjectAnimator.ofFloat(this, "x", 0f);
			break;
		case Right:
			objectAnimator = ObjectAnimator.ofFloat(this, "x", 420f);
			break;
		case Down:
			objectAnimator = ObjectAnimator.ofFloat(this, "y", 240f);
			break;
		default:
			break;
		}
		if (objectAnimator != null) {
			objectAnimator.setDuration(170);
			objectAnimator.setStartDelay(150);
			objectAnimator.start();
		}
	}

	public void animateContract() {
		ObjectAnimator objectAnimator = null;
		switch (mPosition) {
		case Up:
			objectAnimator = ObjectAnimator.ofFloat(this, "y", 120f);
			break;
		case Left:
			objectAnimator = ObjectAnimator.ofFloat(this, "x", 210f);
			break;
		case Right:
			objectAnimator = ObjectAnimator.ofFloat(this, "x", 210f);
			break;
		case Down:
			objectAnimator = ObjectAnimator.ofFloat(this, "y", 120f);
			break;
		default:
			break;
		}
		if (objectAnimator != null) {
			objectAnimator.setDuration(130);
			objectAnimator.start();
		}
	}

	public void focus() {
		layout.setBackgroundResource(R.drawable.item_background_selected);
	}

	public void unFocus() {
		layout.setBackgroundResource(R.drawable.item_background);
	}
}
