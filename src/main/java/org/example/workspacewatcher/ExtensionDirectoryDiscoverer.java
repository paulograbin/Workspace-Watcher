package org.example.workspacewatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;


public class ExtensionDirectoryDiscoverer {

    private final Logger LOG = LoggerFactory.getLogger(ExtensionDirectoryDiscoverer.class);


    public Set<String> searchForHybrisExtensions(File platformDirectory) {
        Set<String> discoveredExtensions = new HashSet<>(20);

        File customExtensionsDirectory = new File(platformDirectory.getParent() + "/custom");
        if (!customExtensionsDirectory.exists() || !customExtensionsDirectory.isDirectory()) {
            LOG.error("Could not determine your custom directory");
            System.exit(1);
        }

        LOG.info("Searching for all extensions under {}", customExtensionsDirectory.getPath());
        try {
            Files.walkFileTree(customExtensionsDirectory.toPath(), new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    File directory = dir.toFile();

                    if (isExtensionDiretory(directory)) {
                        LOG.debug("Found one extension {}", directory.getName());

                        discoveredExtensions.add(directory.getName());
                    }

                    return super.preVisitDirectory(dir, attrs);
                }


                private boolean isExtensionDirectory(File directory) {
                    return Arrays.stream(requireNonNull(directory.listFiles()))
                            .anyMatch(p -> p.getName().equalsIgnoreCase("build.xml"));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return discoveredExtensions;
    }
}
