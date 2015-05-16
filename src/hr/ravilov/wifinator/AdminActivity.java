package hr.ravilov.wifinator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.preference.Preference;
import android.preference.ListPreference;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AdminActivity extends Base.ProtectedPreferenceActivity implements Preference.OnPreferenceClickListener, ListPreference.OnPreferenceChangeListener, PrefPassword.OnPasswordChangedListener, PrefTimePeriod.OnTimeChangedListener, Button.OnClickListener {
	protected static enum Pref {
		MASTER_PASSWORD ("pref_master_password"),
		SYSTEM_APP ("pref_system_app"),
		USER_APP ("pref_user_app"),
		WIFI_MODE ("pref_wifi_mode"),
		WIFI_PASSWORD ("pref_wifi_password"),
		TIME_LIMIT ("pref_time_limit"),
		UNKNOWN (null),
		;

		private static final Pref _default = UNKNOWN;
		private static final Map<String, Pref> sLookup = new HashMap<String, Pref>();
		static {
			for (final Pref x : values()) {
				if (x.equals(_default)) {
					continue;
				}
				sLookup.put(x.key(), x);
			}
		}

		private final String key;

		private Pref(final String k) {
			key = k;
		}

		public String key() {
			return key;
		}

		public static Pref find(final String v) {
			return find(v, _default);
		}

		public static Pref find(final String v, final Pref def) {
			return Utils.coalesce(sLookup.get(v), def);
		}
	}

	protected final Config newConfig = new Config();
	protected final HashMap<Pref, Preference> prefs = new HashMap<Pref, Preference>();
	protected MainService.Channel mChannel = null;
	protected Button ok = null;
	protected Button cancel = null;
	private static final String[] cacheDirs = new String[] {
		Utils.join(File.separator, new String[] { Environment.getDataDirectory().getAbsolutePath(), "dalvik-cache" }),
		Utils.join(File.separator, new String[] { "", "cache", "dalvik-cache" }),
		Utils.join(File.separator, new String[] { Environment.getDownloadCacheDirectory().getAbsolutePath(), "dalvik-cache" }),
		Utils.join(File.separator, new String[] { "", "sd-ext", "dalvik-cache" }),
	};

	@Override
	protected void onCreate(final Bundle saved) {
		super.onCreate(saved);
		setContentView(R.layout.admin);
		addPreferencesFromResource(R.xml.admin);
		startService();
		mChannel = (new MainService.Channel(this)).open();
		ok = (Button)findViewById(R.id.btn_ok);
		cancel = (Button)findViewById(R.id.btn_cancel);
		for (final Pref p : Pref.values()) {
			final String key = p.key();
			if (key == null || key.equals("")) {
				continue;
			}
			prefs.put(p, findPreference(key));
		}
		if (prefs.get(Pref.MASTER_PASSWORD) != null) {
			try {
				((PrefPassword)prefs.get(Pref.MASTER_PASSWORD)).setOnPasswordChangedListener(this);
			}
			catch (final Throwable ignore) { }
		}
		if (prefs.get(Pref.WIFI_MODE) != null) {
			try {
				((ListPreference)prefs.get(Pref.WIFI_MODE)).setOnPreferenceChangeListener(this);
			}
			catch (final Throwable ignore) { }
		}
		if (prefs.get(Pref.WIFI_PASSWORD) != null) {
			try {
				((PrefPassword)prefs.get(Pref.WIFI_PASSWORD)).setOnPasswordChangedListener(this);
			}
			catch (final Throwable ignore) { }
		}
		if (prefs.get(Pref.TIME_LIMIT) != null) {
			try {
				((PrefTimePeriod)prefs.get(Pref.TIME_LIMIT)).setOnTimeChangedListener(this);
			}
			catch (final Throwable ignore) { }
		}
		if (prefs.get(Pref.SYSTEM_APP) != null) {
			prefs.get(Pref.SYSTEM_APP).setOnPreferenceClickListener(this);
		}
		if (prefs.get(Pref.USER_APP) != null) {
			prefs.get(Pref.USER_APP).setOnPreferenceClickListener(this);
		}
		if (ok != null) {
			ok.setOnClickListener(this);
		}
		if (cancel != null) {
			cancel.setOnClickListener(this);
		}
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		setIntent(intent);
	}

	@Override
	public void onChannelAvailable() {
		super.onChannelAvailable();
		newConfig.setFrom(getConfig());
		try {
			mChannel.getService().stopTimer();
		}
		catch (final Throwable ignore) { }
		updateViews();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateViews();
	}

	@Override
	public void onPause() {
		super.onPause();
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

	protected Config getConfig() {
		try {
			return mChannel.getService().getConfig();
		}
		catch (final Throwable ignore) { }
		return null;
	}

	private void updateViews() {
		if (prefs.get(Pref.MASTER_PASSWORD) != null) {
			prefs.get(Pref.MASTER_PASSWORD).setSummary((newConfig.adminPasswordHash == null) ? R.string.pref_master_password_unset : R.string.pref_master_password_set);
			try {
				((PrefPassword)prefs.get(Pref.MASTER_PASSWORD)).setHasPassword((newConfig.adminPasswordHash == null) ? false : true);
			}
			catch (final Throwable ignore) { }
		}
		final boolean sysApp = Utils.isSystemApp(this);
		if (prefs.get(Pref.SYSTEM_APP) != null) {
			prefs.get(Pref.SYSTEM_APP).setEnabled(sysApp ? false : true);
		}
		if (prefs.get(Pref.USER_APP) != null) {
			prefs.get(Pref.USER_APP).setEnabled(sysApp ? true : false);
		}
		if (prefs.get(Pref.WIFI_MODE) != null) {
			try {
				final ListPreference list = (ListPreference)prefs.get(Pref.WIFI_MODE);
				list.setValue(newConfig.wifiMode.asString());
				list.setSummary(list.getEntry());
			}
			catch (final Throwable ignore) { }
		}
		if (prefs.get(Pref.WIFI_PASSWORD) != null) {
			prefs.get(Pref.WIFI_PASSWORD).setSummary((newConfig.wifiPasswordHash == null) ? R.string.pref_wifi_password_unset : R.string.pref_wifi_password_set);
			prefs.get(Pref.WIFI_PASSWORD).setEnabled(newConfig.wifiMode.equals(Config.WifiMode.PROTECTED) ? true : false);
			try {
				((PrefPassword)prefs.get(Pref.WIFI_PASSWORD)).setHasPassword((newConfig.wifiPasswordHash == null) ? false : true);
			}
			catch (final Throwable ignore) { }
		}
		if (prefs.get(Pref.TIME_LIMIT) != null) {
			try {
				prefs.get(Pref.TIME_LIMIT).setSummary(String.format(Locale.getDefault(), getString(R.string.pref_time_limit_),
					newConfig.timeLimitValue,
					getResources().getStringArray(R.array.time_period_names)[newConfig.timeLimitType.ordinal()],
					newConfig.timePeriodValue,
					getResources().getStringArray(R.array.time_period_names)[newConfig.timePeriodType.ordinal()]
				));
			}
			catch (final Throwable ignore) { }
			prefs.get(Pref.TIME_LIMIT).setEnabled(newConfig.wifiMode.equals(Config.WifiMode.TIMED) ? true : false);
			try {
				((PrefTimePeriod)prefs.get(Pref.TIME_LIMIT)).setParams((new PrefTimePeriod.TimeBundle())
					.setTimeAmount(newConfig.timeLimitValue)
					.setTimeUnit(newConfig.timeLimitType)
					.setPeriodAmount(newConfig.timePeriodValue)
					.setPeriodUnit(newConfig.timePeriodType)
				);
			}
			catch (final Throwable ignore) { }
		}
	}

	@Override
	public void onClick(final View view) {
		switch (view.getId()) {
			case R.id.btn_cancel: {
				finish();
				break;
			}
			case R.id.btn_ok: {
				getConfig().setFrom(newConfig);
				mChannel.getService().resetRestrictions();
				getConfig().save();
				Toast.makeText(this, getText(R.string.config_saved), Toast.LENGTH_SHORT).show();
				finish();
				break;
			}
			default: {
				break;
			}
		}
	}

	@Override
	public void onPasswordChanged(final Preference pref, final String password) {
		switch (Pref.find(pref.getKey())) {
			case MASTER_PASSWORD: {
				newConfig.adminPasswordHash = (password == null || password.equals("")) ? null : Utils.makeHash(password);
				updateViews();
				break;
			}
			case WIFI_PASSWORD: {
				newConfig.wifiPasswordHash = (password == null || password.equals("")) ? null : Utils.makeHash(password);
				updateViews();
				break;
			}
			default: {
				break;
			}
		}
	}

	@Override
	public void onTimeChanged(final Preference pref, final PrefTimePeriod.TimeBundle time) {
		switch (Pref.find(pref.getKey())) {
			case TIME_LIMIT: {
				newConfig.timeLimitValue = time.timeAmount;
				newConfig.timeLimitType = time.timeUnit;
				newConfig.timePeriodValue = time.periodAmount;
				newConfig.timePeriodType = time.periodUnit;
				updateViews();
				break;
			}
			default: {
				break;
			}
		}
	}

	@Override
	public boolean onPreferenceClick(final Preference pref) {
		switch (Pref.find(pref.getKey())) {
			case SYSTEM_APP: {
				(new AlertDialog.Builder(this))
					.setTitle(getText(R.string.title_system_app))
					.setMessage(Html.fromHtml(getString(R.string.message_system_app)))
					.setPositiveButton(getText(R.string.button_yes), new AlertDialog.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							dialog.dismiss();
							moveToSystem();
						}
					})
					.setNegativeButton(getText(R.string.button_no), new AlertDialog.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							dialog.dismiss();
						}
					})
					.create()
					.show()
				;
				return true;
			}
			case USER_APP: {
				(new AlertDialog.Builder(this))
					.setTitle(getText(R.string.title_user_app))
					.setMessage(Html.fromHtml(getString(R.string.message_user_app)))
					.setPositiveButton(getText(R.string.button_yes), new AlertDialog.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							dialog.dismiss();
							moveToUser();
						}
					})
					.setNegativeButton(getText(R.string.button_no), new AlertDialog.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							dialog.dismiss();
						}
					})
					.create()
					.show()
				;
				return true;
			}
			default: {
				break;
			}
		}
		return false;
	}

	@Override
	public boolean onPreferenceChange(final Preference pref, final Object value) {
		newConfig.wifiMode = Config.WifiMode.find((String)value);
		updateViews();
		return true;
	}

	private static class AdminRunner extends Shell.UiThreadRunner {
		public AdminRunner(final Activity activity) {
			super(activity);
		}

		@Override
		protected void onTaskGroup(final Task.Group group) {
			switch (group) {
				case PRE: {
					setMessage(R.string.dialog_prerun);
					return;
				}
				case MAIN: {
					setMessage(R.string.dialog_run);
					return;
				}
				case POST: {
					setMessage(R.string.dialog_postrun);
					return;
				}
				default: {
					break;
				}
			}
			super.onTaskGroup(group);
		}
	}

	private static class AdminTask extends Shell.SuperTask {
		protected final String MODE_DIR = String.format("%1$o", 0777);
		protected final String MODE_FILE = String.format("%1$o", 0666);

		@Override
		protected List<String> getPreScript() {
			final List<String> list = new ArrayList<String>();
			list.clear();
			list.addAll(Arrays.asList(new String[] {
				":",	// just to make sure we have root
			}));
			return list;
		}

		@Override
		protected List<String> getPostScript() {
			final List<String> reset = new ArrayList<String>();
			reset.clear();
			reset.add("toolbox stop");
			reset.add("toolbox sleep 1");
			final List<String> chunk = getResetScript();
			if (chunk != null && chunk.size() > 0) {
				reset.addAll(chunk);
				reset.add("toolbox sleep 1");
			}
			reset.add("toolbox start");
			final List<String> list = new ArrayList<String>();
			list.clear();
			list.addAll(Arrays.asList(new String[] {
				"exec toolbox sync",
				"exec toolbox sleep 1",
				Shell.Command.ignoreErrors(Shell.Command.make("daemonize /system/bin/sh -c '%1$s'",
					Utils.join(" ; ", reset.toArray(new String[reset.size()]))
				)),
			}));
			return list;
		}

		protected List<String> getResetScript() {
			return null;
		}

		protected static String rmDcCommand(final String apkname) {
			final String[] dcList = new String[cacheDirs.length];
			for (int i = 0; i < cacheDirs.length; i++) {
				dcList[i] = String.format(Locale.US, "%1$s/*%2$s*", cacheDirs[i], apkname);
			}
			return Shell.Command.makeUnquoted("for path in %1$s ; do [ -e \"${path}\" ] && rm \"${path}\" ; done", Utils.join(" ", dcList));
		}
	}

	private void moveToSystem() {
		(new AdminRunner(this)).setTitle(getString(R.string.title_system_app)).execute(new AdminTask() {
			private final String pkgname = Utils.myPackage();
			private final String apkpath = getApplicationInfo().sourceDir;
			private final String apkname = apkpath.substring(apkpath.lastIndexOf(File.separatorChar) + 1);
			private final String syspath = Environment.getRootDirectory().getAbsolutePath();
			private final String sysdev = Shell.getFilesystemDevice(syspath);
			private String destpath = null;
			private final String user = Utils.getUsername(0);

			@Override
			protected List<String> getScript() {
				final String subdir = ((new File(syspath + "/priv-app")).exists()) ? "priv-app" : "app";
				destpath = String.format(Locale.US, "%1$s/%2$s/%3$s.apk", syspath, subdir, pkgname);
				final List<String> list = new ArrayList<String>();
				list.clear();
				list.addAll(Arrays.asList(new String[] {
					Shell.Command.make("exec toolbox mount -oremount,rw '%1$s' '%2$s'", sysdev, syspath),
					Shell.Command.make("exec toolbox cat '%1$s' > '%2$s'", apkpath, destpath),
					Shell.Command.ignoreErrors(Shell.Command.make("toolbox chown '%2$s.%2$s' '%1$s' || toolbox chown '%2$s' '%1$s'", destpath, user)),
					Shell.Command.ignoreErrors(Shell.Command.make("toolbox chmod 644 '%1$s'", destpath)),
					Shell.Command.ignoreErrors(Shell.Command.make("exec toolbox mount -oremount,ro '%1$s' '%2$s'", sysdev, syspath)),
				}));
				return list;
			}

			@Override
			protected List<String> getResetScript() {
				return Arrays.asList(new String[] {
					Shell.Command.ignoreErrors(rmDcCommand(apkname)),
					Shell.Command.make("test -e '%1$s' && toolbox rm '%1$s'", apkpath),
				});
			}
		});
	}

	private void moveToUser() {
		(new AdminRunner(this)).setTitle(getString(R.string.title_system_app)).execute(new AdminTask() {
			private final String pkgname = Utils.myPackage();
			private final String apkpath = getApplicationInfo().sourceDir;
			private final String apkname = apkpath.substring(apkpath.lastIndexOf(File.separatorChar) + 1);
			private final String syspath = Environment.getRootDirectory().getAbsolutePath();
			private final String sysdev = Shell.getFilesystemDevice(syspath);
			private final String userpath = Environment.getDataDirectory().getAbsolutePath();
			private final String destpath = String.format(Locale.US, "%1$s/app/%2$s.apk", userpath, pkgname);
			private final String user = Utils.getUsername(Process.myUid());

			@Override
			protected List<String> getScript() {
				final List<String> list = new ArrayList<String>();
				list.clear();
				list.addAll(Arrays.asList(new String[] {
					Shell.Command.make("exec toolbox mount -oremount,rw '%1$s' '%2$s'", sysdev, syspath),
					Shell.Command.make("exec toolbox cat '%1$s' > '%2$s.tmp'", apkpath, destpath),
					Shell.Command.ignoreErrors(Shell.Command.make("exec toolbox mount -oremount,ro '%1$s' '%2$s'", sysdev, syspath)),
					Shell.Command.make("exec toolbox mv '%1$s.tmp' '%1$s'", destpath),
					Shell.Command.ignoreErrors(Shell.Command.make("toolbox chown '%2$s.%2$s' '%1$s' || toolbox chown '%2$s' '%1$s'", destpath, user)),
					Shell.Command.ignoreErrors(Shell.Command.make("toolbox chmod 644 '%1$s'", destpath)),
				}));
				// must wipe data because the SYSTEM->USER transition sometimes causes UID change
				for (final File file : Shell.getDataEntries()) {
					if (file.isDirectory()) {
						list.add(Shell.Command.ignoreErrors(Shell.Command.make("toolbox rmdir '%1$s'", file.getAbsolutePath())));
					} else {
						list.add(Shell.Command.ignoreErrors(Shell.Command.make("toolbox rm '%1$s'", file.getAbsolutePath())));
					}
				}
				return list;
			}

			@Override
			protected List<String> getResetScript() {
				return Arrays.asList(new String[] {
					Shell.Command.ignoreErrors(rmDcCommand(apkname)),
					Shell.Command.make("toolbox mount -oremount,rw '%1$s' '%2$s'", sysdev, syspath),
					Shell.Command.make("test -e '%1$s' && toolbox rm '%1$s'", apkpath),
					Shell.Command.make("toolbox mount -oremount,ro '%1$s' '%2$s'", sysdev, syspath),
				});
			}
		});
	}
}
