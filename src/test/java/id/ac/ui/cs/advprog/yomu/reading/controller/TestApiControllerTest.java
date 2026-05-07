package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse;
import id.ac.ui.cs.advprog.yomu.reading.service.TextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextApiControllerTest {

    @Mock
    private TextService textService;

    @InjectMocks
    private TextApiController textApiController;

    @Test
    void getUserStatsShouldReturnOkResponseWithReadingStats() {
        UserReadingStatResponse response = new UserReadingStatResponse(3, 0.75, 225.0);
        when(textService.getUserReadingStats("user-123")).thenReturn(response);

        ResponseEntity<UserReadingStatResponse> result = textApiController.getUserStats("user-123");

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTotalTextsCompleted()).isEqualTo(3);
        assertThat(result.getBody().getAverageAccuracy()).isEqualTo(0.75);
        assertThat(result.getBody().getTotalAccumulatedScore()).isEqualTo(225.0);

        verify(textService).getUserReadingStats("user-123");
    }
}