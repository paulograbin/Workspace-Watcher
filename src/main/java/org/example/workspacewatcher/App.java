package org.example.workspacewatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class App implements CommandLineRunner {

    private final Logger LOG = LoggerFactory.getLogger(CommandLineRunner.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) {
        String path = "";

        if (args.length == 0) {
            path = System.getProperty("user.dir");
        } else {
            path = args[0];
        }

        var commandRunner = new CommandRunner();
        var extensionDirectoryDiscoverer = new ExtensionDirectoryDiscoverer();

        try {
            WorkspaceWatcher ww = new WorkspaceWatcher(path, extensionDirectoryDiscoverer, commandRunner);
            ww.start();
        } catch (RuntimeException | IOException | InterruptedException e) {
            LOG.error("Something went wrong {}", e.getMessage());
        }
    }
}
