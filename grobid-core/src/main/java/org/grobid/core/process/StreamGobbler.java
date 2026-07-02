/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.process;

import java.io.IOException;
import java.io.InputStream;

/**
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 */
public class StreamGobbler extends InputStream {
    class GobblerThread extends Thread {
        @Override
        public void run() {
            byte[] buff = new byte[8192];

            while (true) {
                try {
                    int avail = is.read(buff);

                    synchronized (synchronizer) {
                        if (avail <= 0) {
                            isEOF = true;
                            synchronizer.notifyAll();
                            break;
                        }

                        int space_available = buffer.length - write_pos;

                        if (space_available < avail) {
                            /* compact/resize buffer */

                            int unread_size = write_pos - read_pos;
                            int need_space = unread_size + avail;

                            byte[] new_buffer = buffer;

                            if (need_space > buffer.length) {
                                int inc = need_space / 3;
                                inc = (inc < 256) ? 256 : inc;
                                inc = (inc > 8192) ? 8192 : inc;
                                new_buffer = new byte[need_space + inc];
                            }

                            if (unread_size > 0)
                                System.arraycopy(buffer, read_pos, new_buffer, 0, unread_size);

                            buffer = new_buffer;

                            read_pos = 0;
                            write_pos = unread_size;
                        }

                        System.arraycopy(buff, 0, buffer, write_pos, avail);
                        write_pos += avail;

                        synchronizer.notifyAll();
                    }
                } catch (IOException e) {
                    synchronized (synchronizer) {
                        exception = e;
                        synchronizer.notifyAll();
                        break;
                    }
                }
            }
        }
    }

    private InputStream is;

    private final Object synchronizer = new Object();

    private boolean isEOF = false;
    private boolean isClosed = false;
    private IOException exception = null;

    private byte[] buffer = new byte[2048];
    private int read_pos = 0;
    private int write_pos = 0;

    public StreamGobbler(InputStream is) {
        this.is = is;
        GobblerThread t = new GobblerThread();
        t.setDaemon(true);
        t.start();
    }

    @Override
    public int read() throws IOException {
        boolean wasInterrupted = false;

        try {
            synchronized (synchronizer) {
                if (isClosed)
                    throw new IOException("This StreamGobbler is closed.");

                while (read_pos == write_pos) {
                    if (exception != null)
                        throw exception;

                    if (isEOF)
                        return -1;

                    try {
                        synchronizer.wait();
                    } catch (InterruptedException e) {
                        wasInterrupted = true;
                    }
                }
                return buffer[read_pos++] & 0xff;
            }
        } finally {
            if (wasInterrupted)
                Thread.currentThread().interrupt();
        }
    }

    @Override
    public int available() throws IOException {
        synchronized (synchronizer) {
            if (isClosed)
                throw new IOException("This StreamGobbler is closed.");

            return write_pos - read_pos;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public void close() throws IOException {
        synchronized (synchronizer) {
            if (isClosed)
                return;
            isClosed = true;
            isEOF = true;
            synchronizer.notifyAll();
            is.close();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null)
            throw new NullPointerException();

        if ((off < 0) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0) || (off > b.length))
            throw new IndexOutOfBoundsException();

        if (len == 0)
            return 0;

        boolean wasInterrupted = false;

        try {
            synchronized (synchronizer) {
                if (isClosed)
                    throw new IOException("This StreamGobbler is closed.");

                while (read_pos == write_pos) {
                    if (exception != null)
                        throw exception;

                    if (isEOF)
                        return -1;

                    try {
                        synchronizer.wait();
                    } catch (InterruptedException e) {
                        wasInterrupted = true;
                    }
                }

                int avail = write_pos - read_pos;

                avail = (avail > len) ? len : avail;

                System.arraycopy(buffer, read_pos, b, off, avail);

                read_pos += avail;

                return avail;
            }
        } finally {
            if (wasInterrupted)
                Thread.currentThread().interrupt();
        }
    }
}
