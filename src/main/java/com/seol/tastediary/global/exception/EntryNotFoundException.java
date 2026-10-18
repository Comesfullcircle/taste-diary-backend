package com.seol.tastediary.global.exception;

public class EntryNotFoundException extends RuntimeException {
    public EntryNotFoundException(Long id) {
        super("기록을 찾을 수 없습니다. ID: " + id);
    }
}
