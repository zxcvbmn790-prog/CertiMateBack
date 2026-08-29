package com.certimate.service;

import com.certimate.domain.ExamSchedule;
import com.certimate.repository.ExamScheduleRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL exam_schedule 테이블에서 시험 일정 데이터를 실시간 조회하여
 * JSON 형태의 REST API로 제공하는 서비스 및 컨트롤러 클래스.
 */
@Slf4j
@RestController
@RequestMapping({"/api/qnet-info", "/api/exam-schedule"})
@CrossOrigin(origins = "*")
public class qnet_info_to_json {

    private final ExamScheduleRepository examScheduleRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public qnet_info_to_json(ExamScheduleRepository examScheduleRepository,
                            @Autowired(required = false) ObjectMapper objectMapper) {
        this.examScheduleRepository = examScheduleRepository;
        if (objectMapper != null) {
            this.objectMapper = objectMapper;
        } else {
            this.objectMapper = new ObjectMapper();
            this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }

    /**
     * 시험 일정 정보 DTO (JSON 직렬화/역직렬화 지원)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExamScheduleDto {
        private Integer id;

        @JsonProperty("qual_name")
        private String qualName;

        @JsonProperty("exam_round")
        private String examRound;

        @JsonProperty("exam_date")
        private String examDate;

        /**
         * JPA Entity -> DTO 변환 메서드
         */
        public static ExamScheduleDto fromEntity(ExamSchedule entity) {
            if (entity == null) {
                return null;
            }
            return ExamScheduleDto.builder()
                    .id(entity.getId())
                    .qualName(entity.getQualName())
                    .examRound(entity.getExamRound())
                    .examDate(entity.getExamDate())
                    .build();
        }

        /**
         * DTO -> JPA Entity 변환 메서드
         */
        public ExamSchedule toEntity() {
            return ExamSchedule.builder()
                    .id(this.id)
                    .qualName(this.qualName)
                    .examRound(this.examRound)
                    .examDate(this.examDate)
                    .build();
        }
    }

    /**
     * 하위 호환성을 위한 QnetExamInfoDto 별칭 (ExamScheduleDto와 동일 구조)
     */
    public static class QnetExamInfoDto extends ExamScheduleDto {
        public QnetExamInfoDto() {
            super();
        }
    }

    /**
     * DB(exam_schedule 테이블)에서 전체 시험 일정 정보를 실시간 조회하여 DTO 목록으로 반환합니다.
     */
    public List<ExamScheduleDto> getExamInfoListFromDb() {
        List<ExamSchedule> entityList = examScheduleRepository.findAll();
        return entityList.stream()
                .map(ExamScheduleDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 자격증 명(qualName)으로 DB 실시간 조회
     */
    public List<ExamScheduleDto> getExamInfoListByQualName(String qualName) {
        List<ExamSchedule> entityList = examScheduleRepository.findByQualNameContaining(qualName);
        return entityList.stream()
                .map(ExamScheduleDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 키워드(자격증명, 회차, 시험일자) 통합 검색
     */
    public List<ExamScheduleDto> searchExamInfo(String keyword) {
        List<ExamSchedule> entityList = examScheduleRepository.searchByKeyword(keyword);
        return entityList.stream()
                .map(ExamScheduleDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * DTO 목록을 JSON 문자열로 변환 (Pretty Print)
     */
    public String convertToJson(List<ExamScheduleDto> list) throws JsonProcessingException {
        return objectMapper.writeValueAsString(list);
    }

    /**
     * DB 실시간 데이터를 Formatted JSON 문자열로 반환
     */
    public String getExamInfoJson() throws JsonProcessingException {
        return convertToJson(getExamInfoListFromDb());
    }

    // =========================================================================
    // REST API Endpoints
    // =========================================================================

    /**
     * [API 1] 전체 시험 일정 정보 실시간 목록 조회 (JSON 응답)
     * GET /api/qnet-info
     * GET /api/qnet-info?qualName=정보처리기사
     * GET /api/exam-schedule
     * GET /api/exam-schedule?qualName=정보처리기사
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ExamScheduleDto>> getExamScheduleApi(
            @RequestParam(name = "qualName", required = false) String qualName) {
        List<ExamScheduleDto> resultList;
        if (qualName != null && !qualName.trim().isEmpty()) {
            resultList = getExamInfoListByQualName(qualName.trim());
        } else {
            resultList = getExamInfoListFromDb();
        }
        return ResponseEntity.ok(resultList);
    }

    /**
     * [API 2] ID 기반 시험 일정 정보 단건 조회
     * GET /api/qnet-info/{id}
     * GET /api/exam-schedule/{id}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getExamScheduleByIdApi(@PathVariable("id") Integer id) {
        return examScheduleRepository.findById(id)
                .map(entity -> ResponseEntity.ok(ExamScheduleDto.fromEntity(entity)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * [API 3] 키워드 통합 검색 API (자격증명, 시험회차, 일자)
     * GET /api/qnet-info/search?keyword=기사
     * GET /api/exam-schedule/search?keyword=기사
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ExamScheduleDto>> searchExamScheduleApi(
            @RequestParam("keyword") String keyword) {
        List<ExamScheduleDto> resultList = searchExamInfo(keyword.trim());
        return ResponseEntity.ok(resultList);
    }

    /**
     * [API 4] Formatted (Pretty-printed) JSON 문자열 반환 API
     * GET /api/qnet-info/json
     * GET /api/exam-schedule/json
     */
    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getExamScheduleJsonApi() {
        try {
            String jsonString = getExamInfoJson();
            return ResponseEntity.ok(jsonString);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"JSON 변환 실패: " + e.getMessage() + "\"}");
        }
    }

    /**
     * [API 5] 신규 시험 일정 등록 API
     * POST /api/qnet-info
     * POST /api/exam-schedule
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> createExamSchedule(@RequestBody ExamScheduleDto requestDto) {
        try {
            ExamSchedule entity = requestDto.toEntity();
            ExamSchedule saved = examScheduleRepository.save(entity);
            return ResponseEntity.ok(ExamScheduleDto.fromEntity(saved));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("등록 실패: " + e.getMessage());
        }
    }
}
