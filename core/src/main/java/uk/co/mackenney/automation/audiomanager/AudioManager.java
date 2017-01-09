package uk.co.mackenney.automation.audiomanager;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.co.mackenney.automation.filewatcher.FileProcess;
import uk.co.mackenney.automation.filewatcher.FileProcessor;
import uk.co.mackenney.automation.util.FileUtil;
import uk.co.mackenney.automation.util.HostUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.StringTokenizer;

@Component
public class AudioManager implements FileProcessor {
    private static Logger log = LoggerFactory.getLogger(AudioManager.class);

    @Autowired
    private AudioManagerConfiguration config;

    @Autowired
    private HostUtil hostUtil;

    @Autowired
    private FileUtil fileUtil;

    private boolean isAllowedInputFileExtension(FileProcess process) {
        log.trace("--> isAllowedInputFileExtension(" + process.getPath() + ")");
        for (String extension : config.getAllowedInputFileExtensions()) {
            log.trace("--- isAllowedInputFileExtension(): Checking against extension: " + extension);
            if (process.getPath().getFileName().toString().endsWith(extension)) return true;
        }
        return false;
    }

    private void fixTags(Path filePath) throws Exception {
        log.trace("--> fixTags(" + filePath + ")");
        AudioFile f = AudioFileIO.read(filePath.toFile());
        Tag tag = f.getTag();
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
        log.trace("<-- fixTags()");
    }

    private void ensureDirectoryExists(Path path) {
        log.debug("--> ensureDirectoryExists(" + path + ")");
        File dir = path.toFile();
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        log.debug("<-- ensureDirectoryExists()");
    }

    private void copyArtwork(Tag tag, Path path) throws IOException {
        log.trace("--> copyArtwork(" + tag + "," + path + ")");
        File f = new File(path.toFile(), "folder.jpg");
        if (!f.isFile()) {
            log.debug("--- copyArtwork(): folder.jpg does not yet exist in directory " + path);
            Artwork artwork = tag.getFirstArtwork();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(artwork.getBinaryData());
            fos.flush();
            fos.close();
            log.debug("--- copyArtwork(): Finished copying file");
        }
        log.trace("<--copyArtwork()");
    }


    private Path getTargetPath(Tag t, Path parent, String extension) {
        return parent.resolve(TagUtils.getTargetPath(t)).resolve(TagUtils.getTargetFilename(t) + (extension.startsWith(".") ? "" : ".") + extension);
    }


    private void copyLosslessFileToTarget(Tag tag, Path inputFilePath, String originalFileExtension) throws Exception {
        log.trace("--> copyLosslessFileToTarget(" + inputFilePath + "," + originalFileExtension + ")");
        Path targetFilePath = getTargetPath(tag,Paths.get(config.getOriginalFormatDestination()),originalFileExtension);
        log.debug("--- run(): Copying audio file to target directory: " + targetFilePath.getParent());
        ensureDirectoryExists(targetFilePath.getParent());
        copyArtwork(tag, targetFilePath.getParent());
        Files.copy(inputFilePath,targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        log.trace("<-- copyLosslessFileToTarget()");
    }

    private void runFfmpeg(Tag tag, Path inputFile, Path outputFile, String conversionOptions) throws IOException {
        log.trace("--> runFfmpeg(" + inputFile + "," + outputFile + "," + conversionOptions + ")");
        ensureDirectoryExists(outputFile.getParent());
        copyArtwork(tag, outputFile.getParent());
        StringTokenizer tokenizer = new StringTokenizer(conversionOptions," ");
        ArrayList<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputFile.toString());
        while (tokenizer.hasMoreTokens()) {
            command.add(tokenizer.nextToken());
        }
        command.add(outputFile.toString());

        HostUtil.HostCallResult result = hostUtil.runHostCommand(command.toArray(new String[command.size()]));
        if (result.getExitCode() != 0) throw new RuntimeException(result.toString());
    }

    @Override
    public void processFile(FileProcess fileProcess) {
        log.trace("--> processFile(" + fileProcess + ")");
        if (isAllowedInputFileExtension(fileProcess)) {
            log.debug("--- processFile(): Processing file: " + fileProcess);
            try {
                AudioFile f = AudioFileIO.read(fileProcess.getTempFilePath().toFile());
                Tag tag = f.getTag();
                fixTags(fileProcess.getTempFilePath());
                copyLosslessFileToTarget(tag, fileProcess.getTempFilePath(), fileUtil.getExtension(fileProcess.getPath()));

                for (AudioManagerConfiguration.OutputDestination destination : config.getOutputDestinations()) {
                    runFfmpeg(tag
                             ,fileProcess.getTempFilePath()
                             ,getTargetPath(tag, Paths.get(destination.getDirectory()), destination.getFileExtension())
                             ,destination.getConversionOptions());
                }
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        else {
            log.debug("--- processFile(): Skipping file: " + fileProcess);
        }
        log.trace("<-- processFile()");
    }
}
