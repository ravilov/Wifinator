package hr.ravilov.wifinator;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.util.TypedValue;

public class Utils {
	public static final boolean LOGGING = true;	//BuildConfig.DEBUG;

	private static String myVersion = null;
	private static String myBuild = null;

	// list taken from: https://github.com/keesj/gomo/wiki/AndroidSecurityUserAndGroups
	private static enum Uids {
		ROOT (0),	// traditional unix root user
		SYSTEM (1000),	// system server
		RADIO (1001),	// telephony subsystem, RIL
		BLUETOOTH (1002),	// bluetooth subsystem
		GRAPHICS (1003),	// graphics devices
		INPUT (1004),	// input devices
		AUDIO (1005),	// audio devices
		CAMERA (1006),	// camera devices
		LOG (1007),	// log devices
		COMPASS (1008),	// compass device
		MOUNT (1009),	// mountd socket
		WIFI (1010),	// wifi subsystem
		ADB (1011),	// android debug bridge (adbd)
		INSTALL (1012),	// group for installing packages
		MEDIA (1013),	// mediaserver process
		DHCP (1014),	// dhcp client
		SDCARD_RW (1015),	// external storage write access
		VPN (1016),	// vpn system
		KEYSTORE (1017),	// keystore subsystem
		SHELL (2000),	// adb and debug shell user
		CACHE (2001),	// cache access
		DIAG (2002),	// access to diagnostic resources
		NET_BT_ADMIN (3001),	// bluetooth: create any socket
		NET_BT (3002),	// bluetooth: create sco, rfcomm or l2cap sockets
		INET (3003),	// can create AF_INET and AF_INET6 sockets
		NET_RAW (3004),	// can create raw INET sockets
		NET_ADMIN (3005),	// can configure interfaces and routing tables
		MOT_ACCY (9000),	// access to accessory
		MOT_PWRIC (9001),	// power IC
		MOT_USB (9002),	// mot usb
		MOT_DRM (9003),	// can access DRM resource
		MOT_TCMD (9004),	// mot_tcmd
		MOT_SEC_RTC (9005),	// mot cpcap rtc
		MOT_TOMBSTONE (9006),
		MOT_TPAPI (9007),	// mot_tpapi
		MOT_SECCLKD (9008),	// mot_secclkd
		MISC (9998),	// access to misc storage
		NOBODY (9999),
		APP (10000),	// first app user
		;

		private final static Uids _default = null;
		private static final Map<String, Uids> sUnames = new HashMap<String, Uids>();
		private static final SparseArray<Uids> sUids = new SparseArray<Uids>();
		static {
			for (final Uids x : values()) {
				if (x.equals(_default)) {
					continue;
				}
				sUids.put(x.getUid(), x);
				sUnames.put(x.getUsername(), x);
			}
		}

		private final int uid;

		private Uids(final int id) {
			uid = id;
		}

		public final int getUid() {
			return uid;
		}

		public final String getUsername() {
			return toString().toLowerCase(Locale.US);
		}

		public static Uids find(final String v) {
			return find(v, _default);
		}

		public static Uids find(final String v, final Uids def) {
			return Utils.coalesce(sUnames.get(v), def);
		}

		public static Uids find(final int v) {
			return find(v, _default);
		}

		public static Uids find(final int v, final Uids def) {
			return Utils.coalesce(sUids.get(v), def);
		}

		public static int getUid(final String uname) {
			if (uname == null) {
				return -1;
			}
			final String pfx = "app_";
			if (uname.substring(0, pfx.length()).equals(pfx)) {
				return APP.getUid() + Integer.valueOf(uname.substring(pfx.length()));
			}
			final Uids item = find(uname);
			if (item != null) {
				return item.getUid();
			}
			return -1;
		}

		public static String getUsername(final int uid) {
			if (uid >= APP.getUid()) {
				return String.format(Locale.US, "app_%1$d", uid - APP.getUid());
			}
			final Uids item = find(uid);
			if (item != null) {
				return item.getUsername();
			}
			return String.valueOf(uid);
		}
	}

	public static final String myPackage() {
		try {
			return Utils.class.getPackage().getName();
		}
		catch (final Throwable ex) { }
		return null;
	}

	public static String getClassName(Class<?> c) {
		if (c == null) {
			return null;
		}
		final List<String> l = Arrays.asList(c.getName().split("\\."));
		return (l == null) ? null : l.get(l.size() - 1);
	}

	public static String getMyVersion(final Context context) {
		if (myVersion == null) {
			try {
				myVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			}
			catch (final Throwable ex) { }
		}
		return myVersion;
	}

