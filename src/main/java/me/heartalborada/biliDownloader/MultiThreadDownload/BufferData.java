package me.heartalborada.biliDownloader.MultiThreadDownload;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class BufferData implements Comparable {
    @Getter
    private final long num;
    @Getter
    private final long startPos;
    @Getter
    private final long endPos;
    private final ByteBuffer buffer;

    public BufferData(long num, long startPos, long endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.num = num;
        this.buffer = ByteBuffer.allocate((int) (endPos - startPos + 1));
    }

    public void write(byte[] src) {
        write(src, 0, src.length);
    }

    public void write(byte[] src, int offset, int len) {
        buffer.put(src, offset, len);
    }

    public byte[] array() {
        return buffer.array();
    }

    @Override
    public int compareTo(@NotNull Object o) {
        BufferData buffData = (BufferData) o;
        return (int) (this.getNum() - buffData.getNum());
    }
}
