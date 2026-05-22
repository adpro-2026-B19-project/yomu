package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressResolverTest {

    private final ClientAddressResolver resolver = new ClientAddressResolver();

    @Test
    void resolveShouldReturnUnknownForNullRequest() {
        assertThat(resolver.resolve(null)).isEqualTo("unknown");
    }

    @Test
    void resolveShouldUseFirstForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " 203.0.113.10 , 198.51.100.20 ");
        request.setRemoteAddr("192.0.2.30");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void resolveShouldFallBackToTrimmedRemoteAddressWhenForwardedHeaderIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "   ");
        request.setRemoteAddr(" 192.0.2.44 ");

        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.44");
    }

    @Test
    void resolveShouldReturnUnknownWhenForwardedAndRemoteAddressAreEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " , ");
        request.setRemoteAddr("   ");

        assertThat(resolver.resolve(request)).isEqualTo("unknown");
    }
}
