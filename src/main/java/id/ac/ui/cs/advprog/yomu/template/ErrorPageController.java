package id.ac.ui.cs.advprog.yomu.template;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPageController {

    @GetMapping("/error")
    public String errorPage(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        model.addAttribute("statusCode", statusCode == null ? "Unknown" : statusCode.toString());
        model.addAttribute("errorMessage", message == null ? "Unexpected error" : message.toString());
        model.addAttribute("errorPath", path == null ? "" : path.toString());
        return "index/error";
    }
}
