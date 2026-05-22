package id.ac.ui.cs.advprog.yomu.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.dto.RegisterForm;
import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;
import id.ac.ui.cs.advprog.yomu.auth.service.AuthService;
import id.ac.ui.cs.advprog.yomu.auth.service.RegisterAttemptService;
import id.ac.ui.cs.advprog.yomu.auth.service.UsernameSuggestionGenerator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UsernameSuggestionGenerator usernameSuggestionGenerator;

    @Mock
    private RegisterAttemptService registerAttemptService;

    @Mock
    private ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    @Mock
    private HttpServletRequest request;

    private final RegistrationErrorFieldMapper registrationErrorFieldMapper = new RegistrationErrorFieldMapper();

    @Test
    void authPageShouldRedirectToLogin() {
        assertThat(controller().authPage()).isEqualTo("redirect:/auth/login");
    }

    @Test
    void registerPageShouldKeepExistingModelAttributesAndResolveOAuthProviders() {
        Model model = new ConcurrentModel();
        model.addAttribute("form", new RegisterForm("saved@example.com", "saved", ""));
        model.addAttribute("suggestedUsername", "saved");
        when(clientRegistrationRepositoryProvider.getIfAvailable())
                .thenReturn(new IterableRegistrationRepository(List.of(googleRegistration(), "not-a-registration")));

        String view = controller().registerPage(model);

        assertThat(view).isEqualTo("auth/register");
        assertThat(model.getAttribute("form")).isInstanceOf(RegisterForm.class);
        assertThat(model.getAttribute("suggestedUsername")).isEqualTo("saved");
        assertThat(model.getAttribute("oauthProviders")).isEqualTo(List.of("google"));
        verify(usernameSuggestionGenerator, never()).generateSuggestion();
    }

    @Test
    void loginPageShouldUseEmptyOAuthProvidersWhenRepositoryIsNotIterable() {
        Model model = new ConcurrentModel();
        when(clientRegistrationRepositoryProvider.getIfAvailable()).thenReturn(registrationId -> null);

        String view = controller().loginPage(model);

        assertThat(view).isEqualTo("auth/login");
        assertThat(model.getAttribute("loginForm")).isNotNull();
        assertThat(model.getAttribute("registeredName")).isEqualTo("");
        assertThat(model.getAttribute("registeredEmail")).isEqualTo("");
        assertThat(model.getAttribute("oauthProviders")).isEqualTo(List.of());
    }

    @Test
    void registerShouldShortCircuitWhenRateLimited() {
        RegisterForm form = new RegisterForm("reader@example.com", "reader", "SecretPass1!");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(registerAttemptService.isLimited(request)).thenReturn(true);

        String redirect = controller().register(
                form,
                new BeanPropertyBindingResult(form, "form"),
                request,
                redirectAttributes
        );

        assertThat(redirect).isEqualTo("redirect:/auth/register");
        assertThat(redirectAttributes.getFlashAttributes().get("warning"))
                .isEqualTo("Unable to process registration right now. Please try again later.");
        RegisterForm preserved = (RegisterForm) redirectAttributes.getFlashAttributes().get("form");
        assertThat(preserved.getEmail()).isEqualTo("reader@example.com");
        assertThat(preserved.getUsername()).isEqualTo("reader");
        assertThat(preserved.getPassword()).isEmpty();
        verify(registerAttemptService, never()).recordAttempt(request);
        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerShouldRedirectBackWhenBindingAlreadyHasErrors() {
        RegisterForm form = new RegisterForm("bad-email", "", "");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("email", "invalid_email", "Email is invalid");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(registerAttemptService.isLimited(request)).thenReturn(false);

        String redirect = controller().register(form, bindingResult, request, redirectAttributes);

        assertThat(redirect).isEqualTo("redirect:/auth/register");
        assertThat(redirectAttributes.getFlashAttributes().get("form")).isSameAs(form);
        verify(registerAttemptService).recordAttempt(request);
        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerShouldMapServiceErrorToBindingResultAndWarning() {
        RegisterForm form = new RegisterForm("taken@example.com", "taken", "SecretPass1!");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(registerAttemptService.isLimited(request)).thenReturn(false);
        when(authService.registerUser(any())).thenReturn(AuthService.RegistrationResult.failureResult(
                "duplicate_username",
                "Username is already taken",
                PasswordStrength.STRONG
        ));

        String redirect = controller().register(form, bindingResult, request, redirectAttributes);

        assertThat(redirect).isEqualTo("redirect:/auth/register");
        assertThat(bindingResult.hasFieldErrors("username")).isTrue();
        assertThat(redirectAttributes.getFlashAttributes().get("warning")).isEqualTo("Username is already taken");
    }

    @Test
    void registerShouldRedirectToLoginWithRegisteredFlashAttributesOnSuccess() {
        RegisterForm form = new RegisterForm("reader@example.com", "reader", "SecretPass1!");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(registerAttemptService.isLimited(request)).thenReturn(false);
        when(authService.registerUser(any())).thenReturn(AuthService.RegistrationResult.successResult(
                new AuthService.RegisteredUserSummary("reader", "reader@example.com"),
                PasswordStrength.STRONG
        ));

        String redirect = controller().register(
                form,
                new BeanPropertyBindingResult(form, "form"),
                request,
                redirectAttributes
        );

        assertThat(redirect).isEqualTo("redirect:/auth/login");
        assertThat(redirectAttributes.getFlashAttributes().get("registeredName")).isEqualTo("reader");
        assertThat(redirectAttributes.getFlashAttributes().get("registeredEmail")).isEqualTo("reader@example.com");
    }

    private AuthController controller() {
        return new AuthController(
                authService,
                registrationErrorFieldMapper,
                usernameSuggestionGenerator,
                registerAttemptService,
                clientRegistrationRepositoryProvider
        );
    }

    private ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://example.com/oauth/authorize")
                .tokenUri("https://example.com/oauth/token")
                .userInfoUri("https://example.com/oauth/userinfo")
                .userNameAttributeName("sub")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .build();
    }

    private record IterableRegistrationRepository(List<Object> registrations)
            implements ClientRegistrationRepository, Iterable<Object> {

        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return registrations.stream()
                    .filter(ClientRegistration.class::isInstance)
                    .map(ClientRegistration.class::cast)
                    .filter(registration -> registration.getRegistrationId().equals(registrationId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Iterator<Object> iterator() {
            return registrations.iterator();
        }
    }
}
