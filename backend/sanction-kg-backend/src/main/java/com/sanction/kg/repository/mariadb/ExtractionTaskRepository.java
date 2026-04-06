package com.sanction.kg.repository.mariadb;

import com.sanction.kg.entity.relational.ExtractionTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractionTaskRepository extends JpaRepository<ExtractionTask, Long> {

    List<ExtractionTask> findByStatusOrderByCreatedAtDesc(String status);

    Page<ExtractionTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ExtractionTask> findBySourceSnapshotId(Long snapshotId);
}
