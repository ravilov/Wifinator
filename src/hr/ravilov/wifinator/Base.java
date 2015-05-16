package hr.ravilov.wifinator;

import java.util.List;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;

public class Base {
	private Base() {
	}

	private static final String TOKEN = Utils.getClassName(Base.class) + ".STOKEN";
	public static final String TAG_PREFIX = "Wtor::";

	public static interface ChannelListener {
		public Context getContext();
		public void onChannelAvailable();
		public void onChannelCancel();
	}

	public static Intent protectedIntent(final Intent intent) {
		intent.putExtra(TOKEN, Process.myPid());
		return intent;
	}

	public static boolean checkIntent(final Intent intent) {
		try {
			if (intent.getExtras().getInt(TOKEN, -1) != Process.myPid()) {
				throw new Exception();
			}
			return true;
		}
		catch (final Throwable ignore) { }
		return false;
	}

	public static boolean isServiceRunning(final Context context) {
		final ActivityManager manager = (ActivityManager)context.getSystemService(Activity.ACTIVITY_SERVICE);
		if (manager == null) {
			return false;
		}
		final List<ActivityManager.RunningServiceInfo> list = manager.getRunningServices(Integer.MAX_VALUE);
		if (list == null) {
			return false;
		}
		for (final ActivityManager.RunningServiceInfo entry : list) {
			if (MainService.class.equals(entry.service.getClass())) {
				return true;
			}
		}
		return false;
	}

	public static void startService(final Context context) {
		if (isServiceRunning(context)) {
			return;
		}
		context.startService(new Intent(context, MainService.class));

	}

	public static abstract class Activity extends android.app.Activity implements ChannelListener {
		protected final String TAG = TAG_PREFIX + Utils.getClassName(this.getClass());

		@Override
		public void onDestroy() {
			super.onDestroy();
			// make sure the service keeps running
			startService();
		}

		// to be overridden if needed
		@Override
		public void onChannelAvailable() {
		}

		// to be overridden if needed
		@Override
		public void onChannelCancel() {
			finish();
		}

		@Override
		public Context getContext() {
			return this;
		}

		protected boolean isServiceRunning() {
			return Base.isServiceRunning(this);
		}

		protected void startService() {
			Base.startService(this);
		}
	}

	public static abstract class ProtectedActivity extends Activity {
		@Override
		protected void onCreate(final Bundle state) {
			super.onCreate(state);
			if (!checkIntent(getIntent())) {
				Utils.Log.e(TAG, "invalid token");
				onInvalidToken();
			}
		}

		protected void onInvalidToken() {
			finish();
		}
	}

	public static abstract class PreferenceActivity extends android.preference.PreferenceActivity implements ChannelListener {
		protected final String TAG = TAG_PREFIX + Utils.getClassName(this.getClass());

		@Override
		public void onDestroy() {
			super.onDestroy();
			// make sure the service keeps running
			startService();
		}

		// to be overridden if needed
		@Override
		public void onChannelAvailable() {
		}

		// to be overridden if needed
		@Override
		public void onChannelCancel() {
			finish();
		}

		@Override
		public Context getContext() {
			return this;
		}

		protected boolean isServiceRunning() {
			return Base.isServiceRunning(this);
		}

		protected void startService() {
			Base.startService(this);
		}
	}

	public static abstract class ProtectedPreferenceActivity extends PreferenceActivity {
		@Override
		protected void onCreate(final Bundle state) {
			super.onCreate(state);
			if (!checkIntent(getIntent())) {
				Utils.Log.e(TAG, "invalid token");
				onInvalidToken();
			}
		}

		protected void onInvalidToken() {
			finish();
		}
	}
}
