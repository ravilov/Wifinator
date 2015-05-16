package hr.ravilov.wifinator;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.SparseArray;

public class WifiUtils {
	public static enum State {
		UNKNOWN (WifiManager.WIFI_STATE_UNKNOWN),
		DISABLED (WifiManager.WIFI_STATE_DISABLED),
		DISABLING (WifiManager.WIFI_STATE_DISABLING),
		ENABLED (WifiManager.WIFI_STATE_ENABLED),
		ENABLING (WifiManager.WIFI_STATE_ENABLING);

		private static final State _default = UNKNOWN;
		private static final SparseArray<State> sLookup = new SparseArray<State>();
		static {
			for (final State s : values()) {
				if (s.equals(_default)) {
					continue;
				}
				sLookup.put(s.value(), s);
			}
		}

		private int value;

		private State(final int v) {
			value = v;
		}

		public int value() {
			return value;
		}

		public static State find(final int v) {
			return find(v, _default);
		}

		public static State find(final int v, final State def) {
			return Utils.coalesce(sLookup.get(v), def);
		}
	}

	public static final String BROADCAST_CHANGED = WifiManager.WIFI_STATE_CHANGED_ACTION;
	public static final String INTENT_NEW_STATE = WifiManager.EXTRA_WIFI_STATE;
	public static final String INTENT_PREVIOUS_STATE = WifiManager.EXTRA_PREVIOUS_WIFI_STATE;

	public static boolean enable(final Context context) {
		return ((WifiManager)context.getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
	}

	public static boolean disable(final Context context) {
		return ((WifiManager)context.getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(false);
	}

	public static int getStateRaw(final Context context) {
		return ((WifiManager)context.getSystemService(Context.WIFI_SERVICE)).getWifiState();
	}

	public static State getState(final Context context) {
		return State.find(getStateRaw(context));
	}

	public static boolean isConnected(final Context context) {
		if (!getState(context).equals(State.ENABLED)) {
			return false;
		}
		final WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		if (wifi == null || wifi.getConnectionInfo() == null) {
			return false;
		}
		return (wifi.getConnectionInfo().getIpAddress() != 0) ? true : false;
	}
}
