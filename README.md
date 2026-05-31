# Server Automation - Media Processor

A robust Java-based service designed to automate the ingestion, tagging, and transcoding of media files. This tool monitors specific folders for new content and applies a set of configurable rules to organize and convert the data.

## Features

- **Real-time Monitoring**: Automatically detects new files using the Java NIO WatchService.
- **Atomic Operations**: Ensures files are not "in-use" (via `lsof`) before attempting to process them.
- **Audio Tagging**: Normalizes metadata for various artists, soundtracks, and multi-disc albums.
- **Multi-format Transcoding**: Leverages `ffmpeg` to generate multiple output versions (e.g., MP3, AAC) from a single lossless source.
- **Automated Cleanup**: Deletes source files and prunes empty directories after successful processing.
- **Error Tracking**: Generates detailed `.err` logs for any failed processing attempts.

## Requirements

- **Java 8** or higher.
- **FFmpeg**: Must be installed and available in the system PATH for transcoding support.
- **lsof**: Required on Unix-like systems to check file handles.

## Core Components

### File Watcher
The `FileWatcherService` orchestrates the monitoring of input directories. It handles the recursive registration of subdirectories and manages the queue of files ready for processing.

### Audio Manager
The `AudioManager` is the primary processor. It:
1. Reads audio metadata using `jaudiotagger`.
2. Fixes common tagging inconsistencies.
3. Copies the original lossless file to a permanent library location.
4. Runs `ffmpeg` conversions for mobile devices or web streaming.

### Process Life Cycle
1. **PENDING**: File detected in watch folder.
2. **READY_TO_PROCESS**: File is no longer being written to by another process.
3. **PROCESSING**: File is copied to a temporary location and the `FileProcessor` is invoked.
4. **PROCESSED**: Logic completed successfully; source is deleted.
5. **ERROR**: Logic failed; error log generated.

## Configuration

The application is built on Spring Boot. Configuration is typically handled via `application.properties` or YAML, where you can define:
- `inputDirs`: List of paths to monitor.
- `tempDirectory`: Location for transient processing files.
- `outputDestinations`: Transcoding profiles including bitrate and format.

## Development

To build the project, use the included Gradle wrapper:
```bash
./gradlew build
```