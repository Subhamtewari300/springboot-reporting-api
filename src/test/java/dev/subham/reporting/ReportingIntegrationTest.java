package dev.subham.reporting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ReportingIntegrationTest {
 @Autowired MockMvc mvc;

 @Test void reportsAreGeneratedAndDatesValidated() throws Exception {
  mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
      .content("{\"category\":\"Office\",\"description\":\"Notebooks, blue\",\"amount\":12.50,\"occurredOn\":\"2026-10-12\"}"))
      .andExpect(status().isCreated());
  mvc.perform(get("/api/reports/summary?from=2026-10-01&to=2026-10-31"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1))
      .andExpect(jsonPath("$.byCategory[0].category").value("Office"));
  String csv=mvc.perform(get("/api/reports/export?from=2026-10-01&to=2026-10-31"))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertTrue(csv.contains("\"Notebooks, blue\""));
  mvc.perform(get("/api/reports/summary?from=2026-10-31&to=2026-10-01"))
      .andExpect(status().isBadRequest());
 }

 @Test void csvInjectionIsEscaped(){
  assertEquals("'=SUM(A1:A2)", ReportingController.escape("=SUM(A1:A2)"));
  assertEquals("\"a,\"\"b\"\"\"",ReportingController.escape("a,\"b\""));
 }
}
