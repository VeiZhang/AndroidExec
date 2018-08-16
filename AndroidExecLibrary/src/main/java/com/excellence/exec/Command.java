package com.excellence.exec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2018/8/16
 *     desc   :
 * </pre> 
 */
public class Command {

    private static final String TAG = Command.class.getSimpleName();

    protected static final int DEFAULT_TIME_OUT = 10 * 1000;

    private final LinkedList<CommandTask> mTaskQueue;
    private int mParallelTaskCount = 0;
    private int mTimeOut = 0;

    protected Command(int parallelTaskCount, int timeOut) {
        mTaskQueue = new LinkedList<>();
        mParallelTaskCount = parallelTaskCount;
        if (mParallelTaskCount <= 0) {
            mParallelTaskCount = Integer.MAX_VALUE;
        }
        mTimeOut = timeOut;
        if (mTimeOut <= 0) {
            mTimeOut = DEFAULT_TIME_OUT;
        }
    }

    public CommandTask addTask(List<String> command, IListener listener) {
        CommandTask task = new CommandTask(command, listener);
        synchronized (mTaskQueue) {
            mTaskQueue.add(task);
        }
        schedule();
        return task;
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

    private synchronized void remove(CommandTask task) {
        mTaskQueue.remove(task);
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

    public class CommandTask {

        private static final int STATUS_WAITING = 0;
        private static final int STATUS_FINISHED = 1;
        private static final int STATUS_RUNNING = 2;
        private static final int STATUS_INTERRUPT = 3;

        private List<String> mCommand = null;
        private Process mProcess = null;
        private int mStatus = STATUS_WAITING;
        private IListener mIListener = null;

        private CommandTask(List<String> command, final IListener listener) {
            mCommand = command;
            mIListener = new IListener() {
                @Override
                public void onPre(String command) {
                    if (listener != null) {
                        listener.onPre(command);
                    }
                }

                @Override
                public void onProgress(String message) {
                    if (listener != null) {
                        listener.onProgress(message);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    mStatus = STATUS_FINISHED;
                    if (listener != null) {
                        listener.onError(t);
                    }
                    schedule();
                }

                @Override
                public void onSuccess(String message) {
                    mStatus = STATUS_FINISHED;
                    if (listener != null) {
                        listener.onSuccess(message);
                    }
                    remove(CommandTask.this);
                }
            };
        }

        void deploy() {
            try {
                // only wait task can deploy
                if (mStatus != STATUS_WAITING) {
                    return;
                }
                mStatus = STATUS_RUNNING;
                Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                        if (mStatus == STATUS_INTERRUPT) {
                            return;
                        }

                        StringBuilder cmd = new StringBuilder();
                        for (String item : mCommand) {
                            cmd.append(item).append(" ");
                        }
                        mIListener.onPre(cmd.toString());
                        mProcess = new ProcessBuilder(mCommand).redirectErrorStream(true).start();

                        BufferedReader stdin = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                        StringBuilder result = new StringBuilder();
                        String line = null;
                        while ((line = stdin.readLine()) != null && mStatus != STATUS_INTERRUPT) {
                            mIListener.onProgress(line);
                            result.append(line);
                        }
                        stdin.close();

                        if (mStatus != STATUS_INTERRUPT) {
                            mIListener.onSuccess(result.toString());
                        }
                    }
                }).subscribeOn(Schedulers.io()).subscribe();
            } catch (Exception e) {
                mIListener.onError(e);
            }
        }

        private boolean isRunning() {
            return mStatus == STATUS_RUNNING;
        }

        private void cancel() {
            mStatus = STATUS_INTERRUPT;
            if (mProcess != null) {
                mProcess.destroy();
            }
            mTaskQueue.remove(this);
        }

        public void discard() {
            mStatus = STATUS_INTERRUPT;
            if (mProcess != null) {
                mProcess.destroy();
            }
            remove(this);
        }

    }
}
