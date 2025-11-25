package com.example.inventory.exception;

import java.util.SplittableRandom;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message){
        super(message);
    }
}
