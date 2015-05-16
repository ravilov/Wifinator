package hr.ravilov.wifinator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Environment;
import android.text.Html;

public class Shell {
	private static final boolean LOGGING = true;

	public static class Command {
		private final String command;

		public Command(final String cmd) {
			command = cmd;
		}

		public final String getCommand() {
			return command;
		}

		public String getQuotedCommand() {
			return "'" + quote(getCommand()) + "'";
		}

		public static String quote(final String str) {
			return str.replace("'", "'\\''");
		}

		public static String make(final String str, final Object... list) {
			for (int i = 0; i < list.length; i++) {
				if (list[i] == null) {
					continue;
				}
				list[i] = quote((list[i] instanceof String) ? (String)list[i] : list[i].toString());
			}
			return makeUnquoted(str, list);
		}

		public static String makeUnquoted(final String str, final Object... list) {
			return String.format(Locale.US, str, list);
		}

		public static String ignoreErrors(final String cmd) {
			return String.format(Locale.US, "( %1$s ) 2> /dev/null || :", cmd);
		}

		// to be overridden
		public void onStdout(final String data) {
		}

		// to be overridden
		public void onStderr(final String data) {
		}
	}

	public static final class ExitException extends RuntimeException {
		private static final long serialVersionUID = 1l;
		private final int exitCode;

		public ExitException(final int code) {
			exitCode = code;
		}

		public int getExitCode() {
			return exitCode;
		}
	}

	protected static abstract class Generic {
		protected abstract String[] getShell();

		private static final int BUF_SIZE = 10 * 1024;
		private static final String SIG = Character.toString('\001') + Character.toString('\001') + "--";
		private final String TAG = Base.TAG_PREFIX + Utils.getClassName(this.getClass());
		protected final String[] shell;
		protected volatile Process process = null;
		protected volatile DataOutputStream stdin = null;
		protected volatile BufferedReader stdout = null;
		protected volatile BufferedReader stderr = null;

		public Generic() {
			this(null);
		}

		public Generic(final File cwd) {
			shell = getShell();
			open(cwd);
		}

		@Override
		public String toString() {
			final int pid = getPid();
			return String.format(Locale.US, "%1$s(path=%2$s running=%3$s pid=%4$s)",
				(this == null) ? "NULL" : Utils.getClassName(this.getClass()),
				Utils.join(" ", shell),
				Boolean.toString((ping() < 0) ? true : false),
				(pid <= 0) ? "NULL" : String.valueOf(pid)
			);
		}

		protected String getRunner() {
			return Environment.getRootDirectory() + "/bin/sh";
		}

		protected void checkProcess() {
			String err = "";
			try {
				while (stderr.ready()) {
					final String line = stderr.readLine();
					if (line == null) {
						continue;
					}
					err += line;
				}
			}
			catch (final Throwable ignore) { }
			final int code = ping();
			if (ping() >= 0) {
				close();
				throw new RuntimeException((err == null || err.equals("")) ? String.format(Locale.US, "shell died unexpectedly, exit code %1$d", code) : err);
			}
		}

		protected void open() {
			open(null);
		}

