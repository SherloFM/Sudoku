package com.example.LAST.Service;

import com.example.LAST.Exceptions.invalidCharacterException;
import com.example.LAST.Model.sudoku;
import org.springframework.stereotype.Service;

@Service
public class sudokuServiceSolver {

    private static final int SIZE = sudoku.BOARD_SIZE;    // 9
    private static final int BOX  = sudoku.SUBGRID_SIZE;  // 3
    private static final int ALL  = (1 << SIZE) - 1;       // bits 0..8 == digits 1..9

    public enum Technique {
        FILLED,          // already has a value
        NAKED_SINGLE,    // exactly one candidate -> fill it
        HIDDEN_SINGLE,   // a digit that fits only here within a unit -> fill it
        NEEDS_SEARCH,    // no forced move, needs guessing / backtracking
        CONTRADICTION    // an empty cell with no candidates -> unsolvable
    }


    /** Solves a copy of the given puzzle and returns the completed grid. */
    public int[][] solve(int[][] puzzle) {
        int[][] board = copy(puzzle);   // never mutate the caller's array

        boolean progress = true;
        while (progress) {
            progress = false;
            int[][] candidates = computeCandidates(board);

            outer:
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    boolean placed;
                    switch (classify(board, candidates, r, c)) {
                        case NAKED_SINGLE  -> placed = applyNakedSingle(board, candidates, r, c);
                        case HIDDEN_SINGLE -> placed = applyHiddenSingle(board, candidates, r, c);
                        case CONTRADICTION -> throw new invalidCharacterException(
                                "No solution: cell at row " + r + ", column " + c + " has no candidates.");
                        default            -> placed = false;   // FILLED / NEEDS_SEARCH
                    }
                    if (placed) {
                        progress = true;
                        break outer;   // board changed -> recompute candidates and reclassify
                    }
                }
            }
        }

        // Logic stalled: fall back to search for the remaining NEEDS_SEARCH cells.
        if (!isSolved(board)) {
            solveBySearch(board);
        }
        return board;
    }



    /** boxes[box][rowInBox][colInBox] - the nine 3x3 sub-grids, box 0..8. */
    public int[][][] splitIntoBoxes(int[][] board) {
        int[][][] boxes = new int[SIZE][BOX][BOX];
        for (int box = 0; box < SIZE; box++) {
            int startRow = (box / 3) * BOX;
            int startCol = (box % 3) * BOX;
            for (int r = 0; r < BOX; r++) {
                for (int c = 0; c < BOX; c++) {
                    boxes[box][r][c] = board[startRow + r][startCol + c];
                }
            }
        }
        return boxes;
    }



    /** Classifies every cell of the board into a Technique. */
    public Technique[][] classifyBoard(int[][] board) {
        int[][] candidates = computeCandidates(board);
        Technique[][] result = new Technique[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                result[r][c] = classify(board, candidates, r, c);
            }
        }
        return result;
    }

    /** Decides which technique applies to a single cell. */
    public Technique classify(int[][] board, int[][] candidates, int r, int c) {
        if (board[r][c] != sudoku.EMPTY) return Technique.FILLED;

        int cand = candidates[r][c];
        if (cand == 0) return Technique.CONTRADICTION;
        if (Integer.bitCount(cand) == 1) return Technique.NAKED_SINGLE;
        if (isHiddenSingle(board, candidates, r, c)) return Technique.HIDDEN_SINGLE;

        return Technique.NEEDS_SEARCH;
    }

    /** For every empty cell, the still-legal digits as a bitmask (0 for filled cells). */
    public int[][] computeCandidates(int[][] board) {
        int[][] candidates = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == sudoku.EMPTY) {
                    candidates[r][c] = candidatesFor(board, r, c);
                }
            }
        }
        return candidates;
    }

    /** Digits not yet used in this cell's row, column AND box ("the missing numbers"). */
    private int candidatesFor(int[][] board, int r, int c) {
        int used = 0;
        for (int i = 0; i < SIZE; i++) {
            if (board[r][i] != sudoku.EMPTY) used |= bit(board[r][i]);
            if (board[i][c] != sudoku.EMPTY) used |= bit(board[i][c]);
        }
        int startRow = (r / BOX) * BOX;
        int startCol = (c / BOX) * BOX;
        for (int i = 0; i < BOX; i++) {
            for (int j = 0; j < BOX; j++) {
                int v = board[startRow + i][startCol + j];
                if (v != sudoku.EMPTY) used |= bit(v);
            }
        }
        return ALL & ~used;
    }

    /** True if some candidate digit can go nowhere else in this cell's row, column or box. */
    private boolean isHiddenSingle(int[][] board, int[][] candidates, int r, int c) {
        int cand = candidates[r][c];
        for (int digit = 1; digit <= SIZE; digit++) {
            int b = bit(digit);
            if ((cand & b) == 0) continue;                 // this cell can't hold it
            if (countInRow(board, candidates, r, b) == 1
             || countInCol(board, candidates, c, b) == 1
             || countInBox(board, candidates, r, c, b) == 1) {
                return true;
            }
        }
        return false;
    }

    private int countInRow(int[][] board, int[][] candidates, int r, int b) {
        int count = 0;
        for (int c = 0; c < SIZE; c++)
            if (board[r][c] == sudoku.EMPTY && (candidates[r][c] & b) != 0) count++;
        return count;
    }

    private int countInCol(int[][] board, int[][] candidates, int c, int b) {
        int count = 0;
        for (int r = 0; r < SIZE; r++)
            if (board[r][c] == sudoku.EMPTY && (candidates[r][c] & b) != 0) count++;
        return count;
    }

    private int countInBox(int[][] board, int[][] candidates, int r, int c, int b) {
        int count = 0;
        int startRow = (r / BOX) * BOX;
        int startCol = (c / BOX) * BOX;
        for (int i = 0; i < BOX; i++)
            for (int j = 0; j < BOX; j++) {
                int rr = startRow + i, cc = startCol + j;
                if (board[rr][cc] == sudoku.EMPTY && (candidates[rr][cc] & b) != 0) count++;
            }
        return count;
    }

    private static int bit(int digit) {
        return 1 << (digit - 1);   // digit 1 -> bit 0 ... digit 9 -> bit 8
    }


    /** Naked single: the cell has exactly one legal candidate -> place it. */
    private boolean applyNakedSingle(int[][] board, int[][] candidates, int r, int c) {
        if (board[r][c] != sudoku.EMPTY) return false;
        int cand = candidates[r][c];
        if (Integer.bitCount(cand) != 1) return false;

        board[r][c] = Integer.numberOfTrailingZeros(cand) + 1;
        return true;
    }

    /** Hidden single: place the digit that fits only in this cell within a unit. */
    private boolean applyHiddenSingle(int[][] board, int[][] candidates, int r, int c) {
        if (board[r][c] != sudoku.EMPTY) return false;

        int cand = candidates[r][c];
        for (int digit = 1; digit <= SIZE; digit++) {
            int b = bit(digit);
            if ((cand & b) == 0) continue;
            if (countInRow(board, candidates, r, b) == 1
             || countInCol(board, candidates, c, b) == 1
             || countInBox(board, candidates, r, c, b) == 1) {
                board[r][c] = digit;
                return true;
            }
        }
        return false;
    }

    /** Backtracking fallback: seeds the masks, then runs the search. */
    private boolean solveBySearch(int[][] board) {
        int[] rowMask = new int[SIZE];
        int[] colMask = new int[SIZE];
        int[] boxMask = new int[SIZE];

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != sudoku.EMPTY) {
                    int b = bit(board[r][c]);
                    rowMask[r]           |= b;
                    colMask[c]           |= b;
                    boxMask[boxOf(r, c)] |= b;
                }
            }
        }
        return search(board, rowMask, colMask, boxMask);
    }

    /** Depth-first search, choosing the most-constrained cell first (MRV). */
    private boolean search(int[][] board, int[] rowMask, int[] colMask, int[] boxMask) {
        int bestRow = -1, bestCol = -1, bestCandidates = 0, bestCount = SIZE + 1;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != sudoku.EMPTY) continue;

                int cand = ALL & ~(rowMask[r] | colMask[c] | boxMask[boxOf(r, c)]);
                int count = Integer.bitCount(cand);
                if (count == 0) return false;              // dead end
                if (count < bestCount) {
                    bestCount = count;
                    bestRow = r;
                    bestCol = c;
                    bestCandidates = cand;
                }
            }
        }

        if (bestRow == -1) return true;                    // no empty cells left

        int box = boxOf(bestRow, bestCol);
        for (int digit = 1; digit <= SIZE; digit++) {
            int b = bit(digit);
            if ((bestCandidates & b) == 0) continue;

            board[bestRow][bestCol] = digit;
            rowMask[bestRow]        |= b;
            colMask[bestCol]        |= b;
            boxMask[box]            |= b;

            if (search(board, rowMask, colMask, boxMask)) return true;

            board[bestRow][bestCol] = sudoku.EMPTY;        // undo
            rowMask[bestRow]        &= ~b;
            colMask[bestCol]        &= ~b;
            boxMask[box]            &= ~b;
        }
        return false;
    }

    private int boxOf(int row, int col) {
        return (row / BOX) * BOX + (col / BOX);
    }

    private boolean isSolved(int[][] board) {
        for (int[] row : board)
            for (int v : row)
                if (v == sudoku.EMPTY) return false;
        return true;
    }

    private int[][] copy(int[][] board) {
        int[][] clone = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(board[r], 0, clone[r], 0, SIZE);
        }
        return clone;
    }
}
