package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
