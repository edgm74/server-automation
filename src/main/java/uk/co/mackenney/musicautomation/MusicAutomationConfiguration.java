package uk.co.mackenney.musicautomation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.log4j.Logger;

public class MusicAutomationConfiguration {
  
  private static Logger log = Logger.getLogger(MusicAutomationConfiguration.class);
  private static File sourceDir = new File("C:\\test\\");
  private static File targetDirLossless = new File("C:\\target\\");
  private static String losslessFileExtension = ".wma";
  private static int threadPoolSize = 10;  

  
  private static ConversionConfiguration[] conversionConfiguration = new ConversionConfiguration[] {
    new ConversionConfiguration("C:\\target_lossy\\","mp3","\"C:\\Program Files (x86)\\Illustrate\\dBpoweramp\\CoreConverter.exe\" -infile=\"SOURCE_FILE\" -outfile=\"TARGET_FILE\" -convert_to=\"mp3 (Lame)\" -b 320 -q 0 --replaygain-accurate")
  };
  
  static {
    log.debug("--> <static init>()");
    Properties props = new Properties();

    if (MusicAutomationConfiguration.class.getClassLoader().getResourceAsStream("musicautomation.properties") != null) {
      try {
        props.load(MusicAutomationConfiguration.class.getClassLoader().getResourceAsStream("musicautomation.properties"));
        if (log.isDebugEnabled()) {
          StringWriter sw = new StringWriter();
          props.list(new PrintWriter(sw));
          log.debug("--- loadProperties(): Loaded properties: " + sw.toString());
        }
        sourceDir = props.getProperty("sourceDir") == null ? sourceDir : new File(props.getProperty("sourceDir"));
        targetDirLossless = props.getProperty("targetDirLossless") == null ? targetDirLossless : new File(props.getProperty("targetDirLossless"));
        losslessFileExtension = props.getProperty("losslessFileExtension") == null ? losslessFileExtension : props.getProperty("losslessFileExtension");
        threadPoolSize = props.getProperty("threadPoolSize") == null ? threadPoolSize : Integer.parseInt(props.getProperty("threadPoolSize"));
        int conversionNumber = 0;
        while (props.getProperty("targetDir" + conversionNumber) != null) {
          conversionNumber++;
        }
        if (conversionNumber > 0) conversionConfiguration = new ConversionConfiguration[conversionNumber];
        conversionNumber = 0;
        while (props.getProperty("targetDir" + conversionNumber) != null) {
          ConversionConfiguration config = new ConversionConfiguration(props.getProperty("targetDir" + conversionNumber), props.getProperty("fileExtension" + conversionNumber), props.getProperty("conversionCommand" + conversionNumber));
          conversionConfiguration[conversionNumber] = config;
          conversionNumber++;
        }
      } 
      catch (IOException ioe) {}
    }
    log.info("System settings: ");
    log.info("sourceDir:              " + sourceDir);
    log.info("targetDirLossless:      " + targetDirLossless);
    log.info("losslessFileExtension:  " + losslessFileExtension);
    log.info("threadPoolSize:         " + threadPoolSize);
    log.debug("<-- <constructor>()");
  }

  public static File getSourceDir() {
    return sourceDir;
  }

  public static File getTargetDirLossless() {
    return targetDirLossless;
  }

  public static String getLosslessFileExtension() {
    return losslessFileExtension;
  }


  public static int getThreadPoolSize() {
    return threadPoolSize;
  }
  
  public static ConversionConfiguration[] getConversionConfiguration() {
    return conversionConfiguration;
  }
  
}
