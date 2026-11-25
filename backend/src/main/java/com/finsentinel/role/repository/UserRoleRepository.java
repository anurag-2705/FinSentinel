package com.finsentinel.role.repository;

import com.finsentinel.role.entity.UserRole;
import com.finsentinel.role.entity.UserRoleId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUser_Id(UUID userId);

    List<UserRole> findByUser_IdIn(List<UUID> userIds);

    boolean existsByUser_IdAndRole_Name(UUID userId, String roleName);

    List<UserRole> findByRole_Name(String roleName);

    @Query("SELECT ur.role.name FROM UserRole ur WHERE ur.user.id = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);
}