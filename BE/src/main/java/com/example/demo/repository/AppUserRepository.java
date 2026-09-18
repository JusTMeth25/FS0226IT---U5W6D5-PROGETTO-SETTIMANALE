package com.example.demo.repository;

import com.example.demo.model.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByUsername(String username);

	boolean existsByUsername(String username);

	/** Everyone the given user can talk to, themselves excluded. */
	List<AppUser> findByUsernameNotOrderByDisplayNameAsc(String username);
}
