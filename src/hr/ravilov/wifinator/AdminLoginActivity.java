package hr.ravilov.wifinator;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AdminLoginActivity extends Base.Activity implements Button.OnClickListener {
	private static final int FAIL_WAIT = 2000;

	protected Button ok = null;
	protected Button cancel = null;
	protected EditText password = null;
	protected MainService.Channel mChannel = null;

	@Override
	public void onCreate(final Bundle saved) {
		super.onCreate(saved);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setResult(false);
		setContentView(R.layout.admin_login);
		startService();
		mChannel = (new MainService.Channel(this)).open();
		ok = (Button)findViewById(R.id.btn_ok);
		cancel = (Button)findViewById(R.id.btn_cancel);
		password = (EditText)findViewById(R.id.passkey);
		if (ok != null) {
			ok.setOnClickListener(this);
		}
		if (cancel != null) {
			cancel.setOnClickListener(this);
		}
	}

	@Override
	public void onChannelAvailable() {
		super.onChannelAvailable();
		if (getConfig().adminPasswordHash == null) {
			setResult(true);
			finish();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mChannel != null) {
			mChannel.close();
			mChannel = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		onPause();
	}

	@Override
	public void onConfigurationChanged(final Configuration config) {
		super.onConfigurationChanged(config);
	}

	protected Config getConfig() {
		try {
			return mChannel.getService().getConfig();
		}
		catch (final Throwable ignore) { }
		return null;
	}

	@Override
	public void onClick(final View view) {
		switch (view.getId()) {
			case R.id.btn_cancel: {
				setResult(false);
				finish();
				break;
			}
			case R.id.btn_ok: {
				final ProgressDialog pd = ProgressDialog.show(this, "", getText(R.string.please_wait), true, false);
				final Thread check = new Thread(new Runnable() {
					private boolean result = false;

					private final Runnable finish = new Runnable() {
						@Override
						public void run() {
							pd.dismiss();
							if (result) {
								setResult(true);
								finish();
							} else {
								if (password != null) {
									password.setText("");
									password.requestFocus();
								}
								Toast.makeText(AdminLoginActivity.this, getText(R.string.admin_password_mismatch), Toast.LENGTH_LONG).show();
							}
						}
					};

					@Override
					public void run() {
						if (password != null) {
							final String value = Utils.makeHash(password.getText().toString());
							result = (value.equals(getConfig().adminPasswordHash)) ? true : false;
						}
						if (!result) {
							try {
								Thread.sleep(FAIL_WAIT);
							}
							catch (final Throwable ignore) { }
						}
						runOnUiThread(finish);
					}
				});
				check.setDaemon(true);
				check.start();
				break;
			}
			default: {
				break;
			}
		}
	}

	protected void setResult(final boolean result) {
		((getParent() == null) ? this : getParent()).setResult(result ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
	}
}
