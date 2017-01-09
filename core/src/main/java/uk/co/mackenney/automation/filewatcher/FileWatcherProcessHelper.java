package uk.co.mackenney.automation.filewatcher;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import uk.co.mackenney.automation.util.FileUtil;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

/**
 * Created by developer on 07/01/17.
 */
@Component
public class FileWatcherProcessHelper {
    private static Logger log = LoggerFactory.getLogger(FileWatcherProcessHelper.class);

    @Autowired
    private FileWatcherConfiguration config;

    @Autowired
    private FileUtil fileUtil;

    private Path temporaryFolder;

    private long id = 1;

    private synchronized String generateTempFileName() {
        id++;
        return "" + id;
    }

    private FileProcessor locateProcessorForFile(Path inputFile) {
        for (Path directory : config.getInputDirs()) {
            if (inputFile.startsWith(directory)) {
                return config.getFileProcessorForDirectory(directory);
            }
        }
        return null;
    }

    private void setupTemporaryFile(FileProcess process) throws IOException {
        Path tempFile = temporaryFolder.resolve(generateTempFileName() + fileUtil.getExtension(process.getPath()));
        Files.copy(process.getPath(), tempFile);
        process.setTempFilePath(tempFile);
    }



    @PostConstruct
    public void init() throws IOException {
        // Make sure temporary directory exists and is empty
        temporaryFolder = config.getTempDirectory();
        Files.createDirectories(temporaryFolder);
        FileUtils.cleanDirectory(temporaryFolder.toFile());
    }

    @Async("processorExecutor")
    public void executeProcess(FileProcess inputFile) {
        log.trace("--> executeProcess(" + inputFile + ")");
        try {
            FileProcessor processor = locateProcessorForFile(inputFile.getPath());
            if (processor == null) throw new RuntimeException("Unable to locate processor for file " + inputFile);

            setupTemporaryFile(inputFile);

            // Invoke the processor with the temporary file
            processor.processFile(inputFile);

            inputFile.setProcessed();
        }
        catch (Throwable t) {
            inputFile.setError();
            inputFile.log("ERROR: " + t.toString());
            throw new RuntimeException(t);
        }
        finally {
            // Remove the temporary file
            try {Files.delete(inputFile.getTempFilePath());} catch (IOException ioe) {}
        }
        log.trace("<-- executeProcess()");
    }

}
