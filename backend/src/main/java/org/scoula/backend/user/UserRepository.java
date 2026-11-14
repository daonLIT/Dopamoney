package org.scoula.backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByLoginId(String loginId);

	Optional<User> findByNickname(String nickname);
}
