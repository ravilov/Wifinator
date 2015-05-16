package hr.ravilov.wifinator;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.util.SparseIntArray;

public class Config {
	public String adminPasswordHash = null;
	public String wifiPasswordHash = null;
	public WifiMode wifiMode = WifiMode.OPEN;
	public int timeLimitValue = 2;
	public TimePeriod timeLimitType = TimePeriod.HOUR;
	public int timePeriodValue = 1;
	public TimePeriod timePeriodType = TimePeriod.DAY;
	public Calendar lastPeriodStart = Calendar.getInstance();
	public long timeUsed = 0;

	public static enum WifiMode {
		OPEN,
		PROTECTED,
		TIMED,
		LOCKED,
		UNKNOWN;

		private static final WifiMode _default = UNKNOWN;
		private static final SparseArray<WifiMode> sLookup1 = new SparseArray<WifiMode>();
		private static final HashMap<String, WifiMode> sLookup2 = new HashMap<String, WifiMode>();
		private static final SparseArray<WifiMode> sLookup3 = new SparseArray<WifiMode>();
		static {
			for (final WifiMode wm : values()) {
				if (wm.equals(_default)) {
					continue;
				}
				sLookup1.put(wm.value(), wm);
				sLookup2.put(wm.asString(), wm);
				sLookup3.put(wm.ordinal(), wm);
			}
		}

		public int value() {
			return ordinal() + 1;
		}

		public static WifiMode find(final int v) {
			return find(v, _default);
		}

		public static WifiMode find(final int v, final WifiMode def) {
			return Utils.coalesce(sLookup1.get(v), def);
		}

		public static WifiMode find(final String v) {
			return find(v, _default);
		}

		public static WifiMode find(final String v, final WifiMode def) {
			if (v == null || v.equals("")) {
				return def;
			}
			return Utils.coalesce(sLookup2.get(v), def);
		}

		public static WifiMode findByIndex(final int i) {
			return findByIndex(i, _default);
		}

		public static WifiMode findByIndex(final int i, final WifiMode def) {
			return Utils.coalesce(sLookup3.get(i), def);
		}

		private static String asString(final String v) {
			return v.toLowerCase(Locale.US);
		}

		public String asString() {
			return asString(toString());
		}
	}

	public static enum TimePeriod {
		MINUTE (1),
		HOUR (60),
		DAY (60 * 24),
		WEEK (60 * 24 * 7),
		MONTH (60 * 24 * 30),
		YEAR (60 * 24 * 365),
		UNKNOWN (0);

		private static final TimePeriod _default = UNKNOWN;
		private static final SparseArray<TimePeriod> sLookup1 = new SparseArray<TimePeriod>();
		private static final HashMap<String, TimePeriod> sLookup2 = new HashMap<String, TimePeriod>();
		private static final SparseArray<TimePeriod> sLookup3 = new SparseArray<TimePeriod>();
		static {
			for (final TimePeriod tp : values()) {
				if (tp.equals(_default)) {
					continue;
				}
				sLookup1.put(tp.value(), tp);
				sLookup2.put(tp.asString(), tp);
				sLookup3.put(tp.ordinal(), tp);
			}
		}

		private final int value;

		private TimePeriod(final int v) {
			value = v;
		}

		public int value() {
			return value;
		}

		public static TimePeriod find(final int v) {
			return find(v, _default);
		}

		public static TimePeriod find(final int v, final TimePeriod def) {
			return Utils.coalesce(sLookup1.get(v), def);
		}

		public static TimePeriod find(final String v) {
			return find(v, _default);
		}

		public static TimePeriod find(final String v, final TimePeriod def) {
			if (v == null || v.equals("")) {
				return def;
			}
			return Utils.coalesce(sLookup2.get(v), def);
		}

		public static TimePeriod findByIndex(final int i) {
			return findByIndex(i, _default);
		}

		public static TimePeriod findByIndex(final int i, final TimePeriod def) {
			return Utils.coalesce(sLookup3.get(i), def);
		}

		private static String asString(final String v) {
			return v.toLowerCase(Locale.US);
		}

		public String asString() {
			return asString(toString());
		}
	}

