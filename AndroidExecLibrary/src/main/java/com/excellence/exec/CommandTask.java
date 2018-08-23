package com.excellence.exec;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
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
    private int mTimeOut = 0;

    private Process mProcess = null;
    private int mStatus = STATUS_WAITING;
    private CommandTaskListener mIListener = null;
    private Disposable mTimer = null;
    private String mCmd = null;

    private CommandTask(List<String> command, int timeOut) {
        mManager = Commander.getCommand();
        mCommand = command;
        mTimeOut = timeOut;
        if (mTimeOut <= 0) {
            mTimeOut = mManager.getTimeOut();
        }
        mResponsePoster = mManager.getResponsePoster();
        mIListener = new CommandTaskListener();
    }

    public static class Builder {

        private List<String> mCommand = null;
        private int mTimeOut = 0;

        /**
         * 任务命令
         *
         * @param command
         * @return
         */
        public Builder command(List<String> command) {
            mCommand = command;
            return this;
        }

        /**
         * 单独设置任务超时时间:ms
         *
         * @param timeOut
         * @return
         */
        public Builder timeOut(int timeOut) {
            mTimeOut = timeOut;
            return this;
        }

        public CommandTask build() {
            return new CommandTask(mCommand, mTimeOut);
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
                Log.i(TAG, "deploy status is not STATUS_WAITING");
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
                    mCmd = cmd.toString();
                    mIListener.onPre(mCmd);

                    restartTimer();
                    mProcess = new ProcessBuilder(mCommand).redirectErrorStream(true).start();

                    BufferedReader stdin = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line = null;
                    while (mStatus == STATUS_RUNNING && (line = stdin.readLine()) != null) {
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
            }).subscribeOn(Schedulers.io()).subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) throws Exception {

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
        if (mTimer != null && !mTimer.isDisposed()) {
            mTimer.dispose();
        }
    }

    private void restartTimer() {
        resetTimer();
        mTimer = Observable.timer(mTimeOut, TimeUnit.MILLISECONDS).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                mIListener.onError(new Throwable("Time out : " + mCmd));
                discard();
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