		protected void open(final File cwd) {
			if (shell == null || shell.length <= 0) {
				throw new NullPointerException();
			}
			Utils.Log.v(TAG, "opening shell channel [%1$s]", Utils.join(" ", shell));
			try {
				process = Runtime.getRuntime().exec(shell, null, cwd);
			}
			catch (final Throwable ex) {
				throw new RuntimeException("error starting command shell", ex);
			}
			stdin = new DataOutputStream(process.getOutputStream());
			stdout = new BufferedReader(new InputStreamReader(process.getInputStream()), BUF_SIZE);
			stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()), BUF_SIZE);
			for (int i = 0; i < 10; i++) {
				if (ping() < 0) {
					break;
				}
				try {
					Thread.sleep(100);
				}
				catch (final Throwable ignore) { }
			}
//			for (int i = 0; i < 5; i++) {
//				checkProcess();
//				try {
//					Thread.sleep(50);
//				}
//				catch (final Throwable ignore) { }
//			}
		}

		public void close() {
			if (stdin != null) {
				try {
					stdin.flush();
					stdin.close();
				}
				catch (final Throwable ignore) { }
			}
			if (stdout != null) {
				try {
					stdout.close();
				}
				catch (final Throwable ignore) { }
			}
			if (stderr != null) {
				try {
					stderr.close();
				}
				catch (final Throwable ignore) { }
			}
			if (process != null) {
				Utils.Log.v(TAG, "closing shell channel");
				try {
					process.destroy();
					//process.waitFor();
				}
				catch (final Throwable ignore) { }
			}
			stdin = null;
			stdout = null;
			stderr = null;
			process = null;
		}

		public void send(final Command cmd) {
			if (stdin == null) {
				return;
			}
			checkProcess();
			final String data = getRunner() + " -c " + cmd.getQuotedCommand();
			if (LOGGING) {
				Utils.Log.i(TAG, "command: %1$s", data);
			}
			try {
				stdin.writeBytes(data + "\n");
				stdin.writeBytes(String.format(Locale.US, "echo \"%1$s${?}\"\n", SIG));
				stdin.flush();
			}
			catch (final Throwable ignore) { }
			while (ping() < 0) {
				Integer exitCode = null;
				try {
					while (stdout.ready()) {
						final String line = stdout.readLine();
						if (line == null) {
							continue;
						}
						if (line.substring(0, SIG.length()).equals(SIG)) {
							exitCode = Integer.valueOf(line.substring(SIG.length()));
							break;
						} else {
							cmd.onStdout(line);
						}
					}
				}
				catch (final Throwable ignore) { }
				try {
					while (stderr.ready()) {
						final String line = stderr.readLine();
						if (line == null) {
							continue;
						}
						cmd.onStderr(line);
					}
				}
				catch (final Throwable ignore) { }
				if (exitCode != null) {
					if (LOGGING) {
						Utils.Log.i(TAG, "exitCode: %1$d", exitCode);
					}
					if (exitCode != 0) {
						throw new ExitException(exitCode);
					}
					break;
				}
				try {
					Thread.sleep(100);
				}
				catch (final Throwable ignore) { }
			}
		}

		protected int ping() {
			if (process == null) {
				return -1;
			}
			try {
				return process.exitValue();
			}
			catch (final Throwable ignore) { }
			return -1;
		}

		protected int getPid() {
			if (ping() >= 0) {
				return -1;
			}
			Field f = null;
			final String[] list = new String[] {
				"pid",
				"id",
				"handle",
			};
			for (final String s : list) {
				try {
					f = process.getClass().getDeclaredField(s);
					break;
				}
				catch (final Throwable ignore) { }
			}
			if (f != null) {
				try {
					f.setAccessible(true);
					return f.getInt(process);
				}
				catch (final Throwable ignore) { }
			}
			return -1;
		}
	}

	public static class User extends Generic {
		public User() {
			super();
		}

		public User(final File cwd) {
			super(cwd);
		}

		@Override
		protected String[] getShell() {
			return new String[] {
				getRunner(),
			};
		}
	}

	public static class Superuser extends User {
		private static final String[] candidates = new String[] {
			"/sbin/su",
			Environment.getRootDirectory() + "/xbin/su",
			Environment.getRootDirectory() + "/bin/su",
			Environment.getDataDirectory() + "/local/bin/su",
		};

		public Superuser() {
			super();
		}

		public Superuser(final File cwd) {
			super(cwd);
		}

		private String findSu() {
			for (final String cand : candidates) {
				if ((new File(cand)).exists()) {
					return cand;
				}
			}
			return findInPath("su");
		}

		@Override
		protected String[] getShell() {
			final String su = findSu();
			if (su == null) {
				//return super.getShell();
				throw new RuntimeException("unable to find 'su', is device rooted?");
			}
			final List<String> ret = new ArrayList<String>();
			ret.clear();
			ret.addAll(Arrays.asList(new String[] {
				su,
				"-c",
			}));
			ret.addAll(Arrays.asList(super.getShell()));
			return ret.toArray(new String[ret.size()]);
		}
	}

	public static class Runner {
		private static final String TAG = Base.TAG_PREFIX + Utils.getClassName(Runner.class);

		public static interface Task {
			public static enum Group {
				PRE,
				MAIN,
				POST,
			}

			public void setRunner(final Runner runner);
			public void run();
		}

		protected void onTaskInit(final int total) {
		}

		protected void onTaskGroup(final Task.Group group) {
		}

		protected void onTaskProgress() {
		}

		protected void onTaskFinish() {
		}

		protected void onTaskError(final Throwable error) {
			throw new RuntimeException(error);
		}

		public void execute(final Task task) {
			Throwable err = null;
			try {
				task.setRunner(this);
				task.run();
			}
			catch (final Throwable ex) {
				err = ex;
			}
			finally {
				onTaskFinish();
			}
			if (err != null) {
				onTaskError(err);
				Utils.Log.e(TAG, Utils.getStackTrace(err));
			}
		}
	}

	public static class ThreadRunner extends Runner {
		@Override
		public void execute(final Task task) {
			final Thread thread = new Thread() {
				@Override
				public void run() {
					ThreadRunner.super.execute(task);
				}
			};
			thread.setDaemon(false);
			thread.start();
		}
	}

	public static class UiThreadRunner extends ThreadRunner {
		private final Activity mActivity;
		private String mTitle = "";
		protected ProgressDialog mDialog = null;
		protected boolean doUpdate = false;

		public UiThreadRunner(final Activity activity) {
			mActivity = activity;
		}

		public ThreadRunner setTitle(final String title) {
			mTitle = title;
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mDialog != null) {
						mDialog.setTitle(mTitle);
					}
				}
			});
			return this;
		}

		public ThreadRunner setMessage(final String message) {
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mDialog != null) {
						mDialog.setMessage(message);
					}
				}
			});
			return this;
		}

		public ThreadRunner setMessage(final int resId) {
			return setMessage(mActivity.getString(resId));
		}

		@Override
		protected void onTaskInit(final int total) {
			doUpdate = false;
			if (mDialog != null) {
				doUpdate = (total > 0) ? true : false;
				mDialog.setIndeterminate(doUpdate ? false : true);
				if (doUpdate) {
					mDialog.setMax(total);
					mDialog.setProgress(0);
				}
			}
		}

		@Override
		protected void onTaskGroup(final Task.Group group) {
			setMessage(R.string.please_wait);
		}

		@Override
		protected void onTaskProgress() {
			if (mDialog != null && doUpdate) {
				mDialog.incrementProgressBy(1);
			}
		}

		@Override
		protected void onTaskFinish() {
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mDialog != null) {
						mDialog.dismiss();
						mDialog = null;
					}
				}
			});
		}

		@Override
		protected void onTaskError(final Throwable error) {
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					(new AlertDialog.Builder(mActivity))
						.setTitle(R.string.title_error)
						.setMessage(Html.fromHtml(mActivity.getString(R.string.exception, Utils.getExceptionMessage(error))))
						.setPositiveButton(mActivity.getText(R.string.button_ok), null)
						.create()
						.show()
					;
				}
			});
		}

		@Override
		public void execute(final Task task) {
			mDialog = new ProgressDialog(mActivity);
			mDialog.setTitle(mTitle);
			mDialog.setCancelable(false);
			mDialog.setOnCancelListener(null);
			mDialog.setIndeterminate(true);
			mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			setMessage(R.string.dialog_prerun);
			mDialog.show();
			super.execute(task);
		}
	}

	protected static abstract class Task implements ThreadRunner.Task {
		protected abstract Shell.Generic makeShell();

		private class MyCommand extends Shell.Command {
			public MyCommand(final String cmd) {
				super(cmd);
			}
	
			@Override
			public void onStderr(final String data) {
				err.append(data);
			}
		}

		private final StringBuilder err = new StringBuilder();
		protected Runner mRunner = null;
		private Shell.Generic sh = null;

		protected List<String> getPreScript() {
			return null;
		}

		protected List<String> getScript() {
			return null;
		}

		protected List<String> getPostScript() {
			return null;
		}

		private void openShell() {
			if (sh == null && mRunner != null) {
				sh = makeShell();
			}
		}

		private void closeShell() {
			if (sh != null) {
				sh.close();
				sh = null;
			}
		}

		private void runChunk(final List<String> commands) {
			openShell();
			for (final String cmd : commands) {
				mRunner.onTaskProgress();
				err.setLength(0);
				Shell.ExitException exit = null;
				try {
					sh.send(new MyCommand(cmd));
				}
				catch (final Shell.ExitException ex) {
					exit = ex;
				}
				if (err.length() > 0) {
					throw new RuntimeException(err.toString());
				}
				if (exit != null) {
					throw new RuntimeException(String.format(Locale.US, "command failed with exit code %1$d", exit.getExitCode()));
				}
			}
		}

		@Override
		public void setRunner(final Runner runner) {
			mRunner = runner;
		}

		@Override
		public void run() {
			try {
				final List<String> pre = getPreScript();
				final List<String> main = getScript();
				final List<String> post = getPostScript();
				final int count =
					((pre == null) ? 0 : pre.size()) +
					((main == null) ? 0 : main.size()) +
					((post == null) ? 0 : post.size())
				;
				mRunner.onTaskInit(count);
				if (pre != null && pre.size() > 0) {
					mRunner.onTaskGroup(Group.PRE);
					runChunk(pre);
				}
				if (main != null && main.size() > 0) {
					mRunner.onTaskGroup(Group.MAIN);
					runChunk(main);
				}
				if (post != null && post.size() > 0) {
					mRunner.onTaskGroup(Group.POST);
					runChunk(post);
				}
			}
			finally {
				closeShell();
			}
		}
	}

	public static class UserTask extends Task {
		@Override
		protected Shell.Generic makeShell() {
			return new Shell.User(new File("/"));
		}
	}

	public static class SuperTask extends Task {
		@Override
		protected Shell.Generic makeShell() {
			return new Shell.Superuser(new File("/"));
		}
	}

	public static String findInPath(final String prog) {
		final String pathenv = System.getenv("PATH");
		if (pathenv == null) {
			return null;
		}
		for (final String dir : pathenv.split(":")) {
			final String cand = dir + File.separator + prog;
			if ((new File(cand)).exists()) {
				return cand;
			}
		}
		return null;
	}

	public static String getFilesystemDevice(final String mountpoint) {
		if (mountpoint == null || mountpoint.equals("")) {
			return null;
		}
		final File mounts = new File("/proc/self/mounts");
		if (!mounts.canRead()) {
			return null;
		}
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(mounts)), 1024);
			try {
				while (true) {
					final String line = reader.readLine();
					if (line == null) {
						break;
					}
					final String[] fields = line.split("\\s+");
					if (fields.length < 2) {
						continue;
					}
					if (fields[1].equals(mountpoint)) {
						return fields[0];
					}
				}
			}
			catch (final Throwable ignore) { }
			finally {
				if (reader != null) {
					reader.close();
				}
			}
		}
		catch (final Throwable ignore) { }
		return null;
	}

	private static List<File> getEntries(final File dir, final boolean dirs) {
		final List<File> ret = new ArrayList<File>();
		ret.clear();
		if (dir.isDirectory()) {
			for (final File entry : dir.listFiles()) {
				ret.addAll(getEntries(entry, dirs));
			}
			if (dirs) {
				ret.add(dir);
			}
		} else {
			if (!dirs) {
				ret.add(dir);
			}
		}
		return ret;
	}

	public static File[] getDataEntries() {
		final File base = new File(Environment.getDataDirectory() + File.separator + "data" + File.separator + Utils.myPackage());
		final List<File> list = new ArrayList<File>();
		list.clear();
		list.addAll(getEntries(base, false));
		list.addAll(getEntries(base, true));
		return list.toArray(new File[list.size()]);
	}
}
