package com.excellence.exec;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2018/8/16
 *     desc   :
 * </pre> 
 */
class Command {

    private static final String TAG = Command.class.getSimpleName();

    protected static final int DEFAULT_TIME_OUT = 10 * 1000;

    private final LinkedList<CommandTask> mTaskQueue;
    private int mParallelTaskCount = 0;
    private long mTimeOut = 0;
    private Executor mResponsePoster = null;

    protected Command(CommanderOptions options) {
        mTaskQueue = new LinkedList<>();
        mParallelTaskCount = options.mParallelTaskCount;
        if (mParallelTaskCount <= 0) {
            mParallelTaskCount = Integer.MAX_VALUE;
        }
        mTimeOut = options.mTimeOut;
        if (mTimeOut <= 0) {
            mTimeOut = DEFAULT_TIME_OUT;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        mResponsePoster = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                handler.post(command);
            }
        };
    }

    protected long getTimeOut() {
        return mTimeOut;
    }

    protected Executor getResponsePoster() {
        return mResponsePoster;
    }

    /**
     * @see CommandTask.Builder#build()
     *
     * @param command
     * @param listener
     * @return
     */
    @Deprecated
    protected CommandTask addTask(List<String> command, IListener listener) {
        CommandTask task = new CommandTask.Builder().commands(command).build();
        task.deploy(listener);
        return task;
    }

    protected void addTask(CommandTask task) {
        synchronized (mTaskQueue) {
            mTaskQueue.add(task);
        }
        schedule();
    }

    private synchronized void schedule() {
        // count running task
        int runningTaskCount = 0;
        for (CommandTask task : mTaskQueue) {
            if (task.isRunning()) {
                runningTaskCount++;
            }
        }

        if (runningTaskCount >= mParallelTaskCount) {
            return;
        }

        // deploy task to fill parallel task count
        for (CommandTask task : mTaskQueue) {
            task.deploy();
            if (++runningTaskCount == mParallelTaskCount) {
                return;
            }
        }
    }

    protected synchronized void removeTask(CommandTask task) {
        mTaskQueue.remove(task);
    }

    protected synchronized void remove(CommandTask task) {
        removeTask(task);
        schedule();
    }

    /**
     * 关闭所有下载任务
     */
    public synchronized void clearAll() {
        while (!mTaskQueue.isEmpty()) {
            mTaskQueue.get(0).cancel();
        }
    }

}
