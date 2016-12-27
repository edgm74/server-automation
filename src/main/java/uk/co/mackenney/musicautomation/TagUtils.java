package uk.co.mackenney.musicautomation;

import java.io.File;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;

public class TagUtils implements MusicAutomationConstants {
  private static Logger log = Logger.getLogger(TagUtils.class);  	

    
  public static void dumpTags(Tag tag) {
	for (Iterator<TagField> fields = tag.getFields(); fields.hasNext(); ) {
	  TagField field = fields.next();
  	  log.debug(field.getId() + "=" + field.toString());
	}
  }
  
  public static boolean isCompilation(Tag tag) {
	  return "1".equals(tag.getFirst(COMPILATION)) || "1".equals(tag.getFirst(CPIL));
  }
  
  public static boolean isSingle(Tag tag) {
    return tag.getFirst(FieldKey.ALBUM).contains(SINGLE_TAG);
  }
  
  public static boolean isMultiDisc(Tag tag) {
	  return getTotalDiscs(tag) > 1;
  }
  
  public static int getTrackNumber(Tag tag) {
	  return Integer.parseInt(tag.getFirst(FieldKey.TRACK));
  }
  
  public static int getDiscNumber(Tag tag) {
	  return tag.getFirst(FieldKey.DISC_NO) != null ? Integer.parseInt(tag.getFirst(FieldKey.DISC_NO)) : getPartOfSet(tag)[DISC_NUMBER];
  }
  
  public static int getTotalDiscs(Tag tag) {
    return tag.getFirst(FieldKey.DISC_TOTAL) != null ? Integer.parseInt(tag.getFirst(FieldKey.DISC_TOTAL)) :  getPartOfSet(tag)[TOTAL_DISCS];
  }
  
  public static String getAlbumArtist(Tag tag) {
	  return tag.getFirst(FieldKey.ALBUM_ARTIST);
  }
  
  public static String getArtist(Tag tag) {
    return tag.getFirst(FieldKey.ARTIST);
  }
  
  public static String getAlbum(Tag tag) {
	  return tag.getFirst(FieldKey.ALBUM).replaceAll(",[\\s]*Disc[\\s]*[0-9]+", "").replaceAll(SINGLE_TAG.replace("(", "\\(").replace(")","\\)"),"").trim();
  }
  
  private static int[] getPartOfSet(Tag tag) {
	  String partOfSet = tag.getFirst(PART_OF_SET);
	  if (partOfSet != null && partOfSet.indexOf("/") != -1) {
		  String[] partOfSetString = partOfSet.split("/");
		  int[] result = new int[partOfSetString.length];
		  for (int i=0; i<partOfSetString.length; i++) {
			  try {
			    result[i] = Integer.parseInt(partOfSetString[i]);
			  }
			  catch (NumberFormatException nfe) {}
		  }
		  return result;
	  }	 
	  else {
		  return new int[] {1,1};
	  }
  }
  
  public static String getGenre(Tag tag) {
	  return tag.getFirst(FieldKey.GENRE);
  }
  
  public static boolean isChildrens(Tag tag) {
	  return "Childrens".equals(getGenre(tag));
  }
  
  public static boolean isSoundtrack(Tag tag) {
    return "Soundtrack".equals(getGenre(tag));
  }

  public static String getTrackName(Tag tag) {
	  return tag.getFirst(FieldKey.TITLE); 
  }
  
  private static String stripIllegalFileChars(String input) {
	    log.debug("--> stripIllegalFileChars(" + input + ")");
	    String result = input.replace(":", ";")
	        .replace("\"", ".")
	        .replace("<", ".")
	        .replace(">", ".")
	        .replace("/", ".")
	        .replace("\\", ".")
	        .replace("|", ".")
	        .replace("?", ".")
	        .replace("*", ".")
	        .replaceAll("\\.\\.*", "\\.");
	    log.debug("--- stripIllegalFileChars: result: " + result);
	    log.debug("<-- stripIllegalFileChars()");
	    return result;
	  }  
  
  public static String getTargetFilename(Tag tag) {
	    log.debug("--> getTargetFilename(" + tag + ")");
	    log.debug("--- getTargetFilename(): Determining target file name from tag information");
	    StringBuffer targetFileName = new StringBuffer();
	    if (TagUtils.isMultiDisc(tag)) {
	      log.debug("--- getTargetFilename(): Multi disc album, appending disc number");
	      targetFileName.append("Disc");
	      targetFileName.append(TagUtils.getDiscNumber(tag));
	      targetFileName.append("-");
	    }
	    log.debug("--- getTargetFilename(): Appending track number");
	    targetFileName.append(TagUtils.getTrackNumber(tag) < 10 ? "0" + TagUtils.getTrackNumber(tag) : TagUtils
	        .getTrackNumber(tag));
	    targetFileName.append("-");
	    if (TagUtils.isCompilation(tag) || TagUtils.isSoundtrack(tag)) {
	      log.debug("--- getTargetFilename(): Compilation album, appending artist");
	      targetFileName.append(stripIllegalFileChars(TagUtils.getArtist(tag)));
	      targetFileName.append("-");
	    }
	    log.debug("--- getTargetFilename(): Appending track name");
	    targetFileName.append(stripIllegalFileChars(TagUtils.getTrackName(tag)));

	    log.debug("<-- getTargetFilename() returns " + targetFileName.toString());
	    return targetFileName.toString();
	  }

	  public static String getTargetPath(Tag tag) {
	    log.debug("--> getTargetPath(" + tag + ")");
	    // Determine target folder
	    StringBuffer targetPath = new StringBuffer();

	    if (TagUtils.isChildrens(tag)) {
	      log.debug("--- getTargetPath(): Detected childrens file");
	      targetPath.append(KIDS_FOLDER);
	      targetPath.append(File.separator);
	    }
	    else if (TagUtils.isSoundtrack(tag)) {
	      log.debug("--- getTargetPath(): Soundtrack");
	      targetPath.append(ALBUMS_FOLDER);
	      targetPath.append(File.separator);
	      targetPath.append(stripIllegalFileChars("Soundtrack"));
	      targetPath.append(File.separator);      
	    }    
	    else if (TagUtils.isCompilation(tag)) {
	      log.debug("--- getTargetPath(): Detected compilation");
	      targetPath.append(COMPILATIONS_FOLDER);
	      targetPath.append(File.separator);
	    }
	    else if (TagUtils.isSingle(tag)) {
	      log.debug("--- getTargetPath(): Detected CD Single");
	      targetPath.append(SINGLES_FOLDER);
	      targetPath.append(File.separator);
	      targetPath.append(stripIllegalFileChars(TagUtils.getAlbumArtist(tag)));
	      targetPath.append(File.separator);
	    }
	    else {
	      log.debug("--- getTargetPath(): Default - album");
	      targetPath.append(ALBUMS_FOLDER);
	      targetPath.append(File.separator);
	      targetPath.append(stripIllegalFileChars(TagUtils.getAlbumArtist(tag)));
	      targetPath.append(File.separator);
	    }
	    targetPath.append(stripIllegalFileChars(TagUtils.getAlbum(tag)));
	    log.debug("<-- getTargetPath() returns " + targetPath.toString());
	    return targetPath.toString();
	  }
	    
  
}
