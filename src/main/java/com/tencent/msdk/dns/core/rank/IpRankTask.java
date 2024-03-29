package com.tencent.msdk.dns.core.rank;

import android.os.SystemClock;
import android.util.Pair;

import com.tencent.msdk.dns.core.Const;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class IpRankTask implements Runnable {

    private int timeouts = Const.MAX_CONNECT_TIME;
    private String hostname;
    private String[] ips;
    private IpRankItem ipRankItem;
    private IpRankCallback ipRankCallback;

    public IpRankTask(String hostname, String[] ips, IpRankItem ipRankItem, IpRankCallback ipRankCallback) {
        this.hostname = hostname;
        this.ips = ips;
        this.ipRankItem = ipRankItem;
        this.ipRankCallback = ipRankCallback;
    }

    @Override
    public void run() {
        int[] speeds = new int[ips.length];
        for (int i = 0; i < ips.length; i++) {
            speeds[i] = ipSpeedTask(ips[i], ipRankItem.getPort());
        }
        String[] results = ipsSortedBySpeeds(ips, speeds);
        if (ipRankCallback != null) {
            ipRankCallback.onResult(hostname, results);
        }
    }

    /**
     * ips测速排名
     *
     * @param ips
     * @param speeds
     * @return
     */
    private String[] ipsSortedBySpeeds(String[] ips, int[] speeds) {
        ArrayList<Pair<String, Integer>> ipsSpeedsList = new ArrayList<>();
        for (int i = 0; i < ips.length; i++) {
            ipsSpeedsList.add(new Pair<>(ips[i], speeds[i]));
        }
        Collections.sort(ipsSpeedsList, new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                return o1.second - o2.second;
            }
        });
        String[] sortedIps = new String[ipsSpeedsList.size()];
        for (int j = 0; j < ipsSpeedsList.size(); j++) {
            sortedIps[j] = ipsSpeedsList.get(j).first;
        }
        return sortedIps;
    }

    /**
     * ip socket连接测速任务
     *
     * @param ip
     * @param port
     * @return
     */
    private int ipSpeedTask(String ip, int port) {
        Socket socket = new Socket();
        long start = SystemClock.elapsedRealtime();
        long end = start + timeouts;
        SocketAddress remoteAddress = new InetSocketAddress(ip, port);
        try {
            socket.connect(remoteAddress, timeouts);
            end = SystemClock.elapsedRealtime();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (int) (end - start);
    }
}
