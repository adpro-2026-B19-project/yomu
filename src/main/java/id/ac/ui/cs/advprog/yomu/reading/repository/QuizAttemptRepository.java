package id.ac.ui.cs.advprog.yomu.reading.repository;

import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    boolean existsByUserIdAndTextId(String userId, Long textId);
    List<QuizAttempt> findByUserId(String userId);
    long countByUserId(String userId);
    Optional<QuizAttempt> findByUserIdAndTextId(String userId, Long textId);

    @Modifying
    @Transactional
    @Query("DELETE FROM QuizAttempt q WHERE q.text.id = :textId")
    void deleteByTextId(@Param("textId") Long textId);
}
