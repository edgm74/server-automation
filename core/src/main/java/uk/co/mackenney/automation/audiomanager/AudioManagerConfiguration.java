package uk.co.mackenney.automation.audiomanager;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by developer on 07/01/17.
 */
@Component
@ConfigurationProperties(prefix="audioManager")
public class AudioManagerConfiguration {

    private String originalFormatDestination;

    public String getOriginalFormatDestination() {
        return originalFormatDestination;
    }

    public void setOriginalFormatDestination(String originalFormatDestination) {
        this.originalFormatDestination = originalFormatDestination;
    }

    private List<String> allowedInputFileExtensions = new ArrayList<>();

    public List<String> getAllowedInputFileExtensions() {
        return this.allowedInputFileExtensions;
    }

    public static class OutputDestination {
        private String directory;
        private String conversionOptions;
        private String fileExtension;

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public String getConversionOptions() {
            return conversionOptions;
        }

        public void setConversionOptions(String conversionOptions) {
            this.conversionOptions = conversionOptions;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }
    }

    private List<OutputDestination> outputDestinations = new ArrayList<>();

    public List<OutputDestination> getOutputDestinations() {
        return outputDestinations;
    }

    @PostConstruct
    public void validateParameters() {

    }


}
