package hr.ravilov.wifinator;

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
import android.widget.Toast;

public class PrefPassword extends DialogPreference implements Button.OnClickListener {
	public static interface OnPasswordChangedListener {
		public void onPasswordChanged(final Preference pref, final String password);
	}

	private static class Buttons {
		public static final int OK = android.R.id.button1;
		public static final int CANCEL = android.R.id.button2;
		public static final int CLEAR = android.R.id.button3;
	}

	protected boolean hasPassword = false;
	protected String mismatchMessage = null;
	protected OnPasswordChangedListener listener = null;
	protected EditText password1 = null;
	protected EditText password2 = null;
	protected Button ok = null;
	protected Button cancel = null;
	protected Button clear = null;

	public PrefPassword(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public PrefPassword(final Context context, final AttributeSet attrs, final int styleId) {
		super(context, attrs, styleId);
		init(attrs);
	}

	private void init(final AttributeSet attrs) {
		setDialogLayoutResource(R.layout.pref_password);
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PrefPassword);
		hasPassword = a.getBoolean(R.styleable.PrefPassword_hasPassword, hasPassword);
		mismatchMessage = a.getString(R.styleable.PrefPassword_mismatchMessage);
		a.recycle();
	}

	public void setHasPassword(final boolean value) {
		hasPassword = value;
	}

	public void setMismatchMessage(final int resId) {
		setMismatchMessage(getContext().getString(resId));
	}

	public void setMismatchMessage(final String msg) {
		mismatchMessage = msg;
	}

	public void setOnPasswordChangedListener(final OnPasswordChangedListener l) {
		listener = l;
	}

	@Override
	protected View onCreateDialogView() {
		final View view = super.onCreateDialogView();
		password1 = (EditText)view.findViewById(R.id.password1);
		password2 = (EditText)view.findViewById(R.id.password2);
		if (password1 != null) {
			password1.setHint(hasPassword ? R.string.hint_password_unchanged : R.string.hint_password_new);
		}
		if (password2 != null) {
			password2.setHint(hasPassword ? R.string.hint_password_unchanged : R.string.hint_password_new);
		}
		return view;
	}

	@Override
	protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
		builder.setPositiveButton(R.string.button_ok, null);
		builder.setNegativeButton(R.string.button_cancel, null);
		builder.setNeutralButton(R.string.button_clear, null);
	}

	@Override
	protected void showDialog(final Bundle state) {
		super.showDialog(state);
		ok = (Button)getDialog().findViewById(Buttons.OK);
		cancel = (Button)getDialog().findViewById(Buttons.CANCEL);
		clear = (Button)getDialog().findViewById(Buttons.CLEAR);
		if (ok != null) {
			ok.setOnClickListener(this);
		}
		if (cancel != null) {
			cancel.setOnClickListener(this);
		}
		if (clear != null) {
			clear.setOnClickListener(this);
			if (!hasPassword) {
				clear.setEnabled(false);
			}
		}
	}

	@Override
	public void onClick(final View view) {
		switch (view.getId()) {
			case Buttons.OK: {
				final String p1 = (password1 == null) ? null : password1.getText().toString();
				final String p2 = (password2 == null) ? null : password2.getText().toString();
				boolean ok = false;
				if (p1 != null && p2 != null) {
					if (p1.equals(p2)) {
						ok = true;
					} else {
						if (mismatchMessage != null && !mismatchMessage.equals("")) {
							Toast.makeText(getContext(), mismatchMessage, Toast.LENGTH_LONG).show();
						}
					}
				}
				if (ok) {
					if (!p1.equals("")) {
						listener.onPasswordChanged(this, p1);
					}
					getDialog().dismiss();
				}
				break;
			}
			case Buttons.CANCEL: {
				getDialog().dismiss();
				break;
			}
			case Buttons.CLEAR: {
				if (listener != null) {
					listener.onPasswordChanged(this, null);
				}
				getDialog().dismiss();
				break;
			}
			default: {
				break;
			}
		}
	}
}
