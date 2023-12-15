package org.example.workspacewatcher;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class App implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) {
        WorkspaceWatcher ww = new WorkspaceWatcher();

        try {
            ww.start("/home/paulograbin/Hybris/l.k.bennett/hybris/bin/custom");
        } catch (RuntimeException | IOException | InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }
}
