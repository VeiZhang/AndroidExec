package com.excellence.exec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2018/8/23
 *     desc   :
 * </pre> 
 */
public class CommandTask {

    private static final String TAG = CommandTask.class.getSimpleName();

    private static final int STATUS_WAITING = 0;
    private static final int STATUS_FINISHED = 1;
    private static final int STATUS_RUNNING = 2;
    private static final int STATUS_INTERRUPT = 3;

    private Command mManager = null;
    private Executor mResponsePoster = null;
    private List<String> mCommand = null;
    private TimeUnit mTimeUnit = null;
    private long mTimeOut = 0;
    private long mTimeDelay = 0;

    private Process mProcess = null;
    private int mStatus = STATUS_WAITING;
    private CommandTaskListener mIListener = null;
    private Disposable mCommandTask = null;
    private Disposable mTimerTask = null;
    private String mCmd = null;

    private CommandTask(Builder builder) {
        mManager = Commander.getCommand();

        mCommand = builder.mCommand;
        mTimeUnit = builder.mTimeUnit;
        mTimeOut = builder.mTimeOut;
        if (mTimeOut <= 0) {
            mTimeOut = mManager.getTimeOut();
        }
        mTimeDelay = builder.mTimeDelay;
        if (mTimeDelay < 0) {
            mTimeDelay = 0;
        }
        mResponsePoster = mManager.getResponsePoster();
        mIListener = new CommandTaskListener();
    }

    public static class Builder {

        private List<String> mCommand = null;
        private TimeUnit mTimeUnit = TimeUnit.MILLISECONDS;
        private long mTimeOut = 0;
        private long mTimeDelay = 0;

        /**
         * 任务命令：字符串列表形式
         *
         * @param command
         * @return
         */
        public Builder command(List<String> command) {
            mCommand = command;
            return this;
        }

        /**
         * 任务命令：字符串、字符串数组形式
         *
         * @param command
         * @return
         */
        public Builder command(String[] command) {
            return command(Arrays.asList(command));
        }

        public Builder command(String command) {
            String[] cmd = command.split(" ");
            return command(cmd);
        }

        /**
         * 延时、超时的时间单位，默认:ms {@link TimeUnit#MILLISECONDS}
         *
         * @param timeUnit
         * @return
         */
        public Builder timeUnit(TimeUnit timeUnit) {
            mTimeUnit = timeUnit;
            return this;
        }

        /**
         * 单独设置任务超时时间，默认:10 * 1000ms {@link Command#DEFAULT_TIME_OUT}
         *
         * @param timeOut
         * @return
         */
        public Builder timeOut(long timeOut) {
            mTimeOut = timeOut;
            return this;
        }

        /**
         * 设置任务延时启动，默认:0ms
         *
         * @param timeDelay
         * @return
         */
        public Builder timeDelay(long timeDelay) {
            mTimeDelay = timeDelay;
            return this;
        }

        public CommandTask build() {
            Commander.checkCommander();
            return new CommandTask(this);
        }
    }

    /**
     * 添加到任务队列中
     *
     * @param listener
     */
    public void deploy(final IListener listener) {
        mIListener.setListener(listener);
        mManager.addTask(this);
    }

    /**
     * 执行命令，由{@link Command#schedule()}控制
     */
    void deploy() {
        try {
            // only wait task can deploy
            if (mStatus != STATUS_WAITING) {
                return;
            }
            mStatus = STATUS_RUNNING;
            mCommandTask = Observable.timer(mTimeDelay, mTimeUnit).subscribeOn(Schedulers.io()).subscribe(new Consumer<Long>() {
                @Override
                public void accept(Long aLong) throws Exception {
                    if (mStatus == STATUS_INTERRUPT) {
                        return;
                    }

                    StringBuilder cmd = new StringBuilder();
                    for (String item : mCommand) {
                        cmd.append(item).append(" ");
                    }
                    mCmd = cmd.toString();
                    mIListener.onPre(mCmd);

                    restartTimer();
                    mProcess = new ProcessBuilder(mCommand).redirectErrorStream(true).start();

                    BufferedReader stdin = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line = null;
                    while ((line = stdin.readLine()) != null) {
                        if (mStatus == STATUS_RUNNING) {
                            restartTimer();
                            mIListener.onProgress(line);
                            result.append(line);
                        }
                    }
                    stdin.close();
                    resetTimer();
                    if (mStatus == STATUS_RUNNING) {
                        mIListener.onSuccess(result.toString());
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    mIListener.onError(throwable);
                }
            });
        } catch (Exception e) {
            mIListener.onError(e);
        }
    }

    private void resetTimer() {
        if (mTimerTask != null && !mTimerTask.isDisposed()) {
            mTimerTask.dispose();
        }
    }

    private void restartTimer() {
        resetTimer();
        mTimerTask = Observable.timer(mTimeOut, mTimeUnit).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                mIListener.onError(new Throwable("Time out : " + mCmd));
                interrupt();
            }
        });
    }

    protected boolean isRunning() {
        return mStatus == STATUS_RUNNING;
    }

    private void killProcess() {
        if (mProcess != null) {
            // close stream
            mProcess.destroy();
        }
    }

    protected void cancel() {
        mStatus = STATUS_INTERRUPT;
        killProcess();
        mManager.removeTask(this);
    }

    private void remove() {
        mManager.remove(this);
    }

    private void interrupt() {
        killProcess();
        remove();
    }

    public void discard() {
        mStatus = STATUS_INTERRUPT;
        killProcess();
        remove();
    }

    private class CommandTaskListener implements IListener {

        private IListener mListener = null;

        private void setListener(IListener listener) {
            mListener = listener;
        }

        @Override
        public void onPre(final String command) {
            mResponsePoster.execute(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onPre(command);
                    }
                }
            });
        }

        @Override
        public void onProgress(final String message) {
            mResponsePoster.execute(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onProgress(message);
                    }
                }
            });
        }

        @Override
        public void onError(final Throwable t) {
            mResponsePoster.execute(new Runnable() {
                @Override
                public void run() {
                    if (mStatus != STATUS_INTERRUPT) {
                        if (mListener != null) {
                            mListener.onError(t);
                        }
                    }
                    mStatus = STATUS_FINISHED;
                    remove();
                }
            });
        }

        @Override
        public void onSuccess(final String message) {
            mResponsePoster.execute(new Runnable() {
                @Override
                public void run() {
                    mStatus = STATUS_FINISHED;
                    if (mListener != null) {
                        mListener.onSuccess(message);
                    }
                    remove();
                }
            });
        }

    }
}
