package uk.co.mackenney.musicautomation;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Example to watch a directory (or tree) for changes to files.
 */
public class WatchDir {
  
  private Logger log = Logger.getLogger(WatchDir.class);

  private final WatchService watcher;
  private final Map<WatchKey, Path> keys;
  private final Map<Path, Long> enqueueTime;
  private final ExecutorService threadPool;
  private boolean running = false;  
  
  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  /**
   * Register the given directory with the WatchService
   */
  private void register(Path dir) throws IOException {
    log.debug("--> register(" + dir + ")");
    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
    keys.put(key, dir);
    enqueueTime.put(dir, new Long(System.currentTimeMillis()));
    log.debug("Registered keys: " + keys);
    log.debug("<-- register()");
  }
  
  private void enqueueFile(Path sourceFile) {
    log.debug("--> enqueueFile(" + sourceFile + ")");
    threadPool.execute(new FileProcessorJob(sourceFile));
    log.debug("<-- enqueueFile");
  }

  /**
   * Register the given directory, and all its sub-directories, with the
   * WatchService.
   */
  private void registerAll(final Path start) throws IOException {
    log.debug("--> registerAll(" + start + ")");
    try {Thread.sleep(100);} catch (Exception e){}
    // register directory and sub-directories
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        log.debug("--- registerAll: preVisitDirectory(" + dir + ")");
        register(dir);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        log.debug("--- registerAll: visitFile(" + file + ")");
        enqueueFile(file);
        return super.visitFile(file, attrs);
      }

    });
    log.debug("<-- registerAll()");
  }

  /**
   * Creates a WatchService and registers the given directory
   */
  public WatchDir() throws IOException {
    log.debug("--> <constructor>");
    this.watcher = FileSystems.getDefault().newWatchService();
    this.keys = new HashMap<WatchKey, Path>();
    this.enqueueTime = new HashMap<Path, Long>();
    threadPool = Executors.newFixedThreadPool(MusicAutomationConfiguration.getThreadPoolSize());
    registerAll(Paths.get(MusicAutomationConfiguration.getSourceDir().toURI()));  
    log.debug("<-- <constructor>");
  }
  
  public void start() { 
    log.debug("--> start()");
    processEvents();
    log.debug("<-- start()");
  }

  public void stop() {
    log.debug("--> stop()");
    running = false;
    threadPool.shutdown();
    try {
      // Wait a while for existing tasks to terminate
      if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
        threadPool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
          log.error("--- stop(): Thread Pool did not terminate within the timeout period");
        }
      }
    } 
    catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      threadPool.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }    
    log.debug("<-- stop()");
  }

  /**
   * Process all events for keys queued to the watcher
   */
  void processEvents() {
    log.debug("--> processEvents()");
    running = true;
    while(running) {

      // wait for key to be signalled
      WatchKey key;
      try {
        key = watcher.take();
      }
      catch (InterruptedException x) {
        return;
      }
      
      log.debug("--- processEvents(): WatchKey arrived: " + key);

      Path dir = keys.get(key);
      if (dir == null) {
        log.error("WatchKey not recognized: " + dir);
        continue;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        // Path name = ev.context();
        Path child = dir.resolve(ev.context());
        log.debug("--- processEvents(): Processing " + child + ", event type " + ev.kind());

        // if directory is created, and watching recursively, then
        // register it and its sub-directories
        if (ev.kind() == ENTRY_MODIFY) {
          if (Files.isRegularFile(child, NOFOLLOW_LINKS)) {
            enqueueFile(child);
          }
          else if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
            try {registerAll(child); } catch (IOException ioe) {}
          }
        }
      }

      // reset key and remove from set if directory no longer accessible
      boolean valid = key.reset();
      if (!valid) {
        keys.remove(key);

        // all directories are inaccessible
        if (keys.isEmpty()) {
          break;
        }
      }
      log.debug("--- processEvents(): Finished processing key: " + key);
    }
    log.debug("<-- processEvents()");
  }
}