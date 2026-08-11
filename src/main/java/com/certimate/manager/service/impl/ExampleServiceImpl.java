package com.certimate.manager.service.impl;

import com.certimate.manager.domain.entity.Example;
import com.certimate.manager.dto.request.ExampleRequest;
import com.certimate.manager.dto.response.ExampleResponse;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.repository.ExampleRepository;
import com.certimate.manager.service.ExampleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleServiceImpl implements ExampleService {

    private final ExampleRepository exampleRepository;

    @Override
    public List<ExampleResponse> findAll() {
        return exampleRepository.findAll().stream()
                .map(ExampleResponse::from)
                .toList();
    }

    @Override
    public ExampleResponse findById(Long id) {
        return ExampleResponse.from(getExampleOrThrow(id));
    }

    @Override
    @Transactional
    public ExampleResponse create(ExampleRequest request) {
        Example saved = exampleRepository.save(new Example(request.name()));
        return ExampleResponse.from(saved);
    }

    @Override
    @Transactional
    public ExampleResponse update(Long id, ExampleRequest request) {
        Example example = getExampleOrThrow(id);
        example.updateName(request.name());
        return ExampleResponse.from(example);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        exampleRepository.delete(getExampleOrThrow(id));
    }

    private Example getExampleOrThrow(Long id) {
        return exampleRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Example not found: " + id));
    }
}
