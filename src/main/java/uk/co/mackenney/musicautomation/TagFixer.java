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
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

public class TagFixer {
  private static Logger log = Logger.getLogger(Main.class);  

  private static void fixFile(Path file, String extension) throws Exception {
      AudioFile f = AudioFileIO.read(file.toFile());
      Tag tag = f.getTag();
      
      // Determine if this is a multi disc set and has the old style track numbers
      if (TagUtils.isMultiDisc(tag) && TagUtils.getTrackNumber(tag) > 99) {
    	  int trackNumber = TagUtils.getTrackNumber(tag) % 100;
    	  log.debug("Changing track number from : " + TagUtils.getTrackNumber(tag)  + " to " +  trackNumber);
    	  tag.setField(FieldKey.TRACK, "" + trackNumber);
    	  f.commit();
    	  log.debug("Renaming file from " + file.toString() + " to " + file.getParent().resolve(TagUtils.getTargetFilename(tag)));
    	  Files.move(file, file.getParent().resolve(TagUtils.getTargetFilename(tag) + "." + extension), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
  }
	
	
  private static void processDirectory(File directory, final String extension) throws Exception {
	  
	  Files.walkFileTree(Paths.get(directory.toURI()), new SimpleFileVisitor<Path>() {
	      @Override
	      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	    	  
	        if (file.toString().endsWith(extension)) {
	        	try {
  	        	log.debug("Fixing file:" + file.toString());
	        	fixFile(file, extension);
	        	} 
	        	catch (Throwable t) {
	        		log.error("Error on file: " + file.toString());
	        		log.error(t);
	        	}
	        }
	        return super.visitFile(file, attrs);
	      }

	    });
  } 		  
  
  public static void main(String[] args) throws Exception {
	  processDirectory(MusicAutomationConfiguration.getTargetDirLossless(), MusicAutomationConfiguration.getLosslessFileExtension());
	  for (ConversionConfiguration convConfig : MusicAutomationConfiguration.getConversionConfiguration()) {
		  processDirectory(convConfig.getTargetDir(),convConfig.getFileExtension());
	  }  	  
  }
}
