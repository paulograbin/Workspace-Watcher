package org.example.workspacewatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class CommandRunner {

    private final Logger LOG = LoggerFactory.getLogger(CommandRunner.class);


    public void buildExtension(String extensionName, String extensionPath) {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            Map<String, String> environment = processBuilder.environment();
            environment.put("ANT_OPTS", "-Xmx2g -Dfile.encoding=UTF-8 -Dpolyglot.js.nashorn-compat=true -Dpolyglot.engine.WarnInterpreterOnly=false --add-exports java.xml/com.sun.org.apache.xpath.internal=ALL-UNNAMED --add-exports java.xml/com.sun.org.apache.xpath.internal.objects=ALL-UNNAMED");
            environment.put("ANT_HOME", "/home/paulograbin/Hybris/l.k.bennett/hybris/bin/platform/apache-ant");
            environment.put("PLATFORM_HOME", "/home/paulograbin/Hybris/l.k.bennett/hybris/bin/platform");
            environment.put("PATH", "/home/paulograbin/Hybris/l.k.bennett/hybris/bin/platform/apache-ant/bin:/home/paulograbin/.sdkman/candidates/java/17.0.6-tem/bin:/home/paulograbin/.asdf/shims:/usr/local/go/bin:/home/paulograbin/.local/bin/:/home/paulograbin/programs/::/home/paulograbin/.asdf/shims:/home/paulograbin/.asdf/bin:/usr/local/bin:/usr/bin:/bin:/home/paulograbin/bin:/usr/local/sbin:/usr/sbin:/var/lib/snapd/snap/bin");

            File directory = new File(extensionPath);
            LOG.info("Running command in {}", directory.getPath());

            var process = processBuilder
                    .directory(directory)
                    .command("/bin/zsh", "-c", "ant build")
//                    .redirectOutput(ProcessBuilder.Redirect.to(new File("/home/paulograbin/Desktop/out.txt")))
//                    .redirectError(ProcessBuilder.Redirect.to(new File("/home/paulograbin/Desktop/out.txt")))
                    .start();

            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), LOG::info);
            Future<?> future = executorService.submit(streamGobbler);

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                LOG.info("Extension {} successfully compiled", extensionName);
            } else {
                LOG.warn("Failed compilation of {}", extensionName);
            }
        } catch (RuntimeException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .forEach(consumer);
        }
    }
}
