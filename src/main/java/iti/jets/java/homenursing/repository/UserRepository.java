package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("select u from User u left join fetch u.profiles where u.phoneNumber = :phone")
    Optional<User> findByPhoneNumberWithProfiles(@Param("phone") String phoneNumber);

    @Query("select u from User u left join fetch u.profiles where u.id = :id")
    Optional<User> findByIdWithProfiles(@Param("id") UUID id);

    boolean existsByPhoneNumber(String phoneNumber);
}

