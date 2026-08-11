package upce.fei.garden.dto.workreport;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Report o dokončených pracích odeslaný zahradníkem k poptávce.
 */
@Schema(description = "Report o provedených pracích - popis a fotografie výsledku")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkReportResponse {
    private Long id;
    private String description;
    private List<String> photoUrls;
    private LocalDateTime createdAt;
}
