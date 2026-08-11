package com.certimate.manager.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.dto.request.ExampleRequest;
import com.certimate.manager.dto.response.ExampleResponse;
import com.certimate.manager.service.ExampleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @GetMapping
    public ApiResponse<List<ExampleResponse>> findAll() {
        return ApiResponse.success(exampleService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ExampleResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(exampleService.findById(id));
    }

    @PostMapping
    public ApiResponse<ExampleResponse> create(@Valid @RequestBody ExampleRequest request) {
        return ApiResponse.success(exampleService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExampleResponse> update(@PathVariable Long id, @Valid @RequestBody ExampleRequest request) {
        return ApiResponse.success(exampleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        exampleService.delete(id);
        return ApiResponse.success(null);
    }
}
