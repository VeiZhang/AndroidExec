package com.excellence.exec;

import android.support.annotation.NonNull;
import android.util.Log;

import com.excellence.exec.Command.CommandTask;

import java.util.Arrays;
import java.util.List;

import static com.excellence.exec.Command.DEFAULT_TIME_OUT;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2018/8/16
 *     desc   :
 * </pre> 
 */
public class Commander {

    private static final String TAG = Commander.class.getSimpleName();

    private static Commander mInstance = null;

    private Command mCommand = null;

    /**
     * 默认：不限制并发线程数；指令超时10s终止
     */
    public static void init() {
        init(Integer.MAX_VALUE, DEFAULT_TIME_OUT);
    }

    /**
     * 初始化
     *
     * @param parallelTaskCount 并发线程数
     * @param timeOut 执行命令超时时间（存在某些命令会需要人为输入确认，此时命令会一直卡住等待），默认10s超时，终止指令
     */
    public static void init(int parallelTaskCount, int timeOut) {
        if (mInstance != null) {
            Log.i(TAG, "Commander initialized!!!");
            return;
        }
        mInstance = new Commander();
        mInstance.mCommand = new Command(parallelTaskCount, timeOut);
    }

    private Commander() {

    }

    public static CommandTask addTask(@NonNull List<String> command, IListener listener) {
        checkCommander();
        return mInstance.mCommand.addTask(command, listener);
    }

    public static CommandTask addTask(@NonNull String[] command, IListener listener) {
        checkCommander();
        return addTask(Arrays.asList(command), listener);
    }

    public static CommandTask addTask(@NonNull String command, IListener listener) {
        checkCommander();
        return addTask(new String[]{command}, listener);
    }

    public static CommandTask addUniqueTask() {
        return null;
    }

    private static void checkCommander() {
        if (mInstance == null) {
            throw new RuntimeException("Commander not initialized!!!");
        }
    }
}
