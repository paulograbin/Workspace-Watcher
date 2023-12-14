package org.example.workspacewatcher;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkspaceWatcherApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceWatcherApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        App app = new App();
        app.monitorDirectoryTree("/home/paulograbin/Hybris/l.k.bennett/hybris/bin/custom");

//        CommandRunner runner = new CommandRunner();
//        runner.buildExtension("islandpacific", "/home/paulograbin/Hybris/l.k.bennett/hybris/bin/custom/lkbennett/islandpacific");

    }
}
