package com.excellence.exec;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2018/8/16
 *     desc   :
 * </pre> 
 */
public interface IListener {

    void onPre(String command);

    void onProgress(String message);

    void onError(Throwable t);

    void onSuccess(String message);
}
