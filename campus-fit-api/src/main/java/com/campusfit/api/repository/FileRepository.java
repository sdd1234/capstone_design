package com.campusfit.api.repository;

import com.campusfit.api.domain.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
}
