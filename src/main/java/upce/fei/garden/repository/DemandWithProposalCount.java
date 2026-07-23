package upce.fei.garden.repository;

import upce.fei.garden.model.enums.DemandStatus;

/**
 * Projekce poptávky s počtem přijatých návrhů.
 */
public interface DemandWithProposalCount {
    Long getDemandId();
    String getTitle();
    DemandStatus getStatus();
    Long getProposalCount();
}
