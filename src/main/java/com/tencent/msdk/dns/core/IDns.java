package com.tencent.msdk.dns.core;

public interface IDns<LookupExtra extends IDns.ILookupExtra> {

    interface ISession {

        interface State {

            int CREATED = 0;
            int CONNECTABLE = 1;
            int WRITABLE = 2;
            int READABLE = 3;
            int ENDED = 4;
        }

        interface NonBlockResult {
            int NON_BLOCK_RESULT_SUCCESS = 0;
            int NON_BLOCK_RESULT_FAILED = 1;
            int NON_BLOCK_RESULT_NEED_CONTINUE = 2;
        }

        interface IToken {

            boolean isConnectable();

            // NOTE: SocketChannel要求调用finishConnect来结束connecting状态
            boolean tryFinishConnect();

            boolean isReadable();

            boolean isWritable();

            boolean isAvailable();
        }

        void connect();

        void request();

        String[] receiveResponse();

        void end();

        // NOTE: 命名为copy避免覆盖Object的clone方法
        ISession copy();

        IDns getDns();

        boolean isEnd();

        IToken getToken();

        IStatistics getStatistics();
    }

    interface ILookupExtra {

        ILookupExtra EMPTY = new ILookupExtra() {
        };
    }

    interface IStatistics {

        boolean lookupSuccess();

        boolean lookupNeedRetry();

        boolean lookupFailed();
    }

    DnsDescription getDescription();

    LookupResult lookup(LookupParameters<LookupExtra> lookupParams);

    /* @Nullable */
    ISession getSession(LookupContext<LookupExtra> lookupContext);

    /**
     * 判断是否获取缓存成功，使用lookupResult.stat.lookupSuccess()来判断
     */
    LookupResult getResultFromCache(LookupParameters<LookupExtra> lookupParams);
}
