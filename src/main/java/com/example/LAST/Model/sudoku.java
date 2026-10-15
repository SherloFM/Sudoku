package com.example.LAST.Model;

import lombok.Getter;
import lombok.Setter;

/**
 * Domain model for a single Sudoku puzzle.
 *
 * <p>Carries the two grids the solve flow works with:</p>
 * <ul>
 *   <li>{@link #board} - the puzzle parsed from the uploaded file (0 = empty).</li>
 *   <li>{@link #solution} - the finished grid produced by the solver.</li>
 * </ul>
 *
 * <p>Reading the upload and running the solver live in
 * {@code com.example.LAST.Service.sudokuService}, not in the model.</p>
 */
@Getter
@Setter
public class sudoku {

    /** Number of rows and columns on the board. */
    public static final int BOARD_SIZE = 9;

    /** Width and height of a 3x3 sub-grid (box). */
    public static final int SUBGRID_SIZE = 3;

    /** Value used to represent an empty cell. */
    public static final int EMPTY = 0;

    /** Input puzzle parsed from the upload. 0 = empty, 1-9 = a given clue. */
    private int[][] board;

    /** Solver output: the completed 9x9 grid. */
    private int[][] solution;
}
