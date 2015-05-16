package hr.ravilov.wifinator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Process;
import android.preference.PreferenceManager;

@SuppressLint("NewApi")
public class MainService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final String TAG = Base.TAG_PREFIX + Utils.getClassName(this.getClass());
	private final static int REQUEST_TIMER = 10;
	private final static int MINUTE_MILLIS = 60 * 1000;
	private final static int NM_ID = Process.myPid();

	public class LocalBinder extends Binder {
		public MainService getService() {
			return MainService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();
	protected final Config mConfig = new Config(this);
	protected boolean mLocked = true;
	protected boolean mTimerRunning = false;
	private PendingIntent fireIntent = null;
	private Compat compat = null;

	@Override
	public IBinder onBind(final Intent intent) {
		init();
		return mBinder;
	}

	@Override
	public void onStart(final Intent intent, final int startId) {
		super.onStart(intent, startId);
		onStartCommand(intent, 0, startId);
	}

	//@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		if (compat == null) {
			init();
			checkPeriod();
			PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		}
		return 1;	// Service.START_STICKY
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		if (compat != null) {
			compat.stopForeground(NM_ID);
		}
		compat = null;
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
		mConfig.onChange(prefs, key);
	}

	private void init() {
		mConfig.initLoad();
		final Notification n = new Notification(/* R.drawable.transparent */ 0, "", 0);
		n.setLatestEventInfo(this, "", "", PendingIntent.getBroadcast(this, NM_ID, new Intent(), 0));
		compat = new Compat(this);
		compat.startForeground(NM_ID, n);
		resetLock();
		resetTimer();
	}

	public Config getConfig() {
		return mConfig;
	}

	public boolean isLocked() {
		return mLocked;
	}

	public void setLocked(final boolean value) {
		mLocked = value;
	}

	private boolean checkPeriod() {
		final Calendar now = Calendar.getInstance();
		if (mConfig.lastPeriodStart.after(now) || getPeriodEnd(mConfig.lastPeriodStart).before(now)) {
			resetPeriod();
			return true;
		}
		return false;
	}

	public Calendar getPeriodBegin() {
		if (!mTimerRunning) {
			if (checkPeriod()) {
				mConfig.save();
			}
		}
		return getPeriodBegin(mConfig.lastPeriodStart);
	}

	private Calendar getPeriodBegin(final Calendar ref) {
		final Calendar cal = (Calendar)ref.clone();
		switch (mConfig.timePeriodType) {
			case YEAR: {
				cal.set(Calendar.MONTH, cal.getActualMinimum(Calendar.MONTH));
				cal.get(Calendar.MONTH);
				// fallthru
			}
			case MONTH: {
				cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
				cal.get(Calendar.DAY_OF_MONTH);
				// fallthru
			}
			case WEEK: {
				cal.set(Calendar.DAY_OF_WEEK, cal.getActualMinimum(Calendar.DAY_OF_WEEK));
				cal.get(Calendar.DAY_OF_WEEK);
				// fallthru
			}
			case DAY: {
				cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
				cal.get(Calendar.HOUR_OF_DAY);
				// fallthru
			}
			case HOUR: {
				cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
				cal.get(Calendar.MINUTE);
				// fallthru
			}
			case MINUTE: {
				cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
				cal.get(Calendar.SECOND);
				// fallthru
			}
			default: {
				break;
			}
		}
		return cal;
	}

	public Calendar getPeriodEnd() {
		return getPeriodEnd(getPeriodBegin());
	}

	private Calendar getPeriodEnd(final Calendar begin) {
		final Calendar cal = (Calendar)begin.clone();
		switch (mConfig.timePeriodType) {
			case YEAR: {
				cal.add(Calendar.YEAR, mConfig.timePeriodValue);
				cal.get(Calendar.YEAR);
				break;
			}
			case MONTH: {
				cal.add(Calendar.MONTH, mConfig.timePeriodValue);
				cal.get(Calendar.MONTH);
				break;
			}
			case WEEK: {
				cal.add(Calendar.WEEK_OF_YEAR, mConfig.timePeriodValue);
				cal.get(Calendar.WEEK_OF_YEAR);
				break;
			}
			case DAY: {
				cal.add(Calendar.DAY_OF_MONTH, mConfig.timePeriodValue);
				cal.get(Calendar.DAY_OF_MONTH);
				break;
			}
			case HOUR: {
				cal.add(Calendar.HOUR, mConfig.timePeriodValue);
				cal.get(Calendar.HOUR);
				break;
			}
			case MINUTE: {
				cal.add(Calendar.MINUTE, mConfig.timePeriodValue);
				cal.get(Calendar.MINUTE);
				break;
			}
			default: {
				break;
			}
		}
		cal.add(Calendar.SECOND, -1);
		cal.get(Calendar.SECOND);
		return cal;
	}

	public long getTimeLimit() {
		if (!mTimerRunning) {
			if (checkPeriod()) {
				mConfig.save();
			}
		}
		return mConfig.timeLimitValue * mConfig.timeLimitType.value();
	}

	public void resetPeriod() {
		mConfig.lastPeriodStart.setTime(getPeriodBegin(Calendar.getInstance()).getTime());
		mConfig.timeUsed = 0;
	}

	public void resetLock() {
		final WifiUtils.State state = WifiUtils.getState(this);
		if (state.equals(WifiUtils.State.ENABLED)) {
			setLocked(false);
		} else {
			setLocked(true);
		}
	}

	public void resetTimer() {
		final WifiUtils.State state = WifiUtils.getState(this);
		if (state.equals(WifiUtils.State.ENABLED)) {
			if (mConfig.wifiMode.equals(Config.WifiMode.TIMED)) {
				startTimer();
			} else {
				stopTimer();
			}
		} else {
			stopTimer();
		}
	}

	public void resetRestrictions() {
		resetPeriod();
		resetLock();
		resetTimer();
	}

	private PendingIntent makeIntent() {
		if (fireIntent == null) {
			final Intent intent = new Intent(this, MainReceiver.OnServiceTick.class);
			intent.setFlags(MainReceiver.INTENT_FLAGS);
			fireIntent = PendingIntent.getActivity(this, REQUEST_TIMER, Base.protectedIntent(intent), PendingIntent.FLAG_UPDATE_CURRENT);
		}
		return fireIntent;
	}

	public boolean startTimer() {
		if (mTimerRunning) {
			return true;
		}
		checkPeriod();
		if (mConfig.timeUsed >= getTimeLimit()) {
			return false;
		}
		((AlarmManager)getSystemService(Context.ALARM_SERVICE)).setRepeating(
			AlarmManager.ELAPSED_REALTIME,
			SystemClock.elapsedRealtime() + MINUTE_MILLIS - (Calendar.getInstance().get(Calendar.SECOND) * 1000),
			MINUTE_MILLIS,
			makeIntent()
		);
		Utils.Log.i(TAG, "timer started");
		mTimerRunning = true;
		return true;
	}

	public void stopTimer() {
		if (!mTimerRunning) {
			return;
		}
		try {
			((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(makeIntent());
		}
		catch (final Throwable ignore) { }
		Utils.Log.i(TAG, "timer stopped");
		mTimerRunning = false;
	}

	public boolean isTimerRunning() {
		return mTimerRunning;
	}

	public void timerTick() {
		if (checkPeriod()) {
			Utils.Log.i(TAG, "period under/overflow, reset limits");
			mConfig.save();
		} else if (WifiUtils.isConnected(this)) {
			final long limit = getTimeLimit();
			if (++mConfig.timeUsed >= limit) {
				mConfig.timeUsed = limit;
				try {
					WifiUtils.disable(this);
				}
				catch (final Throwable ignore) { }
				Utils.Log.i(TAG, "period limit reached");
				stopTimer();
			}
			mConfig.save();
		} else {
			Utils.Log.i(TAG, "not connected, ignoring tick");
		}
	}

	// original: http://developer.android.com/reference/android/app/Service.html#startForeground%28int,%20android.app.Notification%29
	private static class Compat {
		private NotificationManager nm = null;
		private Service context = null;
		private static Method mStartForeground = null;
		private static Method mStopForeground = null;
		static {
			try {
				mStartForeground = Service.class.getMethod("startForeground", new Class<?>[] {
					int.class,
					Notification.class,
				});
				mStopForeground = Service.class.getMethod("stopForeground", new Class<?>[] {
					boolean.class,
				});
			}
			catch (final Throwable ex) {
				mStartForeground = null;
				mStopForeground = null;
			}
		}

		public Compat(final Service ctx) {
			context = ctx;
			nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		}

		public void startForeground(final int id, final Notification n) {
			if (mStartForeground != null) {
				invoke(mStartForeground, new Object[] {
					id,
					n,
				});
			} else {
				context.setForeground(true);
//				if (id > 0 && n != null) {
//					nm.notify(id, n);
//				}
			}
		}

		public void stopForeground(final int id) {
			if (mStopForeground != null) {
				invoke(mStopForeground, new Object[] {
					true,
				});
			} else {
				clearNotification(id);
				context.setForeground(false);
			}
		}

		public void clearNotification(final int id) {
			try {
				if (id > 0) {
					nm.cancel(id);
				}
			}
			catch (final Throwable ignore) { }
		}

		private Object invoke(final Method m, final Object... args) {
			try {
				return m.invoke(context, args);
			}
			catch (final InvocationTargetException ignore) { }
			catch (final IllegalAccessException ignore) { }
			return null;
		}
	}

	public static class Channel implements ProgressDialog.OnCancelListener {
		private MainService mService = null;
		private Base.ChannelListener mListener = null;
		private ProgressDialog pd = null;

		private ServiceConnection mConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(final ComponentName name, final IBinder service) {
				mService = ((LocalBinder)service).getService();
				mListener.onChannelAvailable();
				if (pd != null) {
					pd.dismiss();
					pd = null;
				}
			}

			@Override
			public void onServiceDisconnected(final ComponentName name) {
				mService = null;
			}
		};

		@Override
		public void onCancel(final DialogInterface dialog) {
			dialog.dismiss();
			mListener.onChannelCancel();
		}

		public Channel(final Base.ChannelListener listener) {
			mListener = listener;
		}

		public Channel open() {
			return open(true);
		}

		public Channel open(final boolean wait) {
			//mListener.getContext().startService(new Intent(mListener.getContext(), MainService.class));
			if (!mListener.getContext().bindService(new Intent(mListener.getContext(), MainService.class), mConnection, Context.BIND_AUTO_CREATE)) {
				throw new IllegalStateException("Unable to bind to service");
			}
			pd = null;
			if (wait) {
				pd = ProgressDialog.show(mListener.getContext(), "", mListener.getContext().getText(R.string.please_wait), true, true);
				pd.setOnCancelListener(this);
			}
			return this;
		}

		public void close() {
			mListener.getContext().unbindService(mConnection);
			mService = null;
		}

		public MainService getService() {
			return mService;
		}
	}
}