	public static String getMyBuild(final Context context) {
		if (myBuild == null) {
			try {
				myBuild = context.getResources().getString(R.string.class.getField("auto_build").getInt(0));
			}
			catch (final Throwable ex) { }
		}
		return myBuild;
	}

	public static boolean isSystemApp(final Context context) {
		return ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? true : false;
	}

	public static String getStackTrace(final Throwable ex) {
		final ByteArrayOutputStream s = new ByteArrayOutputStream();
		final PrintWriter pw = new PrintWriter(s);
		ex.printStackTrace(pw);
		pw.close();
		return s.toString();
	}

	public static String getExceptionMessage(final Throwable ex) {
		if (ex == null) {
			return null;
		}
		Throwable t = ex;
		while (t != null) {
			if (ex.getMessage() != null) {
				return ex.getMessage();
			}
			t = t.getCause();
		}
		return ex.toString();
	}

	public static String getUsername(final int uid) {
		return Uids.getUsername(uid);
	}

	public static int getUid(final String username) {
		return Uids.getUid(username);
	}

	public static final String join(final String sep, final Object[] list) {
		if (list == null || list.length <= 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final Object s : list) {
			if (!first) {
				sb.append(sep);
			}
			sb.append((s == null) ? s : ((s instanceof String) ? s : s.toString()));
			first = false;
		}
		return sb.toString();
	}

	public static <T> T coalesce(T... list) {
		if (list == null) {
			return null;
		}
		for (final T item : list) {
			if (item != null) {
				return item;
			}
		}
		return null;
	}

	public static String makeHash(final String data) {
		try {
			final byte[] result = MessageDigest.getInstance("MD5").digest(data.getBytes());
			String ret = "";
			for (int i = 0; i < result.length; i++) {
				ret += Integer.toString(result[i] & 0xFF, 16);
			}
			return ret;
		}
		catch (final Throwable ignore) { }
		return null;
	}

	public static Drawable resizeDrawable(final Context context, final int resId, final float widthDim, final float heightDim) {
		final Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resId);
		try {
			final float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, widthDim, context.getResources().getDisplayMetrics());
			final float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, heightDim, context.getResources().getDisplayMetrics());
			if (bmp.getWidth() > width || bmp.getHeight() > height) {
				return new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bmp, Math.round(width), Math.round(height), true));
			}
		}
		catch (final Throwable ex) { }
		return new BitmapDrawable(context.getResources(), bmp);
	}

	public static class Log {
		public static void i(final String tag, final String msg, final Object... args) {
			if (!LOGGING) {
				return;
			}
			android.util.Log.i(tag, String.format(Locale.US, msg, args));
		}

		public static void w(final String tag, final String msg, final Object... args) {
			if (!LOGGING) {
				return;
			}
			android.util.Log.w(tag, String.format(Locale.US, msg, args));
		}

		public static void d(final String tag, final String msg, final Object... args) {
			if (!LOGGING) {
				return;
			}
			android.util.Log.d(tag, String.format(Locale.US, msg, args));
		}

		public static void v(final String tag, final String msg, final Object... args) {
			if (!LOGGING) {
				return;
			}
			android.util.Log.v(tag, String.format(Locale.US, msg, args));
		}

		public static void e(final String tag, final String msg, final Object... args) {
			if (!LOGGING) {
				return;
			}
			android.util.Log.e(tag, String.format(Locale.US, msg, args));
		}

		public static void x(final String msg, final Object... args) {
			if (!LOGGING) {
				return;
			}
			android.util.Log.d("<>", String.format(Locale.US, msg, args));
		}
	}

	public static class Template {
		public static class Var {
			public final String name;
			public final String value;

			public Var(final String n, final String v) {
				name = n;
				value = v;
			}
		}

		public static String process(final String str, final Var... vars) {
			if (str == null || str.equals("")) {
				return str;
			}
			String ret = str;
			for (int i = 0 ;i < vars.length; i++) {
				final Matcher match = Pattern.compile("\\{" + Pattern.quote(vars[i].name.toUpperCase(Locale.US)) + "=([^\\}]*)\\}").matcher(ret);
				while (match.find()) {
					if (vars[i].value == null || vars[i].value.equals("")) {
						ret = match.replaceAll("");
					} else {
						ret = match.replaceAll(String.format(Locale.US, match.group(1), vars[i].value));
					}
				}
			}
			final Matcher match = Pattern.compile("\\{(?:[^=]+=)?[^\\}]*\\}").matcher(ret);
			while (match.find()) {
				ret = match.replaceAll("");
			}
			return ret;
		}
	}
}
