package id.ac.ui.cs.advprog.yomu.league.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMemberRole;
import id.ac.ui.cs.advprog.yomu.league.model.Tier;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanMemberRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.TierRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClanServiceImplTest {

    @Mock
    private ClanRepository clanRepository;

    @Mock
    private ClanMemberRepository clanMemberRepository;

    @Mock
    private TierRepository tierRepository;

    @InjectMocks
    private ClanServiceImpl clanService;

    @Test
    void createClanShouldPersistClanAndLeaderMembership() {
        UUID creatorUserId = UUID.randomUUID();
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");

        when(clanRepository.existsByNameIgnoreCase("Code Masters")).thenReturn(false);
        when(tierRepository.findByCode(TierCode.BRONZE)).thenReturn(Optional.of(bronze));
        when(clanRepository.save(any(Clan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clanMemberRepository.save(any(ClanMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClanService.ClanSummary created = clanService.createClan(
                new ClanService.CreateClanRequest("  Code Masters  "),
                creatorUserId
        );

        ArgumentCaptor<Clan> clanCaptor = ArgumentCaptor.forClass(Clan.class);
        verify(clanRepository).save(clanCaptor.capture());
        assertThat(clanCaptor.getValue().getName()).isEqualTo("Code Masters");
        assertThat(clanCaptor.getValue().getCreatedByUserId()).isEqualTo(creatorUserId);
        assertThat(clanCaptor.getValue().getTier().getCode()).isEqualTo(TierCode.BRONZE);

        ArgumentCaptor<ClanMember> memberCaptor = ArgumentCaptor.forClass(ClanMember.class);
        verify(clanMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(ClanMemberRole.LEADER);
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(creatorUserId);

        assertThat(created.name()).isEqualTo("Code Masters");
        assertThat(created.tier()).isEqualTo("BRONZE");
        assertThat(created.memberCount()).isEqualTo(1);
        assertThat(created.createdByUserId()).isEqualTo(creatorUserId);
    }

    @Test
    void createClanShouldFailWhenNameAlreadyExists() {
        when(clanRepository.existsByNameIgnoreCase("Code Masters")).thenReturn(true);

        assertThatThrownBy(() -> clanService.createClan(
                new ClanService.CreateClanRequest("Code Masters"),
                UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Clan name already exists");

        verify(clanRepository, never()).save(any(Clan.class));
        verify(clanMemberRepository, never()).save(any(ClanMember.class));
    }

    @Test
    void listClansShouldMapEntityToSummary() {
        UUID creatorUserId = UUID.randomUUID();
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan clan = new Clan("Bronze Squad", bronze, creatorUserId);
        clan.addMember(new ClanMember(clan, creatorUserId, ClanMemberRole.LEADER));
        clan.addMember(new ClanMember(clan, UUID.randomUUID(), ClanMemberRole.MEMBER));

        when(clanRepository.findAllForListing()).thenReturn(List.of(clan));

        List<ClanService.ClanSummary> summaries = clanService.listClans();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().name()).isEqualTo("Bronze Squad");
        assertThat(summaries.getFirst().tier()).isEqualTo("BRONZE");
        assertThat(summaries.getFirst().memberCount()).isEqualTo(2);
        assertThat(summaries.getFirst().createdByUserId()).isEqualTo(creatorUserId);
    }
}
