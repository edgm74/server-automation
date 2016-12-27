package uk.co.mackenney.musicautomation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;

public class FileProcessorJob implements Runnable, MusicAutomationConstants {
  private Logger log = Logger.getLogger(FileProcessorJob.class);

  private Path currentFile;
  private boolean processLossless;

  public FileProcessorJob(Path sourceFile) {
    this(sourceFile,true);
  }
  
  public FileProcessorJob(Path sourceFile, boolean processLossless) {
    this.currentFile = sourceFile;
    this.processLossless = processLossless;
  }


  private boolean isAvailable(Path file) {
    log.debug("--> isAvailable( " + file + ")");
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file.toFile());
      log.debug("--- isAvailable() returns true");
      return true;
    }
    catch (IOException ioe) {
      log.debug("--- isAvailable() returns false");
      return false;
    }
    finally {
      if (fis != null) try {
        fis.close();
      }
      catch (IOException ioe) {
      }
      log.debug("<-- isAvailable()");
    }
  }



  private void ensureDirectoryExists(File dir) {
    log.debug("--> ensureDirectoryExists(" + dir + ")");
    if (!dir.isDirectory()) {
      log.debug("--- ensureDirectoryExists(): Directory does not yet exist - check parent");
      ensureDirectoryExists(dir.getParentFile());
      log.debug("--- ensureDirectoryExists(): Creating directory: " + dir);
      dir.mkdir();
    }
    log.debug("<-- ensureDirectoryExists()");
  }

  private void copyArtwork(Tag tag, File dir) throws IOException {
    log.debug("--> copyArtwork(" + tag + "," + dir + ")");
    File f = new File(dir, "folder.jpg");
    if (!f.isFile()) {
      log.debug("--- copyArtwork(): folder.jpg does not yet exist in directory " + dir);
      Artwork artwork = tag.getFirstArtwork();
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(artwork.getBinaryData());
      fos.flush();
      fos.close();
      log.debug("--- copyArtwork(): Finished copying file");
    }
    log.debug("<--copyArtwork()");
  }



  @Override
  public void run() {
    log.debug("--> run()");
    try {      
      log.debug("--- run(): Processing file: " + currentFile);
      long originalStartTime = System.currentTimeMillis();
      String inUseMessage = null;
      while (!isAvailable(currentFile)) {
        // File is still be processed / copied.  Wait for 10 seconds and re-enqueue.
        if (!currentFile.toFile().exists()) {
          inUseMessage = "File " + currentFile + " no longer exists, nothing to do!";
          break;
        }
        else {
          log.debug("--- run(): File in use.  Wait for " + FILE_IN_USE_TIMEOUT + "ms and re-submit");
          Thread.sleep(FILE_IN_USE_TIMEOUT);  
          if (System.currentTimeMillis() > originalStartTime + FILE_IN_USE_TOTAL_WAIT) {
            inUseMessage = "File " + currentFile + " remained locked for more than " + FILE_IN_USE_TOTAL_WAIT + "ms";
            break;
          }
        }
      }        

      if (inUseMessage != null) {
        log.debug("--- run(): " + inUseMessage);
      }
      else if (currentFile.toString().endsWith(MARKER_EXTENSION)) {
        log.debug("--- run(): Ignoring marker file: " + currentFile);
      }
      else if (!currentFile.toString().endsWith("." + MusicAutomationConfiguration.getLosslessFileExtension())) {
        if (processLossless) {
          log.debug("--- run(): File isn't an audio file, remove it from the drop box");
          Files.delete(currentFile);
        }
      }
      else {
        // Grab file information
        log.debug("--- run(): Reading tag from audio file");
        AudioFile f = AudioFileIO.read(currentFile.toFile());
        Tag tag = f.getTag();

        String targetFilename = TagUtils.getTargetFilename(tag);
        String targetPath = TagUtils.getTargetPath(tag);

        Path losslessFilePath = currentFile;
        Path markerFile = losslessFilePath.getParent().resolve(losslessFilePath.getName(losslessFilePath.getNameCount()-1).toString() + MARKER_EXTENSION);
        // Create target folder and copy artwork if required
        if (processLossless) {
          File targetLosslessDirectory = new File(MusicAutomationConfiguration.getTargetDirLossless(), targetPath);
          log.debug("--- run(): Copying audio file to target directory: " + targetLosslessDirectory.getAbsolutePath());
          ensureDirectoryExists(targetLosslessDirectory);
          copyArtwork(tag, targetLosslessDirectory);

          // Copy lossless file to target folder
          losslessFilePath = Paths.get(targetLosslessDirectory.toURI())
              .resolve(targetFilename + "." + MusicAutomationConfiguration.getLosslessFileExtension());
          log.debug("--- run(): Lossless file path is " + losslessFilePath);
          ArrayList<String> pathList = new ArrayList<String>();
          pathList.add(losslessFilePath.toString());
          log.debug("--- run(): Writing marker file: " + markerFile);
          Files.write(markerFile, pathList, Charset.defaultCharset(), java.nio.file.StandardOpenOption.CREATE_NEW );
          Files.move(currentFile, losslessFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          log.debug("--- run(): Copy complete, reopening target file.");
          // Reopen the file for tag manipulation
          f = AudioFileIO.read(losslessFilePath.toFile());
          tag = f.getTag();        

          log.debug("--- run(): Fixing tag");
          // Fix tag details and commit
          boolean requiresCommit = false;

          if (TagUtils.isCompilation(tag)) {
            log.debug("--- run(): Compilation detected - Setting album artist to Various Artists");
            tag.setField(FieldKey.ALBUM_ARTIST, "Various Artists");
            requiresCommit = true;
          }
          
          if (TagUtils.isSoundtrack(tag)) {
            log.debug("--- run(): Soundtrack detected - Setting album artist to Various Artists");
            tag.setField(FieldKey.ALBUM_ARTIST, "Soundtrack");
            requiresCommit = true;
          }          

          if (TagUtils.isMultiDisc(tag)) {
            log.debug("--- run(): Multi disc album detected - removing disc from album title");
            // Fix track numbers to include the disc number
            /*
            if (TagUtils.getTrackNumber(tag) < 100) {
              String toTrackNumber = Integer.toString(TagUtils.getTrackNumber(tag) + TagUtils.getDiscNumber(tag) * 100);
              log.debug("--- run(): Changing track number from " + TagUtils.getTrackNumber(tag) + " to " + toTrackNumber);
              tag.setField(FieldKey.TRACK, toTrackNumber);
            }
            */

            // Remove the disc from the Album title
            tag.setField(FieldKey.ALBUM, TagUtils.getAlbum(tag));
            requiresCommit = true;
          }
          
          if (TagUtils.isSingle(tag)) {
            log.debug("--- run(): Single detected - fixing album name");
            tag.setField(FieldKey.ALBUM, TagUtils.getAlbum(tag));
            requiresCommit = true;
            
          }

          if (requiresCommit) f.commit();
        }
        
        for (int i=0; i<MusicAutomationConfiguration.getConversionConfiguration().length; i++) {
          // Create lossy target directory and copy artwork if required
          File targetDir = new File(MusicAutomationConfiguration.getConversionConfiguration()[i].getTargetDir(), targetPath.toString());
          log.debug("--- run(): Converting audio file to target directory: " + targetDir.getAbsolutePath());
          ensureDirectoryExists(targetDir);
          copyArtwork(tag, targetDir);

          // Convert lossy file to target folder        
          Path targetFilePath = Paths.get(targetDir.toURI()).resolve(targetFilename + "." + MusicAutomationConfiguration.getConversionConfiguration()[i].getFileExtension());
          log.debug("--- run(): File path is " + targetFilePath);
          String command = MusicAutomationConfiguration.getConversionConfiguration()[i].getConversionCommand()
              .replaceAll("SOURCE_FILE",Matcher.quoteReplacement(losslessFilePath.toString()))
              .replaceAll("TARGET_FILE",Matcher.quoteReplacement(targetFilePath.toString()));
          log.debug("--- run(): Conversion command: " + command);
          Process process = Runtime.getRuntime().exec(command);
          process.waitFor();          
        }
        Files.deleteIfExists(markerFile);
        log.debug("--- run(): File conversion complete"); 
      }
      // Check the containing folder for the current file, and if it is empty, delete it.
      File parentDir = currentFile.getParent().toFile();
      while (parentDir != null && parentDir.isDirectory() && !MusicAutomationConfiguration.getSourceDir().equals(parentDir)) {
        log.debug("--- run(): Checking parent folder: " + parentDir);
        if (parentDir.list().length == 0) {
          log.debug("--- run(): Deleting parent folder: " + parentDir);
          parentDir.delete();
          try {Thread.sleep(50); } catch (Exception e) {}
          parentDir = parentDir.getParentFile();
        }
        else {
          log.debug("--- run(): Directory not empty: " + parentDir.list()[0]);
          break;
        }
      }
      log.debug("<-- run()");     
    }
    catch (Exception e) {
      log.error(e);
    }
  }
}

