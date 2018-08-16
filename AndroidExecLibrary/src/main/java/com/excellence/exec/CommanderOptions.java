package com.excellence.exec;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2018/8/16
 *     desc   :
 * </pre> 
 */
public class CommanderOptions {

    protected int mParallelTaskCount = 0;
    protected int mTimeOut = 0;

    private CommanderOptions(Builder builder) {
        mParallelTaskCount = builder.mParallelTaskCount;
        mTimeOut = builder.mTimeOut;
    }

    public static class Builder {

        private int mParallelTaskCount = 0;
        private int mTimeOut = 0;

        /**
         *
         * @param parallelTaskCount 并发线程数
         */
        public Builder setParallelTaskCount(int parallelTaskCount) {
            mParallelTaskCount = parallelTaskCount;
            return this;
        }

        /**
         *
         * @param timeOut 执行命令超时时间（存在某些命令会需要人为输入确认，此时命令会一直卡住等待），默认10s超时，终止指令
         */
        public Builder setTimeOut(int timeOut) {
            mTimeOut = timeOut;
            return this;
        }

        public CommanderOptions build() {
            return new CommanderOptions(this);
        }
    }
}
