package com.example.sitp.course.exception;

/**
 * A trainer asked to delete a course that still carries redeemable codes.
 * The crossed-out row history law says code rows are never silently removed,
 * so the deletion is refused instead of crashing on the foreign key.
 * The handler answers 409 "This course has codes handed out and cannot be deleted yet."
 */
public class CourseDeletionBlockedException extends RuntimeException {
}
