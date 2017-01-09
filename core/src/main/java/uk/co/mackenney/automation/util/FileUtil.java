package uk.co.mackenney.automation.util;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Created by developer on 09/01/17.
 */
@Component
public class FileUtil {
    public String getExtension(Path inputFilePath) {
        String filename = inputFilePath.getFileName().toString();
        return filename.substring(filename.lastIndexOf("."));
    }
}
