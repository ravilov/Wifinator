package hr.ravilov.wifinator;

import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class PrefTimePeriod extends DialogPreference implements Button.OnClickListener {
	public static interface OnTimeChangedListener {
		public void onTimeChanged(final Preference pref, final TimeBundle time);
	}

	public static class TimeBundle {
		public int timeAmount = -1;
		public Config.TimePeriod timeUnit = Config.TimePeriod.UNKNOWN;
		public int periodAmount = -1;
		public Config.TimePeriod periodUnit = Config.TimePeriod.UNKNOWN;

		public TimeBundle setTimeAmount(final int value) {
			timeAmount = value;
			return this;
		}

		public TimeBundle setTimeUnit(final Config.TimePeriod value) {
			timeUnit = value;
			return this;
		}

		public TimeBundle setPeriodAmount(final int value) {
			periodAmount = value;
			return this;
		}

		public TimeBundle setPeriodUnit(final Config.TimePeriod value) {
			periodUnit = value;
			return this;
		}

		public TimeBundle set(final TimeBundle orig) {
			setTimeAmount(orig.timeAmount);
			setTimeUnit(orig.timeUnit);
			setPeriodAmount(orig.periodAmount);
			setPeriodUnit(orig.periodUnit);
			return this;
		}

		@Override
		public String toString() {
			return String.format(Locale.US, "TimeBundle(timeAmount=%1$d, timeUnit=%2$s, periodAmount=%3$d, periodUnit=%4$s)",
				timeAmount,
				timeUnit,
				periodAmount,
				periodUnit
			);
		}
	}

	private static class Buttons {
		public static final int OK = android.R.id.button1;
		public static final int CANCEL = android.R.id.button2;
		public static final int RESET = android.R.id.button3;
	}

	protected OnTimeChangedListener listener = null;
	protected final TimeBundle initial = new TimeBundle();
	protected EditText timeAmount = null;
	protected Spinner timeUnit = null;
	protected EditText periodAmount = null;
	protected Spinner periodUnit = null;
	protected Button ok = null;
	protected Button cancel = null;
	protected Button reset = null;

	public PrefTimePeriod(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public PrefTimePeriod(final Context context, final AttributeSet attrs, final int styleId) {
		super(context, attrs, styleId);
		init(attrs);
	}

	private void init(final AttributeSet attrs) {
		setDialogLayoutResource(R.layout.pref_timeperiod);
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PrefTimePeriod);
		// TODO
		a.recycle();
	}

	public void setParams(final TimeBundle params) {
		initial.set(params);
	}

	public void setOnTimeChangedListener(final OnTimeChangedListener l) {
		listener = l;
	}

	protected void reset() {
		if (timeAmount != null) {
			timeAmount.setText(String.valueOf(initial.timeAmount));
		}
		if (timeUnit != null) {
			try {
				timeUnit.setSelection(Math.min(timeUnit.getCount() - 1, Math.max(0, initial.timeUnit.ordinal())), false);
			}
			catch (final Throwable ignore) { }
		}
		if (periodAmount != null) {
			periodAmount.setText(String.valueOf(initial.periodAmount));
		}
		if (periodUnit != null) {
			try {
				periodUnit.setSelection(Math.min(periodUnit.getCount() - 1, Math.max(0, initial.periodUnit.ordinal())), false);
			}
			catch (final Throwable ignore) { }
		}
	}

	@Override
	protected View onCreateDialogView() {
		final View view = super.onCreateDialogView();
		timeAmount = (EditText)view.findViewById(R.id.time_amount);
		timeUnit = (Spinner)view.findViewById(R.id.time_unit);
		periodAmount = (EditText)view.findViewById(R.id.period_amount);
		periodUnit = (Spinner)view.findViewById(R.id.period_unit);
		reset();
		return view;
	}

	@Override
	protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
		builder.setPositiveButton(R.string.button_ok, null);
		builder.setNegativeButton(R.string.button_cancel, null);
		builder.setNeutralButton(R.string.button_reset, null);
	}

	@Override
	protected void showDialog(final Bundle state) {
		super.showDialog(state);
		ok = (Button)getDialog().findViewById(Buttons.OK);
		cancel = (Button)getDialog().findViewById(Buttons.CANCEL);
		reset = (Button)getDialog().findViewById(Buttons.RESET);
		if (ok != null) {
			ok.setOnClickListener(this);
		}
		if (cancel != null) {
			cancel.setOnClickListener(this);
		}
		if (reset != null) {
			reset.setOnClickListener(this);
		}
	}

	@Override
	public void onClick(final View view) {
		switch (view.getId()) {
			case Buttons.OK: {
				if (listener != null) {
					listener.onTimeChanged(this, (new TimeBundle())
						.setTimeAmount(Integer.valueOf(timeAmount.getText().toString()))
						.setTimeUnit(Config.TimePeriod.findByIndex((int)timeUnit.getSelectedItemId()))
						.setPeriodAmount(Integer.valueOf(periodAmount.getText().toString()))
						.setPeriodUnit(Config.TimePeriod.findByIndex((int)periodUnit.getSelectedItemId()))
					);
				}
				getDialog().dismiss();
				break;
			}
			case Buttons.CANCEL: {
				getDialog().dismiss();
				break;
			}
			case Buttons.RESET: {
				reset();
				break;
			}
			default: {
				break;
			}
		}
	}
}
