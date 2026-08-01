package com.kitradar.ingest;

public class RefreshAlreadyRunningException extends RuntimeException {

    public RefreshAlreadyRunningException() {
        super("A catalogue refresh is already running");
    }
}
