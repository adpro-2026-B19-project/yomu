package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2LoginUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserIdentityExtractor identityExtractor;
    private final OAuth2UserProvisioningService provisioningService;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Autowired
    public OAuth2LoginUserService(
            OAuth2UserIdentityExtractor identityExtractor,
            OAuth2UserProvisioningService provisioningService
    ) {
        this(identityExtractor, provisioningService, new DefaultOAuth2UserService());
    }

    OAuth2LoginUserService(
            OAuth2UserIdentityExtractor identityExtractor,
            OAuth2UserProvisioningService provisioningService,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate
    ) {
        this.identityExtractor = identityExtractor;
        this.provisioningService = provisioningService;
        this.delegate = delegate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        OAuth2UserIdentity identity = identityExtractor.extract(userRequest, oauth2User);
        AuthUser authUser = provisioningService.loadOrCreateUser(identity);

        return new AuthenticatedOAuth2UserPrincipal(
                authUser.getUsername(),
                authUser.getEmail(),
                oauth2User.getAttributes(),
                List.of(new SimpleGrantedAuthority("ROLE_" + authUser.getRole().name()))
        );
    }
}
