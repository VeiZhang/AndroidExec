# AndroidExec

Android命令执行以及回调

## 使用

```
// 初始化，默认：不限制并发线程数；指令超时10s终止
Commander.init();

// 创建执行命令
Commander.addTask("ls", new IListener() {
    @Override
    public void onPre(String command) {
        Log.i(TAG, "onPre: " + command);
    }

    @Override
    public void onProgress(String message) {
        Log.i(TAG, "onProgress: " + message);
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public void onSuccess(String message) {
        Log.i(TAG, "onSuccess: " + message);
    }
});
```

## Runtime

```
// 执行命令
Process process = Runtime.getRuntime().exec("command");

// 读取正常输出
process.getInputStream()

// 读取错误输出
process.getErrorStream()
```

## ProcessBuilder

```
// 执行命令，重定向输出流
Process process = new ProcessBuilder("command").redirectErrorStream(true).start();

// 不设置重定向，则正常输出、错误输出如同Runtime；
// 设置了重定向后，正常输出、错误输出都统一读取process.getInputStream()
```

