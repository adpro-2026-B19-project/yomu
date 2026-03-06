package id.ac.ui.cs.advprog.yomu.league.service;

import java.util.List;
import java.util.UUID;

public interface ClanService {
    ClanSummary createClan(CreateClanRequest request, UUID creatorUserId);

    List<ClanSummary> listClans();

    record CreateClanRequest(String name) {
    }

    record ClanSummary(UUID id, String name, String tier, long memberCount, UUID createdByUserId) {
    }
}
