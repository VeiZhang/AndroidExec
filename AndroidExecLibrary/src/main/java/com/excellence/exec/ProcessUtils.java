package com.excellence.exec;

import android.support.annotation.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2018/9/3
 *     desc   : {@link Process} 进程工具
 * </pre> 
 */
public class ProcessUtils {

    public static final String REG_NUMBER = "[^0-9]";

    public static void killProcess(@NonNull Process process) {
        int pid = getProcessId(process);
        if (pid <= 0) {
            return;
        }
        android.os.Process.killProcess(pid);
        process.destroy();
    }

    public static int getProcessId(@NonNull Process process) {
        try {
            Pattern pattern = Pattern.compile(REG_NUMBER);
            Matcher matcher = pattern.matcher(process.toString());
            String pid = matcher.replaceAll("").trim();
            return Integer.parseInt(pid);
        } catch (Exception e) {

        }
        return 0;
    }

    public static void closeStream(@NonNull Process process) {
        try {
            InputStream in = process.getInputStream();
            if (in != null) {
                in.close();
            }
        } catch (Exception e) {

        }

        try {
            InputStream in = process.getErrorStream();
            if (in != null) {
                in.close();
            }
        } catch (Exception e) {

        }

        try {
            OutputStream out = process.getOutputStream();
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {

        }
    }

    public static void processDestroy(@NonNull Process process) {
        try {
            if (process.exitValue() != 0) {
                closeStream(process);
                killProcess(process);
            }
        } catch (Exception e) {
            closeStream(process);
            killProcess(process);
        }
    }
}
