package com.coworking.booking.controller;

// Правильний шлях до твого сервісу!
import com.coworking.booking.WorkspaceService;
import com.coworking.booking.dto.WorkspaceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkspaceRestController.class)
class CreateEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkspaceService workspaceService;

    @SuppressWarnings("unused")
    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @WithMockUser
    void whenUsingComposedAnnotation_thenActsAsPostAndReturnsCreatedStatus() throws Exception {
        // GIVEN
        WorkspaceDto dto = new WorkspaceDto();
        dto.setName("Test Workspace");
        dto.setType("Meeting Room");
        dto.setCapacity(10);
        dto.setHourlyRate(BigDecimal.valueOf(50.0));

        // Викликаємо правильний метод create
        Mockito.when(workspaceService.create(Mockito.any(WorkspaceDto.class))).thenReturn(dto);

        // WHEN / THEN
        mockMvc.perform(post("/api/workspaces")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }
}