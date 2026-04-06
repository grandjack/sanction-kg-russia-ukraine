package com.sanction.kg.repository.mariadb;

import com.sanction.kg.entity.relational.Snapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {

    List<Snapshot> findBySourceOrderByCreatedAtDesc(String source);

    List<Snapshot> findByStatusOrderByCreatedAtDesc(String status);

    Page<Snapshot> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT s FROM Snapshot s ORDER BY s.createdAt DESC")
    List<Snapshot> findAllOrderByCreatedAtDesc();

    boolean existsByFileHash(String fileHash);
}
