package id.ac.ui.cs.advprog.yomu.reading.repository;

import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}