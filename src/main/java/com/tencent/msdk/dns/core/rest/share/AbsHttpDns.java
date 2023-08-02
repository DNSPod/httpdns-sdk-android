package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.base.utils.HttpHelper;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.LookupContext;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.rest.share.rsp.Response;
import com.tencent.msdk.dns.core.rest.share.rsp.ResponseParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public abstract class AbsHttpDns extends AbsRestDns {
    protected final int mFamily;
    protected final DnsDescription mDescription;

    public AbsHttpDns(int family) {
        mFamily = family;
        // NOTE: 区分inet和inet6, 不允许unspecific
//        mFamily = DnsDescription.Family.INET6 == family ? family : DnsDescription.Family.INET;
        mDescription = new DnsDescription(getDescriptionChannel(), mFamily);
    }

    public abstract String getTag();

    public abstract String getDescriptionChannel();

    public abstract String getTargetUrl(String dnsIp, String hostname, LookupExtra lookupExtra);

    public abstract String encrypt(/* @Nullable */String content, /* @Nullable */String key);

    public abstract String decrypt(/* @Nullable */String content, /* @Nullable */String key);

    public abstract SocketAddress getTargetSocketAddress(/* @Nullable */String dnsIp, int family);


    @Override
    public DnsDescription getDescription() {
        return mDescription;
    }

    @Override
    public LookupResult lookup(LookupParameters<LookupExtra> lookupParams) {
        // block way
        if (null == lookupParams) {
            throw new IllegalArgumentException("lookupParams".concat(Const.NULL_POINTER_TIPS));
        }

        String hostname = lookupParams.hostname;
        int timeoutMills = lookupParams.timeoutMills;
        String dnsIp = lookupParams.dnsIp;
        LookupExtra lookupExtra = lookupParams.lookupExtra;
        //  域名解析统计数据类
        Statistics stat = new Statistics();
        stat.retryTimes = lookupParams.curRetryTime;
        stat.asyncLookup = lookupParams.enableAsyncLookup;
        stat.netChangeLookup = lookupParams.netChangeLookup;
        //  域名解析开始时间统计
        stat.startLookup();

        if (tryGetResultFromCache(lookupParams, stat)) {
            stat.endLookup();
            return new LookupResult<>(stat.ips, stat);
        }

        //  缓冲区，用来写SocketChannel
        BufferedReader reader = null;
        try {
            //  noinspection ConstantConditions
            String urlStr = getTargetUrl(dnsIp, hostname, lookupExtra);
            if (TextUtils.isEmpty(urlStr)) {
                stat.errorCode = ErrorCode.ENCRYPT_REQUEST_CONTENT_FAILED_ERROR_CODE;
                return new LookupResult<>(stat.ips, stat);
            }
            //  接受返回string
            String rawRspContent = "";
            String lineTxt;
            try {
                //  发起请求
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setConnectTimeout(timeoutMills);
                connection.setReadTimeout(timeoutMills);
                //  noinspection CharsetObjectCanBeUsed
                //  读取网络请求结果
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                //  结果赋值
                //  rawRspContent = reader.readLine();
                while ((lineTxt = reader.readLine()) != null) {
                    lineTxt += '\n';
                    rawRspContent += lineTxt;
                }
                // 去除最后的"\n"字符避免干扰 ResponseParser 区分批量查询的结果
                rawRspContent = rawRspContent.length() > 0 ? rawRspContent.substring(0, rawRspContent.length() - 2) : "";
                reader.close();
                stat.statusCode = connection.getResponseCode();
            } catch (Exception e) {
                if (!(e instanceof java.net.SocketTimeoutException)) {
                    stat.errorCode = ErrorCode.RESPONSE_FAILED_FOR_EXCEPTION_ERROR_CODE;
                }
                stat.errorMsg = e.getMessage();
                stat.isGetEmptyResponse = true;
                // return new LookupResult<>(stat.ips, stat);
                throw e;
            }

            //  解密
            String rspContent = decrypt(rawRspContent, lookupExtra.bizKey);
            DnsLog.d(getTag() + "lookup byUrl: %s, rsp:[%s]", urlStr, rspContent);
            if (TextUtils.isEmpty(rspContent)) {
                stat.isGetEmptyResponse = true;
                stat.errorCode = ErrorCode.DECRYPT_RESPONSE_CONTENT_FAILED_ERROR_CODE;
            }
            //  返回结果解析器
            Response rsp = ResponseParser.parseResponse(mFamily, rspContent);
            DnsLog.d(getTag() + "lookup response: ====> %s", rsp.toString());
            if (rsp == Response.EMPTY) {
                stat.isGetEmptyResponse = true;
                stat.errorCode = ErrorCode.PARSE_RESPONSE_CONTENT_FAILED_ERROR_CODE;
                return new LookupResult<>(stat.ips, stat);
            }
            stat.clientIp = rsp.clientIp;
            stat.ttl = rsp.ttl;
            stat.ips = rsp.ips;
            if (rsp.ips.length == 0) {
                DnsLog.d(getTag() + "receive success, but no record");
                stat.isGetEmptyResponse = true;
                stat.errorCode = ErrorCode.NO_RECORD;
                return new LookupResult<>(stat.ips, stat);
            }
            //  返回值正常处理
            mCacheHelper.put(lookupParams, rsp);
            stat.errorCode = ErrorCode.SUCCESS;
            stat.expiredTime = System.currentTimeMillis() + rsp.ttl * 1000;
        } catch (Exception e) {
            DnsLog.d(e, getTag() + "lookup failed");
        } finally {
            CommonUtils.closeQuietly(reader);
            stat.endLookup();
        }
        return new LookupResult<>(stat.ips, stat);
    }

    @Override
    public ISession getSession(LookupContext<LookupExtra> lookupContext) {
        // 参数检查由构造方法完成
        return new Session(lookupContext, this, null);
    }

    private class Session extends AbsSession {

        private SocketChannel mChannel = null;

        private SocketAddress mTargetSockAddr = null;

        private ByteBuffer mReadBuffer = null;
        // 收包同时启用了StringBuilder，因为ReadBuffer可能会被清空，以便再次接收
        private StringBuilder mReadStringBuilder = null;

        private ByteBuffer mWriteBuffer = null;

        private final IToken mToken = new Token() {

            @Override
            public boolean tryFinishConnect() {
                if (null != mChannel) {
                    try {
                        if (mChannel.isConnected()) {
                            // 连接成功后可能是等待OP_READ或者OP_WRITE
                            return true;
                        }
                        boolean res = mChannel.finishConnect();
                        if (res) {
                            // 首次连接成功，switch channel to write-read
                            DnsLog.d(getTag() + "tryFinishConnect connect success");
                            mSelectionKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                        }
                        return res;
                    } catch (Exception e) {
                        DnsLog.d(e, getTag() + "tryFinishConnect connect failed");
                        end();
                        // SocketChannel负责创建连接
                        mStat.errorCode = ErrorCode.CONNECT_FAILED_ERROR_CODE;
                        mStat.errorMsg = e.getMessage();
                    }
                }
                return false;
            }

            @Override
            public boolean isReadable() {
                if (null != mChannel) {
                    return mChannel.isConnected() && super.isReadable();
                }
                return super.isReadable();
            }

            @Override
            public boolean isWritable() {
                if (null != mChannel) {
                    DnsLog.d(getTag() + ", channel isConnected:" + mChannel.isConnected() + ", writable:" + super.isWritable());
                    return mChannel.isConnected() && super.isWritable();
                }
                return super.isWritable();
            }

        };

        @Override
        public IToken getToken() {
            return mToken;
        }

        Session(LookupContext<LookupExtra> lookupContext, IDns dns, AbsSession parent) {
            super(lookupContext, dns, parent);

            if (State.READABLE == mState) {
                return;
            }

            Selector selector = mLookupContext.selector();
            if (null == selector) {
                throw new IllegalArgumentException("selector".concat(Const.NULL_POINTER_TIPS));
            }

            try {
                try {
                    mChannel = SocketChannel.open();
                } catch (Exception e) {
                    // SocketChannel负责创建连接
                    mStat.errorCode = ErrorCode.CREATE_SOCKET_FAILED_ERROR_CODE;
                    mStat.errorMsg = e.getMessage();
                    throw e;
                }
                DnsLog.d(getTag() + "%s opened", mChannel);
                try {
                    mChannel.configureBlocking(false);
                } catch (Exception e) {
                    mStat.errorCode = ErrorCode.SET_NON_BLOCK_FAILED_ERROR_CODE;
                    mStat.errorMsg = e.getMessage();
                    throw e;
                }
                try {
//                    Selector注册监听
                    mSelectionKey = mChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    mSelectionKey.attach(mChannel);
                } catch (Exception e) {
                    mStat.errorCode = ErrorCode.REGISTER_CHANNEL_FAILED_ERROR_CODE;
                    mStat.errorMsg = e.getMessage();
                    throw e;
                }
                mState = State.CONNECTABLE;
            } catch (Exception e) {
                DnsLog.d(e, getTag() + "create socket channel failed");
                end();
                return;
            }

            mTargetSockAddr = getTargetSocketAddress(mLookupContext.dnsIp(), mFamily);
            if (null == mTargetSockAddr) {
                DnsLog.d(getTag() + "get target socket address failed");
                mStat.errorCode = ErrorCode.GET_TARGET_SOCKET_ADDRESS_FAILED_ERROR_CODE;
                end();
            }
        }

        @Override
        protected int connectInternal() {
            try {
                DnsLog.d(getTag() + "connect start");
                mChannel.connect(mTargetSockAddr);
            } catch (Exception e) {
                DnsLog.d(e, getTag() + "connect failed");
                end();
                // SocketChannel负责创建连接
                mStat.errorCode = ErrorCode.CONNECT_FAILED_ERROR_CODE;
                mStat.errorMsg = e.getMessage();
                return NonBlockResult.NON_BLOCK_RESULT_FAILED;
            }
            return NonBlockResult.NON_BLOCK_RESULT_SUCCESS;
        }

        @Override
        protected int requestInternal() {
            // non-block mode
            String dnsIp = mLookupContext.dnsIp();
            String hostname = mLookupContext.hostname();
            LookupExtra lookupExtra = mLookupContext.lookupExtra();
            String urlStr = getTargetUrl(dnsIp, hostname, lookupExtra);
            if (TextUtils.isEmpty(urlStr)) {
                mStat.errorCode = ErrorCode.ENCRYPT_REQUEST_CONTENT_FAILED_ERROR_CODE;
                end();
                return NonBlockResult.NON_BLOCK_RESULT_FAILED;
            }
            String getReq = HttpHelper.getRequest(urlStr);
            if (TextUtils.isEmpty(getReq)) {
                mStat.errorCode = ErrorCode.CREATE_REQUEST_PACKET_CONTENT_FAILED_ERROR_CODE;
                end();
                return NonBlockResult.NON_BLOCK_RESULT_FAILED;
            }
            try {
                DnsLog.v(getTag() + "send httpReq:{\n%s}", getReq);
                DnsLog.d(getTag() + "lookup send byUrl: %s", urlStr);
                if (mWriteBuffer == null) {
                    //noinspection CharsetObjectCanBeUsed
                    mWriteBuffer = ByteBuffer.wrap(getReq.getBytes("UTF-8"));
                }
                ByteBuffer buf = mWriteBuffer;
                int i = 0;
                while (buf.hasRemaining()) {
                    int written = mChannel.write(buf);
                    DnsLog.d(getTag() + "send request count:" + (++i) + ", res:" + written);
                    if (written <= 0) {
                        break;
                    }
                }
                if (buf.hasRemaining()) {
                    // 发包允许重试，记录buffer
                    DnsLog.d(getTag() + "send request has remaining, try again");
                    // need continue
                    return NonBlockResult.NON_BLOCK_RESULT_NEED_CONTINUE;
                } else {
                    mWriteBuffer = null;
                }
                // switch channel to read-only
                mSelectionKey.interestOps(SelectionKey.OP_READ);
                DnsLog.d(getTag() + "send request finish");
            } catch (Exception e) {
                DnsLog.d(e, getTag() + "send request failed, for exception");
                end();
                mStat.errorCode = ErrorCode.REQUEST_FAILED_ERROR_CODE;
                mStat.errorMsg = e.getMessage();
                return NonBlockResult.NON_BLOCK_RESULT_FAILED;
            }
            return NonBlockResult.NON_BLOCK_RESULT_SUCCESS;
        }

        @Override
        protected Response responseInternal() {
            DnsLog.d(getTag() + "receive responseInternal call");
            LookupExtra lookupExtra = mLookupContext.lookupExtra();
            if (mReadBuffer == null) {
                mReadBuffer = ByteBuffer.allocate(TCP_CONTINUOUS_RCV_BUF_SIZE);
            }
            if (mReadStringBuilder == null) {
                mReadStringBuilder = new StringBuilder();
            }
            ByteBuffer rspBuf = mReadBuffer;
            int rspLen;
            int totalLen = 0;
            StringBuilder sb = mReadStringBuilder;
            do {
                try {
                    rspLen = mChannel.read(rspBuf);
                    DnsLog.d(getTag() + "receive response get len:%d, lastLen:%d", rspLen, totalLen);
                    if (rspLen > 0) {
                        totalLen += rspLen;
                        // 此时就处理数据
                        rspBuf.flip();
                        rspLen = rspBuf.limit();
                        byte[] rawRsp = new byte[rspLen];
                        rspBuf.get(rawRsp, 0, rspLen);
                        String rspHttpRsp = new String(rawRsp, Charset.forName("UTF-8"));
                        sb.append(rspHttpRsp);
                        if (HttpHelper.checkHttpRspFinished(rspHttpRsp)) {
                            DnsLog.d(getTag() + "receive response check http rsp finished:%d, so break", rspLen);
                            break;
                        }
                        // 重新清空rspBuf，并再次接收
                        rspBuf.clear();
                    } else if (rspLen == 0) {
                        // break, 等待下次select
                        DnsLog.d(getTag() + "receive response get len:0, and break");
                        break;
                    }
                } catch (Exception e) {
                    DnsLog.d(e, getTag() + "receive response failed, for exception");
                    mStat.isGetEmptyResponse = true;
                    mStat.errorCode = ErrorCode.RESPONSE_FAILED_FOR_EXCEPTION_ERROR_CODE;
                    mStat.errorMsg = e.getMessage();
                    return Response.EMPTY;
                }
            } while (rspLen >= 0);
            DnsLog.d(getTag() + "receive response get total len:%d", totalLen);

            if (rspLen == 0) {
                DnsLog.d(getTag() + "receive response failed, need continue, for total len:%d", totalLen);
                mStat.errorCode = ErrorCode.RESPONSE_FAILED_NEED_CONTINUE_ERROR_CODE;
                return Response.NEED_CONTINUE;
            }

            if (totalLen <= 0) {
                DnsLog.d(getTag() + "receive response failed, for total len:%d", totalLen);
                mStat.isGetEmptyResponse = true;
                mStat.errorCode = ErrorCode.RESPONSE_FAILED_FOR_EXCEPTION_ERROR_CODE;
                return Response.EMPTY;
            }
            String rspHttpRsp = sb.toString();
            DnsLog.v(getTag() + "receive rspHttpRsp:{\n%s}", rspHttpRsp);
            String rspBody = HttpHelper.responseBody(rspHttpRsp);
            String rspContent = decrypt(rspBody, lookupExtra.bizKey);
            mStat.statusCode = HttpHelper.responseStatus(rspHttpRsp);
            DnsLog.d(getTag() + "receive rawLen:%d, raw:[%s], rsp body content:[%s]", totalLen, rspBody, rspContent);
            if (TextUtils.isEmpty(rspContent)) {
                mStat.isGetEmptyResponse = true;
                mStat.errorCode = ErrorCode.DECRYPT_RESPONSE_CONTENT_FAILED_ERROR_CODE;
                return Response.EMPTY;
            }
            Response resParser = ResponseParser.parseResponse(mFamily, rspContent);
            //  将hdns有返回但域名自身解析记录配置为空的情况独立出来
            if (resParser.ips.length == 0) {
                DnsLog.d(getTag() + "receive success, but no record");
                mStat.isGetEmptyResponse = true;
                mStat.errorCode = ErrorCode.NO_RECORD;
            } else {
                mStat.errorCode = ErrorCode.SUCCESS;
            }
            return resParser;

        }

        @Override
        protected void endInternal() {
            // NOTE: 如果短时间内不会存在串行请求, 池化连接意义不大
            CommonUtils.closeQuietly(mChannel);
            mWriteBuffer = null;
            mReadBuffer = null;
            mReadStringBuilder = null;
        }

        @Override
        protected AbsSession copyInternal() {
            return new Session(mLookupContext, mDns, this);
        }
    }
}
