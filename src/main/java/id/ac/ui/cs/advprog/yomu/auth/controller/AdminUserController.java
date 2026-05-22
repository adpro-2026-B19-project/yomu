package id.ac.ui.cs.advprog.yomu.auth.controller;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.AdminUserManagementService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public String usersPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AuthRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        long start = System.nanoTime();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<AuthUser> users = adminUserManagementService.searchUsers(keyword, role, active, pageable);
        model.addAttribute("usersPage", users);
        model.addAttribute("roleOptions", List.of(AuthRole.values()));
        model.addAttribute("selectedKeyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedActive", active);
        model.addAttribute("size", safeSize);
        log.info("GET /admin/users controller preparation took {} ms", elapsedMs(start));
        return "admin/users";
    }

    @PostMapping("/{userId}/status")
    public String updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam boolean active,
            @RequestParam(required = false) String confirmation,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AuthRole role,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes
    ) {
        if (!active && (confirmation == null || !"DEACTIVATE".equals(confirmation.trim()))) {
            redirectAttributes.addFlashAttribute("warning", "Type DEACTIVATE to confirm account deactivation.");
            return "redirect:/admin/users" + buildQuery(keyword, role, status, page, size);
        }

        boolean updated = adminUserManagementService.updateUserStatus(userId, active);
        if (updated) {
            redirectAttributes.addFlashAttribute("success", "Account status updated.");
        } else {
            redirectAttributes.addFlashAttribute("warning", "User account not found.");
        }

        return "redirect:/admin/users" + buildQuery(keyword, role, status, page, size);
    }

    private String buildQuery(String keyword, AuthRole role, Boolean status, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .queryParam("page", Math.max(page, 0))
                .queryParam("size", Math.min(Math.max(size, 1), 50));
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.queryParam("keyword", keyword.trim());
        }
        if (role != null) {
            builder.queryParam("role", role.name());
        }
        if (status != null) {
            builder.queryParam("active", status);
        }
        String query = builder.build().encode().toUriString();
        if (query.isEmpty()) {
            return "";
        }
        return query.startsWith("?") ? query : "?" + query;
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
