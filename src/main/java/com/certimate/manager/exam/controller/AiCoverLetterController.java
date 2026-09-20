package com.certimate.manager.exam.controller;

import com.certimate.manager.exam.dto.AiCoverLetterRequest;
import com.certimate.manager.exam.dto.AiCoverLetterResponse;
import com.certimate.manager.exam.service.AiCoverLetterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiCoverLetterController {

    private final AiCoverLetterService aiCoverLetterService;

    public AiCoverLetterController(AiCoverLetterService aiCoverLetterService) {
        this.aiCoverLetterService = aiCoverLetterService;
    }

    @PostMapping("/cover-letter")
    public ResponseEntity<AiCoverLetterResponse> generateCoverLetter(@RequestBody AiCoverLetterRequest request) {
        AiCoverLetterResponse response = aiCoverLetterService.generateCoverLetter(request);
        return ResponseEntity.ok(response);
    }
}
