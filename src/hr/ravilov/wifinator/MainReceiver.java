package hr.ravilov.wifinator;

import java.util.HashMap;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

public class MainReceiver extends BroadcastReceiver {
	private final String TAG = Base.TAG_PREFIX + Utils.getClassName(this.getClass());
	public static final int INTENT_FLAGS =
		Intent.FLAG_ACTIVITY_NEW_TASK |
		Intent.FLAG_ACTIVITY_NO_HISTORY |
		Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
		Intent.FLAG_FROM_BACKGROUND |
		Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
	;

	private static final Map<String, Class<? extends Activity>> sActivities = new HashMap<String, Class<? extends Activity>>();
	static {
		sActivities.put(Intent.ACTION_BOOT_COMPLETED, OnBootCompleted.class);
		sActivities.put(WifiUtils.BROADCAST_CHANGED, OnWifiStateChanged.class);
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final Class<? extends Activity> activity = sActivities.get(intent.getAction());
		if (activity != null) {
			final Intent launch = new Intent(context, activity);
			launch.setAction(intent.getAction());
			launch.setFlags(INTENT_FLAGS);
			launch.putExtras(intent);
			context.startActivity(Base.protectedIntent(launch));
		} else {
			Utils.Log.e(TAG, "got unrecognized broadcast %1$s", intent.getAction());
		}
	}

	protected static abstract class LocalActivity extends Base.ProtectedActivity implements Handler.Callback {
		protected abstract void handle();

		protected MainService.Channel mChannel = null;
		protected Handler uiHandler = new Handler(Looper.getMainLooper(), this);
		private boolean handled = false;

		@Override
		public void onCreate(final Bundle saved) {
			super.onCreate(saved);
			//Utils.Log.v(TAG, "got intent %1$s", getIntent().getAction());
			startService();
			if (needChannel()) {
				mChannel = (new MainService.Channel(LocalActivity.this)).open(false);
			} else {
				mChannel = null;
				onChannelAvailable();
			}
		}

		@Override
		public void finish() {
			if (handled) {
				super.finish();
			} else {
				Utils.Log.e(TAG, "got early finish request - ignoring");
			}
		}

		@Override
		public void onChannelAvailable() {
			try {
				handle();
			}
			catch (final Throwable ex) {
				//Utils.Log.v(TAG, "error while running handle()");
				ex.printStackTrace(System.err);
			}
			handled = true;
			finish();
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			if (mChannel != null) {
				mChannel.close();
				mChannel = null;
			}
		}

		@Override
		public void onConfigurationChanged(final Configuration config) {
			super.onConfigurationChanged(config);
		}

		@Override
		public boolean handleMessage(final Message msg) {
			return false;
		}

		// to be overridden if needed
		protected boolean needChannel() {
			return true;
		}

		protected Config getConfig() {
			try {
				return mChannel.getService().getConfig();
			}
			catch (final Throwable ignore) { }
			return null;
		}

		protected void setWifi(final boolean enabled) {
			Utils.Log.v(TAG, "sending %1$s WIFI request",
				enabled ? "ENABLE" : "DISABLE"
			);
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					if (enabled) {
						WifiUtils.enable(LocalActivity.this);
					} else {
						WifiUtils.disable(LocalActivity.this);
					}
				}
			});
		}
	}

	public static class OnBootCompleted extends LocalActivity {
		@Override
		protected void handle() {
			final WifiUtils.State state = WifiUtils.getState(this);
			final Config.WifiMode mode = getConfig().wifiMode;
			Utils.Log.v(TAG, "BOOT_COMPLETED - wifi_state=%2$s, wifi_mode=%1$s",
				state,
				mode
			);
			mChannel.getService().setLocked(true);
			if (!state.equals(WifiUtils.State.ENABLED) && !state.equals(WifiUtils.State.ENABLING)) {
				// nothing to do
				return;
			}
			switch (mode) {
				case OPEN: {
					break;
				}
				case PROTECTED: {
					//if (mChannel.getService().isLocked()) {
						if (state.equals(WifiUtils.State.ENABLED)) {
							setWifi(false);
						}
						mChannel.getService().setLocked(true);
						try {
							startActivity(new Intent(this, MainActivity.class));
						}
						catch (final Throwable ignore) { }
					//}
					break;
				}
				case TIMED: {
					if (state.equals(WifiUtils.State.ENABLED)) {
						if (!mChannel.getService().startTimer()) {
							setWifi(false);
						}
					}
					break;
				}
				case LOCKED: {
					setWifi(false);
					break;
				}
				default: {
					break;
				}
			}
		}
	}

	public static class OnWifiStateChanged extends LocalActivity {
		@Override
		public void handle() {
			final WifiUtils.State newState = WifiUtils.State.find(getIntent().getExtras().getInt(WifiUtils.INTENT_NEW_STATE));
			final WifiUtils.State oldState = WifiUtils.State.find(getIntent().getExtras().getInt(WifiUtils.INTENT_PREVIOUS_STATE));
			final Config.WifiMode mode = getConfig().wifiMode;
			Utils.Log.v(TAG, "WIFI_STATE_CHANGED - wifi_state=%2$s->%3$s [sys=%4$s/%5$d], wifi_mode=%1$s",
				mode,
				oldState,
				newState,
				WifiUtils.getState(this),
				WifiUtils.getStateRaw(this)
			);
			switch (mode) {
				case OPEN: {
					// nothing to do
					break;
				}
				case PROTECTED: {
					switch (newState) {
						case ENABLING:
						case ENABLED: {
							if (mChannel.getService().isLocked()) {
								setWifi(false);
							}
							// fall thru
//						}
//						case ENABLING: {
							if (mChannel.getService().isLocked()) {
								try {
									final Intent intent = new Intent(this, MainActivity.class);
									intent.setFlags(
										Intent.FLAG_ACTIVITY_CLEAR_TOP |
										Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
										Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
									);
									startActivity(intent);
								}
								catch (final Throwable ignore) { }
							}
							break;
						}
						case DISABLED:
						case DISABLING:
						case UNKNOWN: {
							mChannel.getService().setLocked(true);
							break;
						}
						default: {
							break;
						}
					}
					break;
				}
				case TIMED: {
					switch (newState) {
						case ENABLED: {
							if (!mChannel.getService().startTimer()) {
								setWifi(false);
							}
							break;
						}
						case DISABLED: {
							mChannel.getService().stopTimer();
							break;
						}
						default: {
							break;
						}
					}
					break;
				}
				case LOCKED: {
					switch (newState) {
						case ENABLING:
						case ENABLED: {
							setWifi(false);
						}
						default: {
							break;
						}
					}
					break;
				}
				default: {
					break;
				}
			}
		}
	}

	public static class OnServiceTick extends LocalActivity {
		@Override
		public void handle() {
			Utils.Log.v(TAG, "SERVICE_TICK");
			mChannel.getService().timerTick();
		}
	}
}
