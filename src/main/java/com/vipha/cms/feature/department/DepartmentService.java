package com.vipha.cms.feature.department;

import com.vipha.cms.feature.department.dto.DepartmentRequest;
import com.vipha.cms.feature.department.dto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    DepartmentResponse create(DepartmentRequest request);
    DepartmentResponse update(Long id, DepartmentRequest request);
    void delete(Long id);
    DepartmentResponse get(Long id);
    List<DepartmentResponse> list();
    Department getEntity(Long id);
}
