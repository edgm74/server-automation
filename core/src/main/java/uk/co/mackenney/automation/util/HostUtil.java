package uk.co.mackenney.automation.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.co.mackenney.automation.filewatcher.FileWatcherController;

import java.io.InputStream;
import java.io.StringWriter;

/**
 * Created by developer on 09/01/17.
 */
@Component
public class HostUtil {
    public class HostCallResult {

        private String[] originalCommand;
        private String stdout;
        private String stderr;
        private int exitCode;

        public HostCallResult(String[] originalCommand, String stdout, String stderr, int exitCode) {
            this.originalCommand = originalCommand;
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
        }

        public String[] getOriginalCommand() {
            return originalCommand;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String toString() {
            StringBuffer result = new StringBuffer();
            for (int i=0; i<originalCommand.length; i++) {
                if (originalCommand[i].contains(" ")) {
                    result.append("\"");
                }
                result.append(originalCommand[i]);
                if (originalCommand[i].contains(" ")) {
                    result.append("\"");
                }
                result.append(" ");
            }
            result.append("\nExit Code: ");
            result.append(exitCode);
            result.append("\nSTDOUT: ");
            result.append(stdout);
            result.append("\nSTDERR: ");
            result.append(stderr);
            return result.toString();
        }


    }

    private static Logger log = LoggerFactory.getLogger(HostUtil.class);

    public HostCallResult runHostCommand(String[] command) {
        log.trace("--> runHostCommand(" + command + ")");
        try {
            Process p = Runtime.getRuntime().exec(command);
            InputStream is = p.getInputStream();
            InputStream eis = p.getErrorStream();
            StringWriter sw = new StringWriter();
            StringWriter esw = new StringWriter();
            int exitCode = p.waitFor();
            IOUtils.copy(is,sw);
            IOUtils.copy(eis,esw);
            HostCallResult result = new HostCallResult(command,sw.toString(), esw.toString(), exitCode);
            log.trace("--- runHostCommand: result: " + result);
            return result;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
