package com.certimate.manager.exam.controller;

import com.certimate.manager.exam.dto.AiRecommendRequest;
import com.certimate.manager.exam.dto.AiRecommendResponse;
import com.certimate.manager.exam.service.AiRecommendationService;
import com.certimate.manager.user.entity.Certification;
import com.certimate.manager.user.repository.CertificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;
    private final CertificationRepository certificationRepository;

    @PostMapping("/recommend")
    public ResponseEntity<AiRecommendResponse> recommend(@RequestBody AiRecommendRequest request) {
        AiRecommendResponse response = aiRecommendationService.getRecommendation(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ensure-cert")
    public ResponseEntity<Map<String, Long>> ensureCert(@RequestBody Map<String, String> request) {
        String certName = request.get("certName");
        if (certName == null || certName.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<Certification> existing = certificationRepository.findByCertName(certName);
        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of("certId", existing.get().getId()));
        }
        
        Certification newCert = Certification.builder()
                .certName(certName)
                .agency("AI추천")
                .difficulty("기타")
                .build();
                
        Certification saved = certificationRepository.save(newCert);
        return ResponseEntity.ok(Map.of("certId", saved.getId()));
    }
}
