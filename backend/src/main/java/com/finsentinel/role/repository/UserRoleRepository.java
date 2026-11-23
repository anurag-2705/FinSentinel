package com.finsentinel.role.repository;

import com.finsentinel.role.entity.UserRole;
import com.finsentinel.role.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUser_Id(UUID userId);

    boolean existsByUser_IdAndRole_Name(UUID userId, String roleName);

    List<UserRole> findByRole_Name(String roleName);
}