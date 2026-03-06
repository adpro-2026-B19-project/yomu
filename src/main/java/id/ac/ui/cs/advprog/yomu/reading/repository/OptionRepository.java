package id.ac.ui.cs.advprog.yomu.reading.repository;

import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<Option, Long> {
}