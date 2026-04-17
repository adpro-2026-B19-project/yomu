package id.ac.ui.cs.advprog.yomu.template;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

class ErrorPageControllerTest {

    private final ErrorPageController controller = new ErrorPageController();

    @Test
    void errorPageShouldUseFallbackValuesWhenAttributesMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.errorPage(request, model);

        assertThat(view).isEqualTo("error/index");
        assertThat(model.getAttribute("statusCode")).isEqualTo("Unknown");
        assertThat(model.getAttribute("status")).isEqualTo("Unknown");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Unexpected error");
        assertThat(model.getAttribute("errorPath")).isEqualTo("");
    }

    @Test
    void errorPageShouldMapRequestAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Not Found");
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/missing");
        ExtendedModelMap model = new ExtendedModelMap();

        controller.errorPage(request, model);

        assertThat(model.getAttribute("statusCode")).isEqualTo("404");
        assertThat(model.getAttribute("status")).isEqualTo("404");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Not Found");
        assertThat(model.getAttribute("errorPath")).isEqualTo("/missing");
    }
}
