package com.attendme;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleTest {
    
    @GetMapping("/simple-test")
    public String test() {
        System.out.println("✅ SimpleTest.test() called!");
        return "Simple test works!";
    }
}