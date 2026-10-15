package com.example.LAST.Service;

import com.example.LAST.Exceptions.SudokuFileNotFoundException;
import com.example.LAST.Exceptions.invalidCharacterException;
import com.example.LAST.Model.sudoku;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Service
public class sudokuServiceReader {

    private static final int SIZE = sudoku.BOARD_SIZE;   // 9

    /** Parses the uploaded comma-separated puzzle into a sudoku model. */
    public sudoku read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SudokuFileNotFoundException("No file was uploaded.");
        }

        String[] rows;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.US_ASCII))) {

            rows = reader.lines()
                         .filter(line -> !line.isBlank())   // ignore stray blank lines
                         .toArray(String[]::new);

        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file.", e);
        }

        if (rows.length != SIZE) {
            throw new invalidCharacterException(
                    "Expected " + SIZE + " rows but found " + rows.length + ".");
        }

        int[][] board = new int[SIZE][SIZE];

        for (int r = 0; r < SIZE; r++) {
            String line = rows[r].trim();
            if (line.endsWith(",")) {                       // tolerate a trailing comma
                line = line.substring(0, line.length() - 1);
            }
            String[] tokens = line.split(",", -1);          // -1 keeps empty cells

            if (tokens.length != SIZE) {
                throw new invalidCharacterException(
                        "Row " + r + " has " + tokens.length + " values, expected " + SIZE + ".");
            }

            for (int c = 0; c < SIZE; c++) {
                String token = tokens[c].trim();
                if (token.isEmpty() || token.equals(".") || token.equals("0")) {
                    board[r][c] = sudoku.EMPTY;
                } else if (token.length() == 1 && token.charAt(0) >= '1' && token.charAt(0) <= '9') {
                    board[r][c] = token.charAt(0) - '0';    // "5" -> 5
                } else {
                    throw new invalidCharacterException(
                            "Invalid value '" + token + "' at row " + r + ", column " + c + ".");
                }
            }
        }

        sudoku puzzle = new sudoku();
        puzzle.setBoard(board);
        return puzzle;
    }
}
