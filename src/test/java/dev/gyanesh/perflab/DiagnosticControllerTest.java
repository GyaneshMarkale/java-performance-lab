package dev.gyanesh.perflab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiagnosticControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void normalEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/normal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void allocationEndpointCapsRequestedAmount() throws Exception {
        mockMvc.perform(get("/api/allocation").param("mb", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryMiBAllocatedApprox").value(64));
    }
}
