package uk.co.mackenney.automation.filewatcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Created by developer on 07/01/17.
 */
@Component
@ConfigurationProperties(prefix="fileWatcher")
public class FileWatcherConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    private Path tempDirectory;
    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = Paths.get(tempDirectory);
    }
    public Path getTempDirectory() {
        return tempDirectory;
    }

    private List<ProcessorClassMapping> processorClassMappings = new ArrayList<>();
    private Map<Path,FileProcessor> processorBeans = new HashMap<>();

    public static class ProcessorClassMapping {
        private String directory;
        private String processor;
        public void setDirectory(String directory) {
            this.directory = directory;
        }
        public void setProcessor(String processor) {
            this.processor = processor;
        }
        public String getDirectory() {
            return directory;
        }
        public String getProcessor() {
            return processor;
        }
    }

    public List<ProcessorClassMapping> getProcessorClassMappings() {
        return processorClassMappings;
    }

    @PostConstruct
    public void init() {
        for (ProcessorClassMapping mapping : processorClassMappings) {
            processorBeans.put(Paths.get(mapping.getDirectory()),(FileProcessor)applicationContext.getBean(mapping.getProcessor()));
        }
    }

    public Collection<Path> getInputDirs() {
        return processorBeans.keySet();
    }

    public FileProcessor getFileProcessorForDirectory(Path directory) {
        return processorBeans.get(directory);
    }

}
