package com.coworking.booking.repository;

import com.coworking.booking.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findAllByOrderByNameAsc();
}
