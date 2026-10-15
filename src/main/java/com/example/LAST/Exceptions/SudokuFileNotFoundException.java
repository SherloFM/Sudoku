package com.example.LAST.Exceptions;

public class SudokuFileNotFoundException extends RuntimeException {
    public SudokuFileNotFoundException(String message) {
        super(message);
    }
}