	private static interface Pref<T> {
		public void load(final Config main, final SharedPreferences prefs, final String key);
		public void save(final Config main, final SharedPreferences.Editor editor, final String key);
		public void copy(final Config main, final Config other);
		public T getValue(final Config main);
		public T getDefault(final Config main);
		public void setDefault(final Config main);
	}
	private static abstract class PrefInteger implements Pref<Integer> {
		protected abstract void _set(final Config main, final Integer value);
		protected abstract Integer _get(final Config main);
		protected abstract int _def();

		protected Integer def(final Config main) {
			final int resId = _def();
			if (resId <= 0) {
				return 0;
			}
			return main.mContext.getResources().getInteger(resId);
		}

		@Override
		public void load(final Config main, final SharedPreferences prefs, final String key) {
			_set(main, prefs.getInt(key, getValue(main)));
		}

		@Override
		public void save(final Config main, final SharedPreferences.Editor editor, final String key) {
			editor.putInt(key, getValue(main));
		}

		@Override
		public void copy(final Config main, final Config other) {
			_set(main, _get(other));
		}

		@Override
		public Integer getValue(final Config main) {
			return _get(main);
		}

		@Override
		public Integer getDefault(final Config main) {
			return def(main);
		}

		@Override
		public void setDefault(final Config main) {
			_set(main, getDefault(main));
		}
	}
	private static abstract class PrefLong implements Pref<Long> {
		protected abstract void _set(final Config main, final Long value);
		protected abstract Long _get(final Config main);
		protected abstract int _def();

		protected Long def(final Config main) {
			final int resId = _def();
			if (resId <= 0) {
				return Long.valueOf(0);
			}
			return Long.valueOf(main.mContext.getResources().getInteger(resId));
		}

		@Override
		public void load(final Config main, final SharedPreferences prefs, final String key) {
			_set(main, prefs.getLong(key, getValue(main)));
		}

		@Override
		public void save(final Config main, final SharedPreferences.Editor editor, final String key) {
			editor.putLong(key, getValue(main));
		}

		@Override
		public void copy(final Config main, final Config other) {
			_set(main, _get(other));
		}

		@Override
		public Long getValue(final Config main) {
			return _get(main);
		}

		@Override
		public Long getDefault(final Config main) {
			return def(main);
		}

		@Override
		public void setDefault(final Config main) {
			_set(main, getDefault(main));
		}
	}
	private static abstract class PrefPassword implements Pref<String> {
		protected abstract void _set(final Config main, final String value);
		protected abstract String _get(final Config main);
		protected abstract int _def();

		protected String def(final Config main) {
			final int resId = _def();
			if (resId <= 0) {
				return null;
			}
			return main.mContext.getResources().getString(resId);
		}

		@Override
		public void load(final Config main, final SharedPreferences prefs, final String key) {
			_set(main, prefs.getString(key, getValue(main)));
			if (_get(main) != null && _get(main).equals("")) {
				_set(main, null);
			}
		}

		@Override
		public void save(final Config main, final SharedPreferences.Editor editor, final String key) {
			final String value = getValue(main);
			editor.putString(key, (value == null) ? "" : value);
		}

		@Override
		public void copy(final Config main, final Config other) {
			_set(main, _get(other));
		}

		@Override
		public String getValue(final Config main) {
			return _get(main);
		}

		@Override
		public String getDefault(final Config main) {
			final String ret = def(main);
			return (ret == null || ret.equals("")) ? null : Utils.makeHash(ret);
		}

		@Override
		public void setDefault(final Config main) {
			_set(main, getDefault(main));
		}
	}
	private static abstract class PrefWifiMode implements Pref<String> {
		protected abstract void _set(final Config main, final WifiMode value);
		protected abstract WifiMode _get(final Config main);
		protected abstract int _def();

		protected WifiMode def(final Config main) {
			final int resId = _def();
			if (resId <= 0) {
				return WifiMode.find(null);
			}
			return WifiMode.find(main.mContext.getResources().getString(resId));
		}

