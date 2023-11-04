package me.heartalborada.biliDownloader.Bili.Exceptions;

import lombok.Getter;

public class BadRequestDataException extends RuntimeException {
    @Getter
    private final int code;
    @Getter
    private final String message;

    public BadRequestDataException(int code, String message) {
        super(String.format("Api returned invalid code: %d, message: %s", code, message));
        this.code = code;
        this.message = message;
    }
}
