package uk.co.mackenney.automation.filewatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;


public class FileProcess {

    private static Logger log = LoggerFactory.getLogger(FileWatcherController.class);

    /**
     * PENDING: File has been detected, but may still have open file handles
     * READY_TO_PROCESS: File no longer has open file handles
     * PROCESSING: The file processor thread has accepted the file and is processing it
     * PROCESSED: The file processor thread has completed processing and the file is ready to delete.
     */
    private enum FileProcessStatus {PENDING, READY_TO_PROCESS, PROCESSING, PROCESSED, ERROR}

    private Path filePath;
    private Path tempFilePath;
    private Map<Long, String> logMessages = new LinkedHashMap<>();
    private FileProcessStatus status;

    public FileProcess(Path filePath) {
        this.filePath = filePath;
        this.status = FileProcessStatus.PENDING;
        log("FileProcess Created");
    }

    public int hashCode() {
        return filePath.hashCode();
    }

    public boolean equals(Object arg) {
        if (arg instanceof FileProcess) {
            return this.filePath.equals(((FileProcess)arg).filePath);
        }
        else return false;
    }

    public Path getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(Path tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

    public Map<Long,String> getLogMessages() {
        return logMessages;
    }

    public Path getPath() {
        return filePath;
    }

    public boolean isPending() {
        return this.status == FileProcessStatus.PENDING;
    }

    public boolean isReadyToProcess() {
        return this.status == FileProcessStatus.READY_TO_PROCESS;
    }

    public boolean isProcessing() {
        return this.status == FileProcessStatus.PROCESSING;
    }

    public boolean isProcessed() {
        return this.status == FileProcessStatus.PROCESSED;
    }

    public boolean isError() { return this.status == FileProcessStatus.ERROR;  }

    public void log(String message) {
        this.logMessages.put(System.currentTimeMillis(), message);
    }

    private void setStatus(FileProcessStatus status) {
        this.status = status;
        log("Changed to status " + status);
        log.trace(this.toString());
    }

    public void setReadyToProcess() {
        setStatus(FileProcessStatus.READY_TO_PROCESS);
    }

    public void setProcessing() {
        setStatus(FileProcessStatus.PROCESSING);
    }

    public void setProcessed() {
        setStatus(FileProcessStatus.PROCESSED);
    }

    public void setError() { setStatus(FileProcessStatus.ERROR); }

    public String toString() {
        return filePath + " (" + status + ")";
    }

    public String toLogString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(toString());
        for (Long time : logMessages.keySet()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            buffer.append("\n");
            buffer.append(sdf.format(new Date(time)));
            buffer.append(":  ");
            buffer.append(logMessages.get(time));
        }
        return buffer.toString();
    }

}
