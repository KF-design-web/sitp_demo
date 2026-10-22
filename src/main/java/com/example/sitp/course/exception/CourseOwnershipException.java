package com.example.sitp.course.exception;

/**
 * A trainer knocked on a course they do not own (update or delete).
 * The handler answers 403 "This course belongs to another trainer."
 */
public class CourseOwnershipException extends RuntimeException {
}
