package com.sanction.kg.repository.mariadb;

import com.sanction.kg.entity.relational.ChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {

    List<ChangeLog> findByTargetIdOrderByCreatedAtDesc(String targetId);

    List<ChangeLog> findByVersionIdOrderByCreatedAtDesc(Integer versionId);

    Page<ChangeLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId, Pageable pageable);

    Page<ChangeLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
