package com.example.LAST.Controller;

import com.example.LAST.Model.sudoku;
import com.example.LAST.Service.sudokuServiceReader;
import com.example.LAST.Service.sudokuServiceSolver;
import com.example.LAST.Service.sudokuServiceWriter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class sudokuController {

    private final sudokuServiceReader reader;
    private final sudokuServiceSolver solver;
    private final sudokuServiceWriter writer;

    public sudokuController(sudokuServiceReader reader,
                            sudokuServiceSolver solver,
                            sudokuServiceWriter writer) {
        this.reader = reader;
        this.solver = solver;
        this.writer = writer;
    }


    @PostMapping(value = "/solve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> solve(@RequestParam("file") MultipartFile file) {
        sudoku puzzle = reader.read(file);
        int[][] solution = solver.solve(puzzle.getBoard());
        puzzle.setSolution(solution);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("sudokusolved.txt")
                .build());

        return new ResponseEntity<>(writer.write(solution), headers, HttpStatus.OK);
    }
}
