package upce.fei.garden.service;

import upce.fei.garden.dto.dashboard.GardenSummary;
import upce.fei.garden.dto.workreport.CreateWorkReport;
import upce.fei.garden.dto.workreport.WorkReportResponse;
import upce.fei.garden.dto.workreport.WorkerJobSummary;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.WorkReport;
import upce.fei.garden.model.Worker;
import upce.fei.garden.repository.WorkerJobProjection;

/**
 * Převod mezi entitou {@link WorkReport} (a projekcí {@link WorkerJobProjection}) a jejich DTO.
 * Držen mimo {@link WorkReportService}, aby servisní vrstva obsahovala pouze business logiku.
 */
final class WorkReportMapper {

    private WorkReportMapper() {
    }

    static WorkReport toEntity(CreateWorkReport request, Demand demand, Worker worker) {
        WorkReport report = new WorkReport();
        report.setDemand(demand);
        report.setWorker(worker);
        report.setDescription(request.getDescription());
        report.setPhotoUrls(request.getPhotoUrls());
        return report;
    }

    static WorkReportResponse toResponse(WorkReport report) {
        if (report == null) {
            return null;
        }
        return new WorkReportResponse(
                report.getId(),
                report.getDescription(),
                report.getPhotoUrls(),
                report.getCreatedAt());
    }

    static WorkerJobSummary toJobSummary(WorkerJobProjection projection) {
        GardenSummary garden = new GardenSummary(
                projection.getGardenName(),
                projection.getCity(),
                projection.getStreet(),
                projection.getHouseNumber(),
                projection.getMainPhotoUrl());
        return new WorkerJobSummary(
                projection.getDemandId(),
                projection.getTitle(),
                projection.getDescription(),
                garden,
                projection.getStatus(),
                projection.getPrice(),
                projection.getCreatedAt(),
                Boolean.TRUE.equals(projection.getReportSubmitted()));
    }
}
