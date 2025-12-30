package io.github.gohoski.numai;

/**
 * Created by Gleb on 23.08.2025.
 * InputStream that automatically closes the connection
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

class ConnectionInputStream extends InputStream {
    private final InputStream inputStream;
    private final HttpURLConnection connection;

    ConnectionInputStream(InputStream inputStream, HttpURLConnection connection) {
        this.inputStream = inputStream;
        this.connection = connection;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        try {
            inputStream.close();
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
}