		@Override
		public void load(final Config main, final SharedPreferences prefs, final String key) {
			_set(main, WifiMode.find(prefs.getString(key, getValue(main))));
		}

		@Override
		public void save(final Config main, final SharedPreferences.Editor editor, final String key) {
			editor.putString(key, getValue(main));
		}

		@Override
		public void copy(final Config main, final Config other) {
			_set(main, _get(other));
		}

		@Override
		public String getValue(final Config main) {
			return _get(main).asString();
		}

		@Override
		public String getDefault(final Config main) {
			return def(main).asString();
		}

		@Override
		public void setDefault(final Config main) {
			_set(main, def(main));
		}
	}
	private static abstract class PrefTimePeriod implements Pref<String> {
		protected abstract void _set(final Config main, final TimePeriod value);
		protected abstract TimePeriod _get(final Config main);
		protected abstract int _def();

		protected TimePeriod def(final Config main) {
			final int resId = _def();
			if (resId <= 0) {
				return TimePeriod.find(null);
			}
			return TimePeriod.find(main.mContext.getResources().getString(resId));
		}

		@Override
		public void load(final Config main, final SharedPreferences prefs, final String key) {
			_set(main, TimePeriod.find(prefs.getString(key, getValue(main))));
		}

		@Override
		public void save(final Config main, final SharedPreferences.Editor editor, final String key) {
			editor.putString(key, getValue(main));
		}

		@Override
		public void copy(final Config main, final Config other) {
			_set(main, _get(other));
		}

		@Override
		public String getValue(final Config main) {
			return _get(main).asString();
		}

		@Override
		public String getDefault(final Config main) {
			return def(main).asString();
		}

		@Override
		public void setDefault(final Config main) {
			_set(main, def(main));
		}
	}
	private static abstract class PrefCalendar implements Pref<String> {
		protected abstract void _set(final Config main, final Calendar value);
		protected abstract Calendar _get(final Config main);
		protected abstract int _def();
		private final static SparseIntArray monthNr = new SparseIntArray();
		static {
			monthNr.put(Calendar.JANUARY, 1);
			monthNr.put(Calendar.FEBRUARY, 2);
			monthNr.put(Calendar.MARCH, 3);
			monthNr.put(Calendar.APRIL, 4);
			monthNr.put(Calendar.MAY, 5);
			monthNr.put(Calendar.JUNE, 6);
			monthNr.put(Calendar.JULY, 7);
			monthNr.put(Calendar.AUGUST, 8);
			monthNr.put(Calendar.SEPTEMBER, 9);
			monthNr.put(Calendar.OCTOBER, 10);
			monthNr.put(Calendar.NOVEMBER, 11);
			monthNr.put(Calendar.DECEMBER, 12);
		}

		protected Calendar def(final Config main) {
			return def(main, null);
		}

		protected Calendar def(final Config main, final Calendar def) {
			final int resId = _def();
			if (resId <= 0) {
				return def;
			}
			final Calendar ret = fromString(main.mContext.getResources().getString(resId));
			return (ret == null) ? def : ret;
		}

		private static String trimLeadingZeros(final String str, final String def) {
			if (str == null || str.equals("")) {
				return def;
			}
			int i = 0;
			while (str.substring(i, 1).equals("0") && i < str.length() - 1) {
				i++;
			}
			return (i >= str.length()) ? "0" : str.substring(i);
		}

		private static interface Parser {
			public boolean parse(final String input, final Calendar cal);
		}

