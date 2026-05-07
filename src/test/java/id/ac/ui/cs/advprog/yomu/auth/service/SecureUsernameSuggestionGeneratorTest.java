package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecureUsernameSuggestionGeneratorTest {

    private final SecureUsernameSuggestionGenerator generator = new SecureUsernameSuggestionGenerator();

    @Test
    void generateSuggestionShouldUseReaderPrefixAndHexSuffix() {
        String suggestion = generator.generateSuggestion();

        assertThat(suggestion).matches("^reader-[0-9a-f]{12}$");
    }

    @Test
    void generateSuggestionShouldProduceNonRepeatingValuesAcrossSamples() {
        Set<String> suggestions = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            suggestions.add(generator.generateSuggestion());
        }

        assertThat(suggestions).hasSize(100);
    }
}
