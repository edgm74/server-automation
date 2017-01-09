package uk.co.mackenney.automation.filewatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;

import java.nio.file.Path;

@RestController
public class FileWatcherController {
    private static Logger log = LoggerFactory.getLogger(FileWatcherController.class);

    @Autowired
    private FileWatcherConfiguration config;

    @Autowired
    private FileWatcherService fileWatcherService;

    @PostConstruct
    public void init() throws RuntimeException {
        try {
            fileWatcherService.start();
            fileWatcherService.processEvents();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @RequestMapping("/filewatcher/status")
    public Collection<FileProcess> getStatus() {
        return fileWatcherService.getFileProcessList();
    }

    private Map<String,String> convertPathMapToStringMap(Map<Path,Long> input) {
        HashMap<String,String> result = new HashMap<>();
        for (Path p : input.keySet()) {
            Date d = new Date(input.get(p));
            SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss");
            result.put(p.toString(),df.format(d));
        }
        return result;
    }

    @RequestMapping("/filewatcher/configuration")
    public Map<String,Object> getConfiguration() {
        Map<String,Object> result = new HashMap<>();
        result.put("tempDir",config.getTempDirectory());
        result.put("processorClassMappings",config.getProcessorClassMappings());
        return result;
    }

}
