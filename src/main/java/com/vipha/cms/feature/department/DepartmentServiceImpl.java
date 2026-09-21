package com.vipha.cms.feature.department;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.department.dto.DepartmentRequest;
import com.vipha.cms.feature.department.dto.DepartmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new BusinessRuleException("Department name already exists");
        }
        Department entity = new Department();
        entity.setName(request.name());
        entity.setDescription(request.description());
        return toResponse(departmentRepository.save(entity));
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department entity = getEntity(id);
        if (!Objects.equals(entity.getName(), request.name())
                && departmentRepository.existsByName(request.name())) {
            throw new BusinessRuleException("Department name already exists");
        }
        entity.setName(request.name());
        entity.setDescription(request.description());
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department entity = getEntity(id);
        departmentRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public Department getEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription());
    }
}
