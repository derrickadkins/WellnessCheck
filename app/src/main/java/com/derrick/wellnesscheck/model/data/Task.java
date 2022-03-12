package com.derrick.wellnesscheck.model.data;

import java.util.TimerTask;

public class Task extends TimerTask {
    Runnable runnable;
    public Task(Runnable runnable){this.runnable = runnable;}
    @Override
    public void run() {
        runnable.run();
    }
}
