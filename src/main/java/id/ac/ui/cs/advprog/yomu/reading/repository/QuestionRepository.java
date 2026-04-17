package id.ac.ui.cs.advprog.yomu.reading.repository;

import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTextId(Long textId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Question q WHERE q.text.id = :textId")
    void deleteByTextId(@Param("textId") Long textId);
}