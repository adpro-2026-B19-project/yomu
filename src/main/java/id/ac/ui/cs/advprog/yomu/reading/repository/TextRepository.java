package id.ac.ui.cs.advprog.yomu.reading.repository;

import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TextRepository extends JpaRepository<Text, Long>, JpaSpecificationExecutor<Text> {

    List<Text> findByPublishedTrue();

    Page<Text> findByPublishedTrue(Pageable pageable);

    java.util.Optional<Text> findByIdAndPublishedTrue(Long id);

}
