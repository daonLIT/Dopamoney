package org.scoula.backend.record;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {
	List<GameRecord> findByUserIdOrderByGameDateDescIdDesc(Long userId);

	Optional<GameRecord> findByIdAndUserId(Long id, Long userId);
}
