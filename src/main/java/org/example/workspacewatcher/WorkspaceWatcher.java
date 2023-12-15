package org.example.workspacewatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


@SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
public class WorkspaceWatcher {

    private final Logger LOG = LoggerFactory.getLogger(WorkspaceWatcher.class);

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

    private final String rootDirectoryPath;
    private final File rootDirectory;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final CommandRunner runner = new CommandRunner();
    private final WatchService watchService;

    private final ConcurrentHashMap<String, String> EXTENSIONS_TO_BUILD = new ConcurrentHashMap<>(10);
    private final ConcurrentHashMap<String, String> MONITORED_fILES = new ConcurrentHashMap<>(1000);


    public WorkspaceWatcher(String rootDirectoryPath) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();

        this.rootDirectoryPath = rootDirectoryPath;
        this.rootDirectory = Paths.get(rootDirectoryPath).toFile();
    }


    public void start() throws IOException, InterruptedException {
        if (rootDirectory.exists() && rootDirectory.isDirectory()) {
            LOG.info("Ok, let's go");
        } else {
            LOG.error("Doesn't look right, are you sure it exists and is a directory?");
        }

        setupBuilderThread();
        findJavaFiles(rootDirectory);

        watchForChangesOnMonitoredDirectories();
    }

    private void watchForChangesOnMonitoredDirectories() throws InterruptedException {
        while (true) {
            WatchKey take = watchService.take();
            for (WatchEvent<?> pollEvent : take.pollEvents()) {
//                System.out.println("Event kind:" + pollEvent.kind() + ". File affected: " + pollEvent.context() + ".");

                Path changedFile = (Path) pollEvent.context();
                String changedFileName = changedFile.getFileName().toString();
                changedFileName = changedFileName.replaceAll("~", "");

                if (MONITORED_fILES.containsKey(changedFileName)) {
                    String pathToChangedFile = MONITORED_fILES.get(changedFileName);

                    for (String extensionName : EXTENSION_NAMES) {
                        if (pathToChangedFile.contains(extensionName)) {
                            LOG.info("Adding extension to build pipeline: {} at {} ", extensionName, pathToChangedFile);

                            EXTENSIONS_TO_BUILD.put(extensionName, pathToChangedFile);
                            LOG.info("Pipeline size increased to {}", EXTENSIONS_TO_BUILD.size());

                            break;
                        }
                    }
                } else {
                    LOG.warn("Could not find extension for file {}, perhaps it was added afterwards?", changedFileName);
                }
            }

            take.reset();
        }
    }

    private void registerAllRelevantPaths() throws IOException {

    }

    private void callRebuildExtension(String extensionName, String pathToChangedFile) {
        System.out.println("Rebuilding " + extensionName);

        File extensionRootDirectory = new File(pathToChangedFile);

        while (!extensionRootDirectory.getName().equals(extensionName)) {
            extensionRootDirectory = extensionRootDirectory.getParentFile();
        }

        runner.buildExtension(extensionName, extensionRootDirectory.getPath());
    }

    private void setupBuilderThread() {
        Runnable builderThreadTask = () -> {
            while (true) {
                if (!EXTENSIONS_TO_BUILD.isEmpty()) {
                    Iterator<Entry<String, String>> iterator = EXTENSIONS_TO_BUILD.entrySet().iterator();
                    Entry<String, String> entry = iterator.next();

                    String key = entry.getKey();
                    String value = entry.getValue();

                    iterator.remove();

                    callRebuildExtension(key, value);
                } else {
//                    System.out.println("Build pipeline is empty...");
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread thread = new Thread(builderThreadTask, "builderThread");
        LOG.info("Submitting builder thread...");
        thread.start();
//        executorService.submit(thread);
    }

    private void findJavaFiles(File rootDirectory) {
        Runnable findFilesTask = () -> {
            while (true) {
                Map<String, String> fileToPathMap = new HashMap<>(1000);

                try {
                    Files.walkFileTree(rootDirectory.toPath(), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            File visitedFile = file.toFile();

                            LOG.debug("{}", file.getFileName());
                            fileToPathMap.put(visitedFile.getName(), visitedFile.getPath());

                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                LOG.info("Files found {}", fileToPathMap.size());

                Map<String, String> relevantFilesFound = fileToPathMap.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().contains("java"))
                        .filter(entry -> !MONITORED_fILES.containsKey(entry.getKey()))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

                LOG.info("Relevant files found {}", relevantFilesFound.size());

                MONITORED_fILES.putAll(relevantFilesFound);
                LOG.info("Monitoring {} java files", MONITORED_fILES.size());

                Set<Path> pathsWithAtLeastOneJavaFile = makeDirectoryPathsFromFileList(MONITORED_fILES.values());

                for (Path path : pathsWithAtLeastOneJavaFile) {
                    try {
                        path.register(watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_MODIFY);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread javaFileFinderTask = new Thread(findFilesTask, "fileFinderThread");
        LOG.info("Submitting finder thread...");
        javaFileFinderTask.start();
//        executorService.submit(javaFileFinderTask);
    }

    private Set<Path> makeDirectoryPathsFromFileList(Collection<String> values) {
        return values.stream()
                .distinct()
                .map(Paths::get)
                .map(Path::getParent)
                .collect(Collectors.toSet());
    }
}