		private static final Parser[] parsers = new Parser[] {
			new Parser() {
				private final Pattern pattern = Pattern.compile("^(\\d+)[/.-](\\d+)[/.-](\\d+)\\s+(\\d+)[\\:](\\d+)[\\:](\\d+)(?:\\s+([+-]\\d+))?$");
				private final int[] fields = new int[] {
					Calendar.YEAR,
					Calendar.MONTH,
					Calendar.DAY_OF_MONTH,
					Calendar.HOUR_OF_DAY,
					Calendar.MINUTE,
					Calendar.SECOND,
				};

				@Override
				public boolean parse(final String input, final Calendar cal) {
					final Matcher match = pattern.matcher(input);
					if (!match.matches()) {
						return false;
					}
					if (match.groupCount() >= fields.length + 1) {
						final TimeZone tz = TimeZone.getTimeZone("GMT" + match.group(fields.length + 1));
						final String[] ids = TimeZone.getAvailableIDs(tz.getRawOffset());
						cal.setTimeZone((ids == null || ids.length <= 0) ? tz : TimeZone.getTimeZone(ids[0]));
					}
					for (int i = 0; i < fields.length; i++) {
						if (fields[i] <= 0) {
							continue;
						}
						final int value = Integer.valueOf(trimLeadingZeros(match.group(i + 1), String.valueOf(cal.get(fields[i]))));
						if (fields[i] == Calendar.MONTH) {
							cal.set(fields[i], monthNr.indexOfValue(value));
						} else {
							cal.set(fields[i], value);
						}
						cal.get(fields[i]);
					}
					return true;
				}
			},
		};

		protected static Calendar fromString(final String str) {
			if (str == null || str.equals("")) {
				return null;
			}
			final Calendar cal = Calendar.getInstance();
			for (final Parser parser : parsers) {
				if (parser.parse(str.trim(), cal)) {
					return cal;
				}
			}
			return null;
		}

		protected static String toString(final Calendar cal) {
			if (cal == null) {
				return null;
			}
			final float offset = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / (1000 * 60);
			return String.format(Locale.US, "%1$04d-%2$02d-%3$02d %4$02d:%5$02d:%6$02d %7$+03d%8$02d",
				cal.get(Calendar.YEAR),
				monthNr.get(cal.get(Calendar.MONTH)),
				cal.get(Calendar.DAY_OF_MONTH),
				cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE),
				cal.get(Calendar.SECOND),
				Math.round(Math.floor(offset / 60)),
				Math.round(Math.abs(offset) % 60)
			);
		}

		@Override
		public void load(final Config main, final SharedPreferences prefs, final String key) {
			final Calendar now = Calendar.getInstance();
			final Calendar cal = fromString(prefs.getString(key, getValue(main)));
			_set(main, (cal == null) ? def(main, now) : cal);
			if (_get(main).after(now.getTime())) {
				_set(main, now);
			}
		}

		@Override
		public void save(final Config main, final SharedPreferences.Editor editor, final String key) {
			editor.putString(key, getValue(main));
		}

		@Override
		public void copy(final Config main, final Config other) {
			_set(main, _get(other));
		}

		@Override
		public String getValue(final Config main) {
			return toString(_get(main));
		}

		@Override
		public String getDefault(final Config main) {
			final Calendar cal = def(main);
			return toString((cal == null) ? Calendar.getInstance() : cal);
		}

