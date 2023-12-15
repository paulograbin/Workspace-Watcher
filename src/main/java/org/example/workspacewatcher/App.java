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
        try {
            WorkspaceWatcher ww = new WorkspaceWatcher("/home/paulograbin/Hybris/l.k.bennett/hybris/bin/custom");
            ww.start();
        } catch (RuntimeException | IOException | InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }
}
