package com.certimate.manager.service;

import com.certimate.manager.dto.request.ExampleRequest;
import com.certimate.manager.dto.response.ExampleResponse;
import java.util.List;

public interface ExampleService {

    List<ExampleResponse> findAll();

    ExampleResponse findById(Long id);

    ExampleResponse create(ExampleRequest request);

    ExampleResponse update(Long id, ExampleRequest request);

    void delete(Long id);
}
