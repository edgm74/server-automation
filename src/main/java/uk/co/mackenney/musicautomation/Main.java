package uk.co.mackenney.musicautomation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.log4j.Logger;

public class Main {
  private static Logger log = Logger.getLogger(Main.class);  
  private static WatchDir process;
  public static void main(String[] args) throws Exception {
    if ("start".equals(args[0])) start(args);
    else if ("stop".equals(args[0])) stop(args);
    else throw new Exception ("Invalid arguments");
  }
  
  private static void recovery() {
    // Find all marker files and move the mentioned files back
    log.debug("--> recovery()");    
    try {
    Files.walkFileTree(Paths.get(MusicAutomationConfiguration.getSourceDir().toURI()), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toString().endsWith(MusicAutomationConstants.MARKER_EXTENSION)) {
          log.debug("--- recovery(): Recovering marker: " + file);
          List<String> path = Files.readAllLines(file, Charset.defaultCharset());
          Path sourceFile = Paths.get(new File(path.get(0)).toURI());
          log.debug("--- recovery(): Marker points to: " + sourceFile);
          Files.move(sourceFile, file.getParent().resolve(sourceFile.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          Files.delete(file);
          log.debug("--- recovery(): Recovery of file " + sourceFile + " complete.");
        }
        return super.visitFile(file, attrs);
      }

    });
    } 
    catch (Exception e) {
      log.error("Recovery failed");
      log.error(e);
    }
    log.debug("<-- recovery()");
  }
  
  private static void start(String[] args) throws Exception {
    recovery();
    process = new WatchDir();
    process.start();    
  }
  
  public static void stop(String[] args) throws Exception {
    if (process != null) {
      process.stop();
      System.exit(0);
    }
  }
  
}
