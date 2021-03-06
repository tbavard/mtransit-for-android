package org.mtransit.android.task;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.AsyncTaskLoader;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTAsyncTaskLoaderV4<D> extends AsyncTaskLoader<D> implements MTLog.Loggable {

	public MTAsyncTaskLoaderV4(Context context) {
		super(context);
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	@Override
	public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "dump(%s,%s,%s,%s)", prefix, fd, writer, args);
		}
		super.dump(prefix, fd, writer, args);
	}

	@Override
	public D loadInBackground() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "loadInBackground()");
		}
		return loadInBackgroundMT();
	}

	/**
	 * @see AsyncTaskLoader#loadInBackground()
	 */
	public abstract D loadInBackgroundMT();

	@Override
	public void onCanceled(D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onCanceled(%s)", data);
		}
		super.onCanceled(data);
	}

	@Override
	protected void onReset() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onReset()");
		}
		super.onReset();
	}

	@Override
	public void setUpdateThrottle(long delayMS) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "setUpdateThrottle(%s)", delayMS);
		}
		super.setUpdateThrottle(delayMS);
	}

	@Override
	public void deliverResult(D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "deliverResult(%s)", data);
		}
		super.deliverResult(data);
	}

	@Override
	protected void onStartLoading() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onStartLoading()");
		}
		super.onStartLoading();
	}

	@Override
	protected void onStopLoading() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onStopLoading()");
		}
		super.onStopLoading();
	}

	// inherited from Loader

	@Override
	public void abandon() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "abandon()");
		}
		super.abandon();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public boolean cancelLoad() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "cancelLoad()");
		}
		return super.cancelLoad();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void commitContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "commitContentChanged()");
		}
		super.commitContentChanged();
	}

	@Override
	public String dataToString(D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "dataToString(%s)", data);
		}
		return super.dataToString(data);
	}

	@Override
	public void forceLoad() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "forceLoad()");
		}
		super.forceLoad();
	}

	@Override
	public boolean isAbandoned() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isAbandoned()");
		}
		return super.isAbandoned();
	}

	@Override
	public boolean isReset() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isReset()");
		}
		return super.isReset();
	}

	@Override
	public boolean isStarted() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isStarted()");
		}
		return super.isStarted();
	}

	@Override
	public void onContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onContentChanged()");
		}
		super.onContentChanged();
	}

	@Override
	public void registerListener(int id, OnLoadCompleteListener<D> listener) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "registerListener(%s,%s)", id, listener);
		}
		super.registerListener(id, listener);
	}

	@Override
	public void reset() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "reset()");
		}
		super.reset();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void rollbackContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "rollbackContentChanged()");
		}
		super.rollbackContentChanged();
	}

	@Override
	public void stopLoading() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "stopLoading()");
		}
		super.stopLoading();
	}

	@Override
	public boolean takeContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "takeContentChanged()");
		}
		return super.takeContentChanged();
	}

	@Override
	public void unregisterListener(OnLoadCompleteListener<D> listener) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "unregisterListener(%s)", listener);
		}
		super.unregisterListener(listener);
	}
}
