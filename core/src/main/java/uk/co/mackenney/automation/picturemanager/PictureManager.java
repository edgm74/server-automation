package uk.co.mackenney.automation.picturemanager;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.co.mackenney.automation.filewatcher.FileProcess;
import uk.co.mackenney.automation.filewatcher.FileProcessor;

import java.nio.file.Path;

/**
 * Created by developer on 07/01/17.
 */
@Component
@ConfigurationProperties(prefix="pictureManager")
public class PictureManager implements FileProcessor {

    @Override
    public void processFile(FileProcess fileProcess) {

    }
}

