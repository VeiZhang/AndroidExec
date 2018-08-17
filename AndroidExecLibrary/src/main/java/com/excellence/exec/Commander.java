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
        init(new CommanderOptions.Builder().setParallelTaskCount(Integer.MAX_VALUE).setTimeOut(DEFAULT_TIME_OUT).build());
    }

    /**
     * 初始化
     */
    public static void init(CommanderOptions options) {
        if (mInstance != null) {
            Log.i(TAG, "Commander initialized!!!");
            return;
        }
        mInstance = new Commander();
        mInstance.mCommand = new Command(options);
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

    /**
     * 字符串命令，参数请以空格分隔
     *
     * @param command
     * @param listener
     * @return
     */
    public static CommandTask addTask(@NonNull String command, IListener listener) {
        checkCommander();
        String[] cmd = command.split(" ");
        return addTask(cmd, listener);
    }

    public static CommandTask addUniqueTask(@NonNull String command, IListener listener) {
        checkCommander();
        return null;
    }

    public static void destory() {
        checkCommander();
        mInstance.mCommand.clearAll();
    }

    private static void checkCommander() {
        if (mInstance == null) {
            throw new RuntimeException("Commander not initialized!!!");
        }
    }
}
