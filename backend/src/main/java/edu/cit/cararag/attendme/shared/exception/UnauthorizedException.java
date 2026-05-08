package edu.cit.cararag.attendme.shared.exception;

public class UnauthorizedException extends RuntimeException {
    
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    
    public UnauthorizedException() {
        super("You are not authorized to perform this action");
    }
    
    
    public UnauthorizedException(String username, String action) {
        super(String.format("User '%s' is not authorized to %s", username, action));
    }
}
