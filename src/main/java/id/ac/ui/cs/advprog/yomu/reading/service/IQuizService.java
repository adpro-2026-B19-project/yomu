package id.ac.ui.cs.advprog.yomu.reading.service;

import id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;

import java.util.List;
import java.util.Map;

public interface IQuizService {
    boolean hasUserAttemptedQuiz(String userId, Long textId);
    QuizAttempt getQuizResult(String userId, Long textId);
    QuizAttempt submitQuiz(Long textId, String userId, Map<String, String> formData);
    UserReadingStatResponse getUserReadingStats(String userId);
    List<QuizAttempt> getUserQuizHistory(String userId);

    List<Question> getQuestionsByTextId(Long textId);
    void addQuestion(Long textId, String questionText, String optionA, String optionB, String optionC, String optionD, String correctOption);
    Long editQuestion(Long questionId, String questionText, Long optionAId, Long optionBId, Long optionCId, Long optionDId, String optionA, String optionB, String optionC, String optionD, String correctOption);
    Long deleteQuestion(Long questionId);
}
