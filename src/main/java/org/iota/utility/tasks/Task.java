package org.iota.utility.tasks;

import org.iota.utility.NodeSource;

public abstract class Task implements Runnable {

    protected NodeSource source;

    public void load(NodeSource source) {
        this.source = source;
    }

    @Override
    public void run() {

    }
}
