package org.iota.utility.tasks;

public class Quit extends Task {

    public Quit() {

    }

    @Override
    public void run() {
        System.out.println("Bye!");
        System.exit(0);
    }

}
