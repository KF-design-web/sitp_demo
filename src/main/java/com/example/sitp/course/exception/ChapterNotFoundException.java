package com.example.sitp.course.exception;

public class ChapterNotFoundException extends RuntimeException {

    public ChapterNotFoundException() {

        super("Episode not found.");
    }

}
