package com.coworking.booking.controller;

import com.coworking.booking.annotation.CreateEndpoint;
import com.coworking.booking.annotation.TenantId;
import com.coworking.booking.dto.WorkspaceDto;
import com.coworking.booking.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceRestController {

    private final WorkspaceService workspaceService;

    public WorkspaceRestController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    // Acceptance criteria 8 (THEN): Застосування кастомної анотації для резолвера аргументів.
    // Spring бачить @TenantId, викликає наш TenantIdArgumentResolver, витягує значення із заголовка "X-Tenant-ID"
    // та інжектить його прямо у змінну tenantId. Це повністю позбавляє від необхідності працювати з сирим HttpServletRequest.
    public List<WorkspaceDto> findAll(@TenantId String tenantId) {
        // Демонстрація того, що значення використовується в тілі методу (Acceptance criteria 8)
        System.out.println("Отримано запит для орендаря (tenant): " + tenantId);
        return workspaceService.findAll();
    }

    @GetMapping("/{id}")
    public WorkspaceDto findById(@PathVariable Long id) {
        return workspaceService.findById(id);
    }

    // Acceptance criteria 5 (THEN): Використання нашої композитної мета-анотації (Composed Annotation).
    // Замість дублювання стандартних @PostMapping та @ResponseStatus(HttpStatus.CREATED) у кожному контролері,
    // ми ставимо одну мітку @CreateEndpoint. Spring розгортає її під капотом, тому цей метод поводиться
    // абсолютно так само, як звичайний POST-ендпоінт.
    @CreateEndpoint
    public WorkspaceDto create(@Valid @RequestBody WorkspaceDto dto) {
        return workspaceService.create(dto);
    }

    @PutMapping("/{id}")
    public WorkspaceDto update(@PathVariable Long id, @Valid @RequestBody WorkspaceDto dto) {
        return workspaceService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workspaceService.delete(id);
    }
}