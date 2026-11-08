package id.co.bca.crosbatch.file;

/** A line of an input file could not be converted. Carries the file and line number for the log. */
public class FileParseException extends RuntimeException {

    public FileParseException(String fileName, long lineNumber, String message, Throwable cause) {
        super(fileName + " line " + lineNumber + ": " + message, cause);
    }
}
