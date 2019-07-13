package org.iota.utility;

import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;

import org.iota.utility.tasks.PushMilestones;
import org.iota.utility.tasks.QueueRequester;
import org.iota.utility.tasks.Quit;
import org.iota.utility.tasks.Task;

public class Main {
    
    enum TaskName {
        MILESTONES(PushMilestones.class), QUEUE(QueueRequester.class), EXIT(Quit.class);

        Class<? extends Task> taskClass;

        private TaskName(Class<? extends Task> taskClass) {
            this.taskClass = taskClass;
        }

        public Task create() {
            try {
                return taskClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                System.out.println(e.getMessage());
                return EXIT.create();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println(Arrays.toString(args));
                System.out.println("Usage: java -jar pushmilestones.jar [target-node] [source-node-1] [source-node-x]");
                System.out.println("Milestones will be queried from source nodes and send to target node");
                return;
            }
            
            String target = args[0];
            if (!ArgValidator.isUrl(target)) {
                System.out.println("target node must be a valid url/ip/address");
                return;
            }
            
            URL[] sources = new URL[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                if (!ArgValidator.isUrl(args[i])) {
                    System.out.println("source node " + args[i] + " is not a valid url/ip/address");
                    return;
                }
                sources[i-1] = new URL(args[i]);
            }
            
            TaskName t = findFunction();
            Task task = t.create();

            if (!(task instanceof Quit)) {
                NodeSource source = new NodeSource(new URL(target), sources);
                task.load(source);
            }

            task.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static TaskName findFunction() {
        TaskName task = null;
        try (Scanner myObj = new Scanner(System.in)) {
            do {
                System.out.println("What are we doing? milestones / queue / exit");

                String taskInput = myObj.nextLine().toUpperCase(); // Read user input

                try {
                    task = TaskName.valueOf(taskInput);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            } while (task == null);
        }

        System.out.println("You chose " + task.toString());
        return task;
    }
}
