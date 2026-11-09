package minisoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Reads one log file and hands each line to a handler.
 *
 * In follow mode it behaves like "tail -f": it starts at the end of the
 * file and waits for new lines. In replay mode it reads the whole file
 * once and stops, which is handy for testing with sample logs.
 */
public class LogTailer implements Runnable {

    private static final long POLL_MS = 500;

    private final Path path;
    private final boolean replay;
    private final Consumer<String> handler;
    private volatile boolean stopped = false;

    public LogTailer(Path path, boolean replay, Consumer<String> handler) {
        this.path = path;
        this.replay = replay;
        this.handler = handler;
    }

    public void stop() {
        this.stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void run() {
        try {
            if (replay) {
                readWholeFile();
            } else {
                follow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("stopped reading " + path + ": " + e.getMessage());
        }
    }

    private void readWholeFile() throws IOException {
        // ISO-8859-1 never fails on odd bytes, which logs are full of
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
            String line;
            while (!stopped && (line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    handler.accept(line);
                }
            }
        }
    }

    private void follow() throws IOException, InterruptedException {
        while (!Files.exists(path) && !Thread.currentThread().isInterrupted() && !stopped) {
            System.err.println("waiting for " + path + " to appear...");
            Thread.sleep(2000);
        }

        if (stopped || Thread.currentThread().isInterrupted()) {
            return;
        }

        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long position = file.length();
            file.seek(position);

            while (!Thread.currentThread().isInterrupted() && !stopped) {
                if (file.length() < position) {
                    // file was truncated, start over from the top
                    position = 0;
                    file.seek(0);
                }

                String line = file.readLine();
                if (line == null) {
                    Thread.sleep(POLL_MS);
                    continue;
                }
                position = file.getFilePointer();
                if (!line.isEmpty()) {
                    handler.accept(line);
                }
            }
        }
    }
}
