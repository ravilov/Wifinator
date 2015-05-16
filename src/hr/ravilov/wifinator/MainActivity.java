package hr.ravilov.wifinator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Base.Activity implements View.OnClickListener, Handler.Callback {
	private static final int ACTIVITY_LOGIN_CODE = 10;
	private static final long POLL_MILLIS = 500;

	protected static enum ViewMode {
		UNKNOWN,
		ENABLED,
		DISABLED,
		PROTECTED,
		LOCKED,
		NOTIME,
	}

	protected static enum StatusField {
		UNKNOWN,
		MODE,
		CONNECTED,
		_SPACER,
		STATE,
		LOCKED,
		PERIOD_BEGIN,
		PERIOD_END,
		TIME_LIMIT,
		TIME_LEFT,
	}

	protected static final HashMap<ViewMode, Integer> wifiViewIds = new HashMap<ViewMode, Integer>();
	static {
		wifiViewIds.put(ViewMode.ENABLED, R.id.main_wifi_enabled);
		wifiViewIds.put(ViewMode.DISABLED, R.id.main_wifi_disabled);
		wifiViewIds.put(ViewMode.PROTECTED, R.id.main_wifi_protected);
		wifiViewIds.put(ViewMode.LOCKED, R.id.main_wifi_locked);
		wifiViewIds.put(ViewMode.NOTIME, R.id.main_wifi_notime);
	}
	protected static final HashMap<StatusField, Integer> wifiRowIds = new HashMap<StatusField, Integer>();
	static {
		wifiRowIds.put(StatusField.MODE, R.id.row_mode);
		wifiRowIds.put(StatusField.STATE, R.id.row_state);
		wifiRowIds.put(StatusField.CONNECTED, R.id.row_connected);
		wifiRowIds.put(StatusField._SPACER, R.id.row_spacer);
		wifiRowIds.put(StatusField.LOCKED, R.id.row_locked);
		wifiRowIds.put(StatusField.PERIOD_BEGIN, R.id.row_period_begin);
		wifiRowIds.put(StatusField.PERIOD_END, R.id.row_period_end);
		wifiRowIds.put(StatusField.TIME_LIMIT, R.id.row_time_limit);
		wifiRowIds.put(StatusField.TIME_LEFT, R.id.row_time_left);
	}
	protected static final HashMap<StatusField, Integer> wifiFieldIds = new HashMap<StatusField, Integer>();
	static {
		wifiFieldIds.put(StatusField.MODE, R.id.field_mode);
		wifiFieldIds.put(StatusField.STATE, R.id.field_state);
		wifiFieldIds.put(StatusField.CONNECTED, R.id.field_connected);
		wifiFieldIds.put(StatusField.LOCKED, R.id.field_locked);
		wifiFieldIds.put(StatusField.PERIOD_BEGIN, R.id.field_period_begin);
		wifiFieldIds.put(StatusField.PERIOD_END, R.id.field_period_end);
		wifiFieldIds.put(StatusField.TIME_LIMIT, R.id.field_time_limit);
		wifiFieldIds.put(StatusField.TIME_LEFT, R.id.field_time_left);
	}
	protected static final int[] clickables = new int[] {
		R.id.btn_unlock,
		R.id.btn_cancel,
		R.id.btn_close_disabled,
		R.id.btn_close_enabled,
		R.id.btn_close_protected,
		R.id.btn_close_locked,
		R.id.btn_close_notime,
		R.id.btn_enable,
		R.id.btn_disable,
	};
	protected TextView wifiStatus = null;
	protected EditText password = null;
	protected int lastOrientation = Configuration.ORIENTATION_UNDEFINED;
	protected final HashMap<ViewMode, View> wifiViews = new HashMap<ViewMode, View>();
	protected final HashMap<StatusField, TextView> statusFields = new HashMap<StatusField, TextView>();
	protected final HashMap<StatusField, View> statusRows = new HashMap<StatusField, View>();
	protected ProgressDialog dialog = null;

	protected static enum MessageType {
		UNKNOWN,
		MONITOR_UPDATE,
		WIFI_STATE_CHANGED,
		WIFI_UNLOCK,
		WIFI_ENABLE,
		WIFI_DISABLE,
		;

		private static final MessageType _default = UNKNOWN;
		private static final SparseArray<MessageType> sLookup = new SparseArray<MessageType>();
		static {
			for (final MessageType m : values()) {
				if (m.equals(_default)) {
					continue;
				}
				sLookup.put(m.value(), m);
			}
		}

		public int value() {
			return ordinal();
		}

		public static MessageType find(final int v) {
			return find(v, _default);
		}

		public static MessageType find(final int v, final MessageType def) {
			return Utils.coalesce(sLookup.get(v), def);
		}
	}

	protected boolean loaded = false;
	protected final Handler ui = new Handler(Looper.getMainLooper(), this);
	protected boolean monitorSuspended = false;
	protected MainService.Channel mChannel = null;
	private final BroadcastReceiver monitor = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (loaded) {
				ui.obtainMessage(MessageType.WIFI_STATE_CHANGED.value(), intent.getExtras().getInt(WifiUtils.INTENT_NEW_STATE), 0).sendToTarget();
			}
		}
	};

	@Override
	protected void onCreate(final Bundle saved) {
		super.onCreate(saved);
		lastOrientation = getResources().getConfiguration().orientation;
		init();
		startService();
		mChannel = (new MainService.Channel(this)).open();
		monitorSuspended = false;
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		setIntent(intent);
	}

	@Override
	public void onChannelAvailable() {
		super.onChannelAvailable();
		loaded = true;
		ui.obtainMessage(MessageType.MONITOR_UPDATE.value()).sendToTarget();
	}

	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(monitor, new IntentFilter(WifiUtils.BROADCAST_CHANGED));
		monitorSuspended = false;
		ui.obtainMessage(MessageType.MONITOR_UPDATE.value()).sendToTarget();
	}

	@Override
	public void onPause() {
		super.onPause();
		try {
			unregisterReceiver(monitor);
		}
		catch (final Throwable ignore) { }
		monitorSuspended = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mChannel != null) {
			mChannel.close();
			mChannel = null;
		}
	}

	//@Override
	public void onBackPressed() {
		onDestroy();
		finish();
	}

	@Override
	public void onConfigurationChanged(final Configuration config) {
		super.onConfigurationChanged(config);
		if (config.orientation != lastOrientation) {
			lastOrientation = config.orientation;
			init();
			updateWifiState(true);
		}
	}

	private void init() {
		setContentView(R.layout.main);
		wifiStatus = (TextView)findViewById(R.id.wifi_status);
		password = (EditText)findViewById(R.id.passkey);
		for (int i = 0; i < clickables.length; i++) {
			final View view = findViewById(clickables[i]);
			if (view == null) {
				continue;
			}
			view.setOnClickListener(this);
		}
		wifiViews.clear();
		for (final ViewMode mode : wifiViewIds.keySet()) {
			wifiViews.put(mode, findViewById(wifiViewIds.get(mode)));
		}
		statusRows.clear();
		for (final StatusField f : wifiRowIds.keySet()) {
			statusRows.put(f, findViewById(wifiRowIds.get(f)));
		}
		statusFields.clear();
		for (final StatusField f : wifiFieldIds.keySet()) {
			statusFields.put(f, (TextView)findViewById(wifiFieldIds.get(f)));
		}
	}

	protected Config getConfig() {
		try {
			return mChannel.getService().getConfig();
		}
		catch (final Throwable ignore) { }
		return null;
	}

	private void setField(final StatusField field, final String value) {
		setField(field, value, null);
	}

	private void setField(final StatusField field, final String value, final Boolean visible) {
		final TextView view = statusFields.get(field);
		if (view != null) {
			view.setText((value == null) ? "" : value);
		}
		final View row = statusRows.get(field);
		if (row != null) {
			row.setVisibility(((visible == null) ? (value == null || value.equals("")) : (visible ? false : true)) ? View.GONE : View.VISIBLE);
		}
	}

	private void setButtons(final boolean enabled) {
		for (int i = 0; i < clickables.length; i++) {
			final View view = findViewById(clickables[i]);
			if (view == null) {
				continue;
			}
			view.setEnabled(enabled);
		}
	}

	private void updateWifiState(final boolean silent) {
		updateWifiState(WifiUtils.getState(this), silent);
	}

	private void updateWifiState(final WifiUtils.State state, final boolean silent) {
		final boolean connected = WifiUtils.isConnected(this);
		final Config.WifiMode mode = (getConfig() == null) ? Config.WifiMode.UNKNOWN : getConfig().wifiMode;
		if (wifiStatus != null) {
/*
			wifiStatus.setText(String.format(Locale.US, "%1$s / %2$s / %3$s",
				mode.toString(),
				state.toString(),
				getString(connected ? R.string.wifi_status_intro_connected_true : R.string.wifi_status_intro_connected_false)
			));
*/
			switch (state) {
				case ENABLED: {
					wifiStatus.setText(connected ? getString(R.string.wifi_status_intro_connected_true) : state.toString());
					break;
				}
				case UNKNOWN:
				case DISABLED:
				case ENABLING:
				case DISABLING:
				default: {
					wifiStatus.setText(state.toString());
					break;
				}
			}
		}
		if (!silent) {
			switch (state) {
				case ENABLING:
				case DISABLING: {
					setButtons(false);
					if (dialog == null) {
						dialog = ProgressDialog.show(this, "", getText(R.string.please_wait), true, false, null);
					}
					break;
				}
				case ENABLED: {
					setButtons(true);
					if (dialog != null) {
						dialog.dismiss();
						dialog = null;
						Toast.makeText(this, getText(R.string.notice_enabled), Toast.LENGTH_SHORT).show();
					}
					break;
				}
				case DISABLED: {
					setButtons(true);
					if (dialog != null) {
						dialog.dismiss();
						dialog = null;
						Toast.makeText(this, getText(R.string.notice_disabled), Toast.LENGTH_SHORT).show();
					}
					break;
				}
				default: {
					setButtons(true);
					if (dialog != null) {
						dialog.dismiss();
						dialog = null;
					}
					break;
				}
			}
		}
		ViewMode active = null;
		switch (mode) {
			case TIMED:
			case OPEN: {
				switch (state) {
					case ENABLED:
					case ENABLING: {
						active = ViewMode.ENABLED;
						break;
					}
					case DISABLED:
					case DISABLING:
					case UNKNOWN: {
						active = (mChannel.getService() == null) ? ViewMode.DISABLED : ((mode.equals(Config.WifiMode.TIMED) && mChannel.getService().getTimeLimit() - getConfig().timeUsed <= 0) ? ViewMode.NOTIME : ViewMode.DISABLED);
						break;
					}
					default: {
						break;
					}
				}
				break;
			}
			case PROTECTED: {
				switch (state) {
					case ENABLED:
					case DISABLING: {
						active = ViewMode.ENABLED;
						break;
					}
					case DISABLED:
					case ENABLING:
					case UNKNOWN: {
						active = ViewMode.PROTECTED;
						break;
					}
					default: {
						break;
					}
				}
				break;
			}
			case LOCKED: {
				active = ViewMode.LOCKED;
				break;
			}
			default: {
				break;
			}
		}
		for (final ViewMode m : wifiViews.keySet()) {
			final View view = wifiViews.get(m);
			if (view != null) {
				view.setVisibility((active != null && m.equals(active)) ? View.VISIBLE : View.GONE);
			}
		}
		for (final StatusField f : statusRows.keySet()) {
			final View view = statusRows.get(f);
			if (view != null) {
				view.setVisibility(View.GONE);
			}
		}
		setField(StatusField.MODE, mode.toString());
		setField(StatusField.STATE, ((state == null) ? WifiUtils.getState(this) : state).toString());
		setField(StatusField.CONNECTED, getString(connected ? R.string.status_connected_true : R.string.status_connected_false));
		boolean hasExtra = false;
		switch (mode) {
			case OPEN: {
				break;
			}
			case PROTECTED: {
				if (mChannel.getService() != null) {
					setField(StatusField.LOCKED, getString(mChannel.getService().isLocked() ? R.string.status_locked_yes : R.string.status_locked_no));
					hasExtra = true;
				}
				break;
			}
			case TIMED: {
				if (mChannel.getService() != null) {
					setField(StatusField.PERIOD_BEGIN, dateFormat(this, mChannel.getService().getPeriodBegin()));
					setField(StatusField.PERIOD_END, dateFormat(this, mChannel.getService().getPeriodEnd()));
					setField(StatusField.TIME_LIMIT, timeFormat(this, mChannel.getService().getTimeLimit()));
					setField(StatusField.TIME_LEFT, timeFormat(this, Math.max(0, mChannel.getService().getTimeLimit() - getConfig().timeUsed)));
					hasExtra = true;
				}
				break;
			}
			case LOCKED: {
				break;
			}
			default: {
				break;
			}
		}
		setField(StatusField._SPACER, null, hasExtra);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_settings: {
				final Intent intent = new Intent(this, AdminLoginActivity.class);
				startActivityForResult(intent, ACTIVITY_LOGIN_CODE);
				return true;
			}
			case R.id.menu_about: {
				final String html = Utils.Template.process(getString(R.string.about), new Utils.Template.Var[] {
					new Utils.Template.Var("NAME", getString(getApplicationInfo().labelRes)),
					new Utils.Template.Var("VERSION", Utils.getMyVersion(this)),
					new Utils.Template.Var("BUILD", Utils.getMyBuild(this)),
				});
				final AlertDialog d = (new AlertDialog.Builder(this))
					.setTitle(R.string.menu_about)
					.setIcon(Utils.resizeDrawable(this, R.drawable.icon, 16, 16))
					.setMessage(Html.fromHtml(html))
					.setPositiveButton(R.string.button_ok, null)
					.create()
				;
				d.show();
				final TextView tv = (TextView)d.findViewById(android.R.id.message);
				if (tv != null) {
					tv.setMovementMethod(LinkMovementMethod.getInstance());
				}
				return true;
			}
			case R.id.menu_exit: {
				finish();
				return true;
			}
			default: {
				break;
			}
		}
		return false;
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
			case ACTIVITY_LOGIN_CODE: {
				if (resultCode == Activity.RESULT_OK) {
					startActivity(Base.protectedIntent(new Intent(this, AdminActivity.class)));
				}
				break;
			}
			default: {
				break;
			}
		}
	}

	@Override
	public boolean handleMessage(final Message msg) {
		switch (MessageType.find(msg.what)) {
			case WIFI_STATE_CHANGED: {
				updateWifiState(WifiUtils.State.find(msg.arg1), false);
				break;
			}
			case MONITOR_UPDATE: {
				msg.getTarget().removeMessages(msg.what);
				if (!monitorSuspended) {
					//Utils.Log.i(TAG, "MONITOR_UPDATE - tick");
					updateWifiState(true);
					final Message newMsg = Message.obtain();
					newMsg.copyFrom(msg);
					msg.getTarget().sendMessageDelayed(newMsg, POLL_MILLIS);
				}
				return true;
			}
			case WIFI_UNLOCK: {
				try {
					mChannel.getService().setLocked(false);
					Toast.makeText(this, getText(R.string.notice_unlocked), Toast.LENGTH_SHORT).show();
					Message.obtain(msg.getTarget(), MessageType.WIFI_ENABLE.value(), -1, 0).sendToTarget();
				}
				catch (final Throwable ignore) { }
				break;
			}
			case WIFI_ENABLE: {
				//dialog = ProgressDialog.show(this, "", getText(R.string.please_wait), true, false, null);
				WifiUtils.enable(this);
				//if (msg.arg1 >= 0) {
				//	Toast.makeText(this, getText((msg.arg1 > 0) ? msg.arg1 : R.string.notice_enabled), Toast.LENGTH_SHORT).show();
				//}
				break;
			}
			case WIFI_DISABLE: {
				//dialog = ProgressDialog.show(this, "", getText(R.string.please_wait), true, false, null);
				WifiUtils.disable(this);
				//Toast.makeText(this, getText(R.string.notice_disabled), Toast.LENGTH_SHORT).show();
				break;
			}
			case UNKNOWN:
			default: {
				break;
			}
		}
		return false;
	}

	@Override
	public void onClick(final View view) {
		switch (view.getId()) {
			case R.id.btn_unlock: {
				final ProgressDialog pd = ProgressDialog.show(this, "", getText(R.string.please_wait), true, false, null);
				final Thread check = new Thread(new Runnable() {
					private boolean result = false;

					private final Runnable finish = new Runnable() {
						@Override
						public void run() {
							pd.dismiss();
							if (password != null) {
								password.setText("");
							}
							if (result) {
								Message.obtain(ui, MessageType.WIFI_UNLOCK.value(), 0, 0).sendToTarget();
							} else {
								if (password != null) {
									password.requestFocus();
								}
								Toast.makeText(MainActivity.this, getText(R.string.wifi_password_mismatch), Toast.LENGTH_LONG).show();
							}
						}
					};

					@Override
					public void run() {
						result = false;
						if (password != null) {
							final String value = password.getText().toString();
							final String pass = (value == null || value.equals("")) ? null : Utils.makeHash(value);
							result = ((getConfig().wifiPasswordHash == null && pass == null) || (pass != null && pass.equals(getConfig().wifiPasswordHash))) ? true : false;
						}
						runOnUiThread(finish);
					}
				});
				check.setDaemon(true);
				check.start();
				break;
			}
			case R.id.btn_enable: {
				Message.obtain(ui, MessageType.WIFI_ENABLE.value()).sendToTarget();
				break;
			}
			case R.id.btn_disable: {
				Message.obtain(ui, MessageType.WIFI_DISABLE.value()).sendToTarget();
				break;
			}
			case R.id.btn_close_locked:
			case R.id.btn_close_notime:
			case R.id.btn_close_enabled:
			case R.id.btn_close_disabled:
			case R.id.btn_close_protected:
			case R.id.btn_cancel: {
				finish();
				break;
			}
			default: {
				break;
			}
		}
	}

	private static String timeFormat(final Context context, final long time) {
		final long minutes = time % 60;
		final long hours = Math.round(Math.floor(time / 60)) % 24;
		final long days = Math.round(Math.floor(time / 60 / 24));
		return String.format(Locale.getDefault(), context.getString((days > 0) ? R.string.time_format_with_days : R.string.time_format), days, hours, minutes);
	}

	private static String dateFormat(final Context context, final Calendar cal) {
		return (new SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault())).format(cal.getTime());
	}
}
