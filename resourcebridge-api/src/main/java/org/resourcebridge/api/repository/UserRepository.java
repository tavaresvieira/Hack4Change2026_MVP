package org.resourcebridge.api.repository;

import org.resourcebridge.api.entity.User;
import org.resourcebridge.api.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByOrganizationId(Long organizationId);

    boolean existsByEmail(String email);

    // Bypasses @SQLRestriction to find deactivated users
    @Query(value = "SELECT * FROM users WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<User> findDeactivatedById(@Param("id") Long id);

    // Restores a soft-deleted user
    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);

    // All staff including deactivated — for admin list view
    @Query(value = "SELECT * FROM users WHERE role = 'STAFF'", nativeQuery = true)
    List<User> findAllStaffIncludingDeactivated();
}
