package id.ac.ui.cs.advprog.yomu.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.AdminUserManagementService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserManagementService adminUserManagementService;

    @Test
    void usersPageShouldClampPagingInputsAndTrimKeywordModelAttribute() {
        when(adminUserManagementService.searchUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        AdminUserController controller = new AdminUserController(adminUserManagementService);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.usersPage("  reader  ", AuthRole.USER, true, -3, 99, model);

        assertThat(view).isEqualTo("admin/users");
        assertThat(model.getAttribute("selectedKeyword")).isEqualTo("reader");
        assertThat(model.getAttribute("selectedRole")).isEqualTo(AuthRole.USER);
        assertThat(model.getAttribute("selectedActive")).isEqualTo(true);
        assertThat(model.getAttribute("size")).isEqualTo(50);
        assertThat(model.getAttribute("roleOptions")).isEqualTo(List.of(AuthRole.values()));

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        org.mockito.Mockito.verify(adminUserManagementService)
                .searchUsers(org.mockito.Mockito.eq("  reader  "), org.mockito.Mockito.eq(AuthRole.USER),
                        org.mockito.Mockito.eq(true), pageRequestCaptor.capture());
        assertThat(pageRequestCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void usersPageShouldUseEmptyKeywordAndMinimumSize() {
        when(adminUserManagementService.searchUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        ConcurrentModel model = new ConcurrentModel();

        new AdminUserController(adminUserManagementService)
                .usersPage(null, null, null, 2, 0, model);

        assertThat(model.getAttribute("selectedKeyword")).isEqualTo("");
        assertThat(model.getAttribute("size")).isEqualTo(1);
    }

    @Test
    void updateUserStatusShouldRequireConfirmationBeforeDeactivation() {
        AdminUserController controller = new AdminUserController(adminUserManagementService);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String redirect = controller.updateUserStatus(
                UUID.randomUUID(),
                false,
                " wrong ",
                "  reader  ",
                AuthRole.USER,
                true,
                -1,
                99,
                redirectAttributes
        );

        assertThat(redirect).isEqualTo("redirect:/admin/users?page=0&size=50&keyword=reader&role=USER&active=true");
        assertThat(redirectAttributes.getFlashAttributes().get("warning"))
                .isEqualTo("Type DEACTIVATE to confirm account deactivation.");
    }

    @Test
    void updateUserStatusShouldFlashSuccessWhenUserWasUpdated() {
        UUID userId = UUID.randomUUID();
        when(adminUserManagementService.updateUserStatus(userId, true)).thenReturn(true);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String redirect = new AdminUserController(adminUserManagementService).updateUserStatus(
                userId,
                true,
                null,
                " ",
                null,
                null,
                0,
                10,
                redirectAttributes
        );

        assertThat(redirect).isEqualTo("redirect:/admin/users?page=0&size=10");
        assertThat(redirectAttributes.getFlashAttributes().get("success")).isEqualTo("Account status updated.");
    }

    @Test
    void updateUserStatusShouldFlashWarningWhenUserWasNotFound() {
        UUID userId = UUID.randomUUID();
        when(adminUserManagementService.updateUserStatus(userId, false)).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String redirect = new AdminUserController(adminUserManagementService).updateUserStatus(
                userId,
                false,
                " DEACTIVATE ",
                null,
                null,
                false,
                1,
                5,
                redirectAttributes
        );

        assertThat(redirect).isEqualTo("redirect:/admin/users?page=1&size=5&active=false");
        assertThat(redirectAttributes.getFlashAttributes().get("warning")).isEqualTo("User account not found.");
    }

    @Test
    void usersPageShouldExposeReturnedPage() {
        AuthUser reader = new AuthUser("reader", "reader@example.com", null, "Reader", "hashed");
        PageImpl<AuthUser> page = new PageImpl<>(List.of(reader));
        when(adminUserManagementService.searchUsers(any(), any(), any(), any())).thenReturn(page);
        ConcurrentModel model = new ConcurrentModel();

        new AdminUserController(adminUserManagementService).usersPage("", null, null, 0, 10, model);

        assertThat(model.getAttribute("usersPage")).isSameAs(page);
    }
}
