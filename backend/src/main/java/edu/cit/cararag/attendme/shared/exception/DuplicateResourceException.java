package edu.cit.cararag.attendme.shared.exception;

public class DuplicateResourceException extends RuntimeException {
    
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s : '%s'", resourceName, fieldName, fieldValue));
    }
    
    
    public DuplicateResourceException(String resourceName, String message) {
        super(String.format("%s: %s", resourceName, message));
    }
}
