package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthIdentifierValidatorTest {

    private final AuthIdentifierValidator validator = new AuthIdentifierValidator();

    @Test
    void classifyShouldReturnEmailWhenIdentifierMatchesEmailPattern() {
        AuthIdentifierValidator.IdentifierType result = validator.classify("  alice@example.com  ");

        assertThat(result).isEqualTo(AuthIdentifierValidator.IdentifierType.EMAIL);
    }

    @Test
    void classifyShouldReturnUsernameWhenIdentifierMatchesUsernamePattern() {
        AuthIdentifierValidator.IdentifierType result = validator.classify("alpha.user_01-test");

        assertThat(result).isEqualTo(AuthIdentifierValidator.IdentifierType.USERNAME);
    }

    @Test
    void classifyShouldReturnInvalidWhenIdentifierDoesNotMatchSupportedPatterns() {
        assertThat(validator.classify("")).isEqualTo(AuthIdentifierValidator.IdentifierType.INVALID);
        assertThat(validator.classify("   ")).isEqualTo(AuthIdentifierValidator.IdentifierType.INVALID);
        assertThat(validator.classify("bad value")).isEqualTo(AuthIdentifierValidator.IdentifierType.INVALID);
    }

    @Test
    void isValidEmailShouldApplyExpectedRule() {
        assertThat(validator.isValidEmail("reader@example.com")).isTrue();
        assertThat(validator.isValidEmail("reader@localhost")).isFalse();
        assertThat(validator.isValidEmail("not-an-email")).isFalse();
    }

    @Test
    void isValidUsernameShouldOnlyAllowExpectedCharactersAndMustNotLookLikeEmail() {
        assertThat(validator.isValidUsername("Reader_01.test-user")).isTrue();
        assertThat(validator.isValidUsername("reader@example.com")).isFalse();
        assertThat(validator.isValidUsername("bad space")).isFalse();
        assertThat(validator.isValidUsername("bad*char")).isFalse();
    }
}
