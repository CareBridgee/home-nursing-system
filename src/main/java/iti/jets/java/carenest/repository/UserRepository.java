package iti.jets.java.carenest.repository;

import iti.jets.java.carenest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
