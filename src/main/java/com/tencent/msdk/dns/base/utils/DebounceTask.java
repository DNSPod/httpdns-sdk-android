package com.tencent.msdk.dns.base.utils;

import java.util.Timer;
import java.util.TimerTask;

public class DebounceTask {
    private Timer timer;
    private Long delay;
    private Runnable runnable;

    public DebounceTask(Runnable runnable,  Long delay) {
        this.runnable = runnable;
        this.delay = delay;
    }

    public static DebounceTask build(Runnable runnable, Long delay){
        return new DebounceTask(runnable, delay);
    }

    public void run(){
        if(timer!=null){
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timer=null;
                runnable.run();
            }
        }, delay);
    }
}
