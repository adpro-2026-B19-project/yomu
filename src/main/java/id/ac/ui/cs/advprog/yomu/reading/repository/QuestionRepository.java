package id.ac.ui.cs.advprog.yomu.reading.repository;

import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}