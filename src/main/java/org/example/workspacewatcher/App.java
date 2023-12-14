package org.example.workspacewatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@SuppressWarnings("InfiniteLoopStatement")
public class App {

    private final CommandRunner runner = new CommandRunner();
    private final Set<String> EXTENSION_NAMES = Set.of("lkbennettfulfilment",
            "lkbennettwebserviceclient",
            "lkbennettcore",
            "lkbennettfacades",
            "lkbennetttest",
            "lkbennetttax",
            "lkbennettreleasedata",
            "lkbennettamplience",
            "lkbennettamplienceplugin",
            "lkbennettcommercewebservices",
            "lkbennettintegrationgfs",
            "lkbennetttrackingdressipi",
            "islandpacific",
            "globalecore",
            "globalefacades",
            "globaleendpoint",
            "globalepromotions",
            "globaleaddon",
            "globalebackoffice",
            "lkbennettglobale",
            "lkbennettadyenextensions");


    private final ConcurrentHashMap<String, String> extensionToBuild = new ConcurrentHashMap<>(10);


    private Map<String, String> findJavaFiles(File rootDirectory) throws IOException {
        Map<String, String> fileToPathMap = new HashMap<>(1000);

        Files.walkFileTree(rootDirectory.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                File visitedFile = file.toFile();

                if (visitedFile.getName().endsWith(".java")) {
                    fileToPathMap.put(visitedFile.getName(), visitedFile.getParent());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return fileToPathMap.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().contains("jalo"))
                .filter(entry -> !entry.getValue().contains("testsrc"))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public void monitorDirectoryTree(String rootDirectoryPath) throws IOException, InterruptedException {
        Runnable compilerThread = () -> {
            while (true) {
                if (!extensionToBuild.isEmpty()) {
                    Iterator<Entry<String, String>> iterator = extensionToBuild.entrySet().iterator();
                    Entry<String, String> entry = iterator.next();

                    String key = entry.getKey();
                    String value = entry.getValue();

                    iterator.remove();

                    callRebuildExtension(key, value);
                } else {
                    System.out.println("Build pipeline is empty...");
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread thread = new Thread(compilerThread);
        thread.start();

        File rootDirectory = Paths.get(rootDirectoryPath).toFile();

        if (rootDirectory.exists() && rootDirectory.isDirectory()) {
            System.out.println("Ok, let's go");
        } else {
            System.err.println("Nah");
        }


        Map<String, String> fileToPathMap = findJavaFiles(rootDirectory);
        Set<Path> pathsWithAtLeastOneJavaFile = makeDirectoryPathsFromFileList(fileToPathMap.values());

        System.out.println("Found " + fileToPathMap.size() + " files...");
        System.out.println("Found " + pathsWithAtLeastOneJavaFile.size() + " directories to watch...");

        WatchService watchService = FileSystems.getDefault().newWatchService();

        for (Path path : pathsWithAtLeastOneJavaFile) {
            path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_DELETE);
        }

        while (true) {
            WatchKey take = watchService.take();
            for (WatchEvent<?> pollEvent : take.pollEvents()) {
                System.out.println("Event kind:" + pollEvent.kind() + ". File affected: " + pollEvent.context() + ".");

                Path changedFile = (Path) pollEvent.context();
                String changedFileName = changedFile.getFileName().toString();
                changedFileName = changedFileName.substring(0, changedFileName.length() - 1);

                if (fileToPathMap.containsKey(changedFileName)) {
                    String pathToChangedFile = fileToPathMap.get(changedFileName);

                    for (String extensionName : EXTENSION_NAMES) {
                        if (pathToChangedFile.contains(extensionName)) {
                            System.out.println("Found extension to recompile: " + extensionName + " on " + pathToChangedFile);

                            extensionToBuild.put(extensionName, pathToChangedFile);

                            break;
                        }
                    }
                }
            }

            take.reset();
        }
    }

    private Set<Path> makeDirectoryPathsFromFileList(Collection<String> values) {
        return values.stream()
                .distinct()
                .map(Paths::get)
                .collect(Collectors.toSet());
    }

    private void callRebuildExtension(String extensionName, String pathToChangedFile) {
        System.out.println("Rebuilding " + extensionName);

        File extensionRootDirectory = new File(pathToChangedFile);

        while (!extensionRootDirectory.getName().equals(extensionName)) {
            extensionRootDirectory = extensionRootDirectory.getParentFile();
        }

        runner.buildExtension(extensionName, extensionRootDirectory.getPath());
    }
}
