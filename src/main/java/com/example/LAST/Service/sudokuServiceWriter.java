package com.example.LAST.Service;

import com.example.LAST.Model.sudoku;
import org.springframework.stereotype.Service;

/**
 * Writes a solved grid back out as a comma-separated ASCII string.
 */
@Service
public class sudokuServiceWriter {

    private static final int SIZE = sudoku.BOARD_SIZE;   // 9

    /** Renders the grid as 9 comma-separated rows (empty cells become '.'). */
    public String write(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (c > 0) sb.append(',');
                int v = grid[r][c];
                sb.append(v == sudoku.EMPTY ? '.' : (char) ('0' + v));
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
