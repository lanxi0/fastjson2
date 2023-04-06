package com.alibaba.fastjson2.stream;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

public class JSONStreamReaderUTF16
        extends JSONStreamReader {
    char[] buf;
    Reader input;
    JSONReader.Context context;

    JSONStreamReaderUTF16(Reader input, ObjectReaderAdapter objectReader) {
        super(objectReader);
        this.input = input;
        this.context = JSONFactory.createReadContext();
    }

    JSONStreamReaderUTF16(Reader input, Type[] types) {
        super(types);
        this.input = input;
        this.context = JSONFactory.createReadContext();
    }

    @Override
    protected boolean seekLine() throws IOException {
        if (buf == null) {
            if (input != null) {
                buf = new char[SIZE_256K];
                int cnt = input.read(buf);
                if (cnt == -1) {
                    inputEnd = true;
                    return false;
                }
                this.end = cnt;

                if (end > 3) {
                    if (buf[0] == -17 && buf[1] == -69 && buf[2] == -65) {
                        off = 3;
                        lineNextStart = off;
                    }
                }
            }
        }

        for (int k = 0; k < 3; ++k) {
            lineTerminated = false;

            for (int i = off; i < end; i++) {
                if (i + 4 < end) {
                    char b0 = buf[i];
                    char b1 = buf[i + 1];
                    char b2 = buf[i + 2];
                    char b3 = buf[i + 3];
                    if (b0 > '"' && b1 > '"' && b2 > '"' && b3 > '"') {
                        lineSize += 4;
                        i += 3;
                        continue;
                    }
                }

                char ch = buf[i];

                if (ch == '\n') {
                    if (lineSize > 0 || (features & Feature.IgnoreEmptyLine.mask) == 0) {
                        rowCount++;
                    }
                    lineTerminated = true;
                    lineSize = 0;
                    lineEnd = i;
                    lineStart = lineNextStart;
                    lineNextStart = off = i + 1;

                    break;
                } else if (ch == '\r') {
                    if (lineSize > 0 || (features & Feature.IgnoreEmptyLine.mask) == 0) {
                        rowCount++;
                    }

                    lineTerminated = true;
                    lineSize = 0;
                    lineEnd = i;

                    int n = i + 1;
                    if (n >= end) {
                        break;
                    }
                    if (buf[n] == '\n') {
                        i++;
                    }

                    lineStart = lineNextStart;
                    lineNextStart = off = i + 1;

                    break;
                } else {
                    lineSize++;
                }
            }

            if (!lineTerminated) {
                if (input != null && !inputEnd) {
                    int len = end - off;
                    if (off > 0) {
                        if (len > 0) {
                            System.arraycopy(buf, off, buf, 0, len);
                        }
                        lineStart = lineNextStart = 0;
                        off = 0;
                        end = len;
                    }

                    int cnt = input.read(buf, end, buf.length - end);
                    if (cnt == -1) {
                        inputEnd = true;
                        if (off == end) {
                            return false;
                        }
                    } else {
                        end += cnt;
                        continue;
                    }
                }

                lineStart = lineNextStart;
                lineEnd = end;
                rowCount++;
                lineSize = 0;
                off = end;
            }

            lineTerminated = off == end;
            break;
        }

        return true;
    }

    @Override
    public <T> T readLineObject() {
        try {
            if (inputEnd) {
                return null;
            }

            if (input == null) {
                if (off >= end) {
                    return null;
                }
            }

            boolean result = seekLine();

            if (!result) {
                return null;
            }
        } catch (IOException e) {
            throw new JSONException("seekLine error", e);
        }

        JSONReader reader = JSONReader.of(buf, lineStart, lineEnd - lineStart, context);

        Object object;
        if (objectReader != null) {
            object = objectReader.readObject(reader, null, null, features);
        } else if (reader.isArray() && types != null && types.length != 0) {
            object = reader.readList(types);
        } else {
            object = reader.readAny();
        }

        return (T) object;
    }
}