		@Override
		public void setDefault(final Config main) {
			_set(main, def(main, Calendar.getInstance()));
		}
	}
	private static final HashMap<String, Pref<?>> sPrefs = new HashMap<String, Pref<?>>();
	static {
		sPrefs.put("admin_password_hash", new PrefPassword() {
			@Override
			protected void _set(final Config main, final String value) {
				main.adminPasswordHash = value;
			}

			@Override
			protected String _get(final Config main) {
				return main.adminPasswordHash;
			}

			@Override
			protected int _def() {
				return R.string.default_admin_password;
			}
		});
		sPrefs.put("wifi_password_hash", new PrefPassword() {
			@Override
			protected void _set(final Config main, final String value) {
				main.wifiPasswordHash = value;
			}

			@Override
			protected String _get(final Config main) {
				return main.wifiPasswordHash;
			}

			@Override
			protected int _def() {
				return R.string.default_wifi_password;
			}
		});
		sPrefs.put("wifi_mode", new PrefWifiMode() {
			@Override
			protected void _set(final Config main, final WifiMode value) {
				main.wifiMode = value;
			}

			@Override
			protected WifiMode _get(final Config main) {
				return main.wifiMode;
			}

			@Override
			protected int _def() {
				return R.string.default_wifi_mode;
			}
		});
		sPrefs.put("time_limit_value", new PrefInteger() {
			@Override
			protected void _set(final Config main, final Integer value) {
				main.timeLimitValue = value;
			}

			@Override
			protected Integer _get(final Config main) {
				return main.timeLimitValue;
			}

			@Override
			protected int _def() {
				return R.integer.default_time_limit_value;
			}
		});
		sPrefs.put("time_limit_type", new PrefTimePeriod() {
			@Override
			protected void _set(final Config main, final TimePeriod value) {
				main.timeLimitType = value;
			}

			@Override
			protected TimePeriod _get(final Config main) {
				return main.timeLimitType;
			}

			@Override
			protected int _def() {
				return R.string.default_time_limit_type;
			}
		});
		sPrefs.put("time_period_value", new PrefInteger() {
			@Override
			protected void _set(final Config main, final Integer value) {
				main.timePeriodValue = value;
			}

			@Override
			protected Integer _get(final Config main) {
				return main.timePeriodValue;
			}

			@Override
			protected int _def() {
				return R.integer.default_time_period_value;
			}
		});
		sPrefs.put("time_period_type", new PrefTimePeriod() {
			@Override
			protected void _set(final Config main, final TimePeriod value) {
				main.timePeriodType = value;
			}

			@Override
			protected TimePeriod _get(final Config main) {
				return main.timePeriodType;
			}

			@Override
			protected int _def() {
				return R.string.default_time_period_type;
			}
		});
		sPrefs.put("last_period_start", new PrefCalendar() {
			@Override
			protected void _set(final Config main, final Calendar value) {
				main.lastPeriodStart.setTime(value.getTime());
			}

			@Override
			protected Calendar _get(final Config main) {
				return main.lastPeriodStart;
			}

			@Override
			protected int _def() {
				return R.string.default_time_period_type;
			}
		});
		sPrefs.put("time_used", new PrefLong() {
			@Override
			protected void _set(final Config main, final Long value) {
				main.timeUsed = value;
			}

			@Override
			protected Long _get(final Config main) {
				return main.timeUsed;
			}

			@Override
			protected int _def() {
				return R.integer.default_time_used;
			}
		});
	}

	protected Context mContext = null;
	private boolean loaded = false;
	private boolean saving = false;

	public Config() {
		mContext = null;
	}

	public Config(final Config other) {
		mContext = null;
		setFrom(other);
	}

	public Config(final Context context) {
		mContext = context;
		try {
			init();
		}
		catch (final Throwable ignore) { }
	}

	public Config init() {
		if (mContext == null) {
			return this;
		}
		for (final String key : sPrefs.keySet()) {
			sPrefs.get(key).setDefault(this);
		}
		return this;
	}

	public Config load() {
		if (mContext == null) {
			return this;
		}
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		for (final String key : sPrefs.keySet()) {
			try {
				sPrefs.get(key).load(this, prefs, key);
			}
			catch (final Throwable ignore) { }
		}
		return this;
	}

	public Config initLoad() {
		if (loaded) {
			return this;
		}
		loaded = true;
		init();
		return load();
	}

	public Config save() {
		if (mContext == null) {
			return this;
		}
		saving = true;
		final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
		editor.clear();
		for (final String key : sPrefs.keySet()) {
			sPrefs.get(key).save(this, editor, key);
		}
		editor.commit();
		saving = false;
		return this;
	}

	public Config setFrom(final Config other) {
		if (other == null) {
			return this;
		}
		for (final String key : sPrefs.keySet()) {
			sPrefs.get(key).copy(this, other);
		}
		return this;
	}

	public void onChange(final SharedPreferences prefs, final String key) {
		if (saving) {
			return;
		}
		final Pref<?> p = sPrefs.get(key);
		if (p == null) {
			return;
		}
		p.load(this, prefs, key);
	}

	@Override
	public String toString() {
		final String[] list = new String[sPrefs.keySet().size()];
		int i = 0;
		for (final String key : sPrefs.keySet()) {
			list[i++] = String.format(Locale.US, "%1$s=%2$s",
				key,
				sPrefs.get(key).getValue(this)
			);
		}
		return String.format(Locale.US, "%1$s(%2$s)",
			Utils.getClassName(getClass()),
			Utils.join(", ", list)
		);
	}
}
