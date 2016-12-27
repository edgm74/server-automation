package uk.co.mackenney.musicautomation;

import java.io.File;


public class ConversionConfiguration {
  private File targetDir;
  private String fileExtension;
  private String conversionCommand;
  public ConversionConfiguration(String targetDir, String fileExtension, String conversionCommand) {
    this.targetDir = new File(targetDir);
    this.fileExtension = fileExtension;
    this.conversionCommand = conversionCommand;
  }
  
  public File getTargetDir() {
    return targetDir;
  }
  public String getFileExtension() {
    return fileExtension;
  }
  public String getConversionCommand() {
    return conversionCommand;
  }
  
  public String toString() {
    return targetDir + ":" + fileExtension + ":" + conversionCommand;
  }
}