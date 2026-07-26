package upce.fei.garden.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrační testy {@link DemandController} přes {@link MockMvc} nad reálným Spring kontextem
 * a in-memory H2 databází (profil {@code test}, viz {@code application-test.properties}).
 * <p>
 * Autorizace se v testech neobchází – uživatelé se registrují přes {@code POST /api/auth/register}
 * a v požadavcích se posílá skutečný JWT vrácený tímto endpointem, stejně jako by to dělal reálný klient.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemandControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // vlastni instance misto @Autowired - pouziva se jen na cteni jednoduchych poli z JSON
    // odpovedi (id, token), nezavisi tedy na tom, jak je v kontextu nakonfigurovany Jackson
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String emailPrefix, String role) throws Exception {
        String email = emailPrefix + "-" + UUID.randomUUID() + "@example.com";
        String body = """
                {
                  "firstName": "Test",
                  "lastName": "User",
                  "role": "%s",
                  "email": "%s",
                  "password": "TestPass123"
                }
                """.formatted(role, email);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private long createGarden(String ownerToken) throws Exception {
        String body = """
                {
                  "gardenName": "Testovací zahrada",
                  "areaSqm": 100,
                  "city": "Pardubice",
                  "street": "Testovací",
                  "houseNumber": "1",
                  "postalCode": "53003"
                }
                """;

        String response = mockMvc.perform(post("/api/gardens")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long firstServiceTypeId() throws Exception {
        String response = mockMvc.perform(get("/api/service-types"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get(0).get("id").asLong();
    }

    private String demandRequestJson(long serviceTypeId, String desiredDate) {
        return """
                {
                  "title": "Poptávka",
                  "serviceTypeIds": [%d],
                  "description": "Popis",
                  "desiredDate": "%s"
                }
                """.formatted(serviceTypeId, desiredDate);
    }

    @Test
    void createDemand_asOwner_returns201() throws Exception {
        String ownerToken = registerAndGetToken("owner-create", "OWNER");
        long gardenId = createGarden(ownerToken);
        long serviceTypeId = firstServiceTypeId();

        mockMvc.perform(post("/api/gardens/{gardenId}/demands", gardenId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandRequestJson(serviceTypeId, LocalDate.now().plusDays(5).toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Poptávka"))
                .andExpect(jsonPath("$.status").value("NOVA"));
    }

    @Test
    void createDemand_asWorker_returns403() throws Exception {
        String workerToken = registerAndGetToken("worker-create", "WORKER");

        mockMvc.perform(post("/api/gardens/{gardenId}/demands", 1L)
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandRequestJson(1L, LocalDate.now().plusDays(5).toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCatalog_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/demands/catalog"))
                .andExpect(status().isOk());
    }

    @Test
    void updateDemand_withExistingProposal_returns409() throws Exception {
        String ownerToken = registerAndGetToken("owner-update", "OWNER");
        long gardenId = createGarden(ownerToken);
        long serviceTypeId = firstServiceTypeId();

        String createResponse = mockMvc.perform(post("/api/gardens/{gardenId}/demands", gardenId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandRequestJson(serviceTypeId, LocalDate.now().plusDays(5).toString())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long demandId = objectMapper.readTree(createResponse).get("id").asLong();

        String workerToken = registerAndGetToken("worker-update", "WORKER");
        mockMvc.perform(post("/api/demands/{demandId}/proposals", demandId)
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": 1000, "description": "Nabídka"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/demands/{demandId}", demandId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandRequestJson(serviceTypeId, LocalDate.now().plusDays(10).toString())))
                .andExpect(status().isConflict());
    }

    @Test
    void createDemand_withPastDate_returns400WithFieldErrors() throws Exception {
        String ownerToken = registerAndGetToken("owner-pastdate", "OWNER");
        long gardenId = createGarden(ownerToken);
        long serviceTypeId = firstServiceTypeId();

        mockMvc.perform(post("/api/gardens/{gardenId}/demands", gardenId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandRequestJson(serviceTypeId, LocalDate.now().minusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.desiredDate").exists());
    }
}
