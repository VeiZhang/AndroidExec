package com.excellence.exec;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

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

    private static final int DEFAULT_TIME_OUT = 10 * 1000;

    private static Commander mInstance = null;

    private Command mCommand = null;

    /**
     * 默认：不限制并发线程数；指令超时{@link Commander#DEFAULT_TIME_OUT}终止
     */
    public static void init() {
        init(new CommanderOptions.Builder().parallelTaskCount(Integer.MAX_VALUE).timeOut(DEFAULT_TIME_OUT).build());
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

    protected static Command getCommand() {
        return mInstance.mCommand;
    }

    private Commander() {

    }

    /**
     * @see CommandTask.Builder#build()
     *
     * @param command
     * @param listener
     * @return
     */
    @Deprecated
    public static CommandTask addTask(@NonNull List<String> command, IListener listener) {
        checkCommander();
        return mInstance.mCommand.addTask(command, listener);
    }

    /**
     * @see CommandTask.Builder#build()
     *
     * @param command
     * @param listener
     * @return
     */
    @Deprecated
    public static CommandTask addTask(@NonNull String[] command, IListener listener) {
        checkCommander();
        return addTask(Arrays.asList(command), listener);
    }

    /**
     * @see CommandTask.Builder#build()
     * 字符串命令，参数请以空格分隔
     *
     * @param command
     * @param listener
     * @return
     */
    @Deprecated
    public static CommandTask addTask(@NonNull String command, IListener listener) {
        checkCommander();
        String[] cmd = command.split(" ");
        return addTask(cmd, listener);
    }

    public static void destroy() {
        checkCommander();
        mInstance.mCommand.clearAll();
    }

    protected static void checkCommander() {
        if (mInstance == null) {
            throw new RuntimeException("Commander not initialized!!!");
        }
    }
}
