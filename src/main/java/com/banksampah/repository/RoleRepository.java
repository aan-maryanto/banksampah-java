package com.banksampah.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banksampah.entity.Role;
import com.banksampah.entity.RoleType;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);
    
}
