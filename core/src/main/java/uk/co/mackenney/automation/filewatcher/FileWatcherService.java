package uk.co.mackenney.automation.filewatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.co.mackenney.automation.util.HostUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Component which monitors directories and yields events.
 */
@Component
public class FileWatcherService {
    private static final String ERROR_FILE_EXTENSION = ".err";

    private boolean running = false;

    @Autowired
    private FileWatcherConfiguration config;

    @Autowired
    private FileWatcherProcessHelper processHelper;

    @Autowired
    private HostUtil hostUtil;

    private static Logger log = LoggerFactory.getLogger(FileWatcherController.class);

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;

    private Set<FileProcess> fileProcessList;


    public Collection<FileProcess> getFileProcessList() {
        return fileProcessList;
    }


    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        log.trace("--> register(" + dir + ")");
        WatchKey key = dir.register(watcher, ENTRY_DELETE, ENTRY_CREATE, ENTRY_MODIFY);
        keys.put(key, dir);
        //enqueueTime.put(dir, System.currentTimeMillis());
        log.debug("Registered keys: " + keys);
        log.trace("<-- register()");
    }

    private void enqueueNewFile(Path sourceFile) {
        log.trace("--> enqueueNewFile(" + sourceFile + ")");
        fileProcessList.add(new FileProcess(sourceFile));
        log.trace("<-- enqueueNewFile");
    }

    private boolean isAvailable(Path file) {
        log.trace("--> isAvailable( " + file + ")");
        HostUtil.HostCallResult lsofResult = hostUtil.runHostCommand(new String[] {"lsof","-t", file.toString()});
        return lsofResult.getStdout() == null || lsofResult.getStdout().length() == 0;
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        log.trace("--> registerAll(" + start + ")");
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                log.trace("--- registerAll: preVisitDirectory(" + dir + ")");
                register(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.trace("--- registerAll: visitFile(" + file + ")");
                enqueueNewFile(file);
                return super.visitFile(file, attrs);
            }

        });
        log.trace("<-- registerAll()");
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    public FileWatcherService() throws IOException {
        log.trace("--> <constructor>");
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.fileProcessList = Collections.synchronizedSet(new HashSet<>());
        log.trace("<-- <constructor>");
    }

    public void start() {
        log.trace("--> start()");
        running = true;
        for(Path inputDir : config.getInputDirs()) {
            log.info("Adding path to monitor: " + inputDir);
            try {
                registerAll(inputDir);
            }
            catch (IOException e) {
                log.error("Unable to register path " + inputDir + " due to error", e);
            }
        }
        log.trace("<-- start()");
    }

    public void stop() {
        log.trace("--> stop()");
        running = false;
        log.trace("<-- stop()");
    }

    /**
     * Process all events for keys queued to the watcher
     */
    @Async
    public void processEvents() {
        log.trace("--> processEvents()");
        while(running) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            }
            catch (InterruptedException x) {
                return;
            }

            log.trace("--- processEvents(): WatchKey arrived: " + key);

            Path dir = keys.get(key);
            if (dir == null) {
                log.error("WatchKey not recognized: " + dir);
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path child = dir.resolve(ev.context());
                log.trace("--- processEvents(): Processing " + child + ", event type " + ev.kind());

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (ev.kind() == ENTRY_CREATE || ev.kind() == ENTRY_MODIFY) {
                    if (Files.isRegularFile(child, NOFOLLOW_LINKS) && !child.toString().endsWith(ERROR_FILE_EXTENSION)) {
                        log.trace("--- processEvents(): New file detected " + child);
                        enqueueNewFile(child);
                    }
                    else if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        log.trace("--- processEvents(): New directory detected " + child);
                        try {
                            registerAll(child);
                        }
                        catch (IOException ioe) {}
                    }
                }
            }
            log.trace("--- processEvents(): Finished processing key: " + key);
        }
        log.trace("<-- processEvents()");
    }


    @Scheduled(fixedDelay=5000)
    public void monitorPendingFiles() throws IOException {
        log.trace("--> monitorPendingFiles()");
        List<FileProcess> processListCopy = new ArrayList<>();
        processListCopy.addAll(fileProcessList);
        int pendingFilesCount = 0;
        int pendingFilesReadyCount = 0;
        for (Iterator<FileProcess> it = processListCopy.iterator(); it.hasNext();) {
            FileProcess process = it.next();
            if (process.isPending()) {
                if (isAvailable(process.getPath())) {
                    process.setReadyToProcess();
                }
            }
        }
        log.debug(MessageFormatter.format("--- monitorPendingFiles(): {} pending files found. {} pending files moved to ready to process state.",pendingFilesCount, pendingFilesReadyCount).getMessage());
        log.trace("<-- monitorPendingFiles()");
    }

    @Scheduled(fixedDelay=5000)
    public void removeInvalidWatchKeys() {
        log.trace("--> removeInvalidWatchKeys()");
        for (Iterator<Map.Entry<WatchKey,Path>> it = keys.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<WatchKey,Path> key = it.next();
            if (!key.getKey().reset()) {
              log.trace("--- removeInvalidWatchKeys(): Removed WatchKey: " + key.getKey().toString() + " for path " + key.getValue().toString());
              it.remove();
            }
        }
        log.trace("<-- removeInvalidWatchKeys()");
    }


    @Scheduled(fixedDelay=5000)
    public void processFiles() {
        log.trace("--> processFiles()");
        int filesToProcessCount = 0;
        List<FileProcess> processListCopy = new ArrayList<>();
        processListCopy.addAll(fileProcessList);
        for (Iterator<FileProcess> it = processListCopy.iterator(); it.hasNext();) {
            FileProcess process = it.next();
            log.trace("--- processFiles(): Checking file " + process);
            if (process.isReadyToProcess()) {
                log.trace("--- processFiles(): File " + process + " is ready to process");
                filesToProcessCount++;
                process.setProcessing();
                processHelper.executeProcess(process);
            }
        }
        log.debug(MessageFormatter.format("--- processFiles(): Found {} files ready to process.",filesToProcessCount).getMessage());
        log.trace("<-- processFiles()");
    }

    private Path locateParentDirectoryForFile(Path inputFile) {
        for (Path directory : config.getInputDirs()) {
            if (inputFile.startsWith(directory)) {
                return directory;
            }
        }
        return null;
    }

    public void removeFileAndParentDirs(Path file) throws IOException {
        if (Files.exists(file)) Files.delete(file);
        Path parentDir = file.getParent();
        Path inputDir = locateParentDirectoryForFile(file);
        if (inputDir != null) {
          while (!parentDir.equals(inputDir)) {
              if (parentDir.toFile().list().length == 0) {
                  Files.delete(parentDir);
                  parentDir = parentDir.getParent();
              }
              else {
                  break;
              }
          }
        }
    }


    @Scheduled(fixedDelay=5000)
    public void cleanupInputDirectories() throws IOException {
        log.trace("--> cleanupInputDirectories()");
        List<FileProcess> processListCopy = new ArrayList<>();
        processListCopy.addAll(fileProcessList);
        for (Iterator<FileProcess> it = processListCopy.iterator(); it.hasNext();) {
            FileProcess process = it.next();
            log.trace("Checking file " + process);
            if (process.isProcessed()) {
                log.trace("File completed - " + process + " - cleaning up.");
                removeFileAndParentDirs(process.getPath());
                fileProcessList.remove(process);
            }
        }
        log.trace("<-- cleanupInputDirectories()");
    }

    @Scheduled(fixedDelay=5000)
    public void reportErrors() throws IOException {
        log.trace("--> reportErrors()");
        List<FileProcess> processListCopy = new ArrayList<>();
        processListCopy.addAll(fileProcessList);
        for (Iterator<FileProcess> it = processListCopy.iterator(); it.hasNext(); ) {
            FileProcess process = it.next();
            log.trace("Checking file " + process);
            if (process.isError()) {
                log.trace("File finished with error - " + process + " - cleaning up.");
                // Write an error file out
                PrintWriter pw = new PrintWriter(process.getPath().getParent().resolve(process.getPath().getFileName().toString() + ERROR_FILE_EXTENSION).toFile());
                pw.println(process.toLogString());
                pw.flush();
                pw.close();
                fileProcessList.remove(process);
            }
        }
        log.trace("<-- cleanupInputDirectories()");
    }

}
