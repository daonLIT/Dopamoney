package org.scoula.backend.record;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game_records")
@Getter
@Setter
@NoArgsConstructor
public class GameRecord {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;
	private LocalDate gameDate;
	private String opponentCode;
	/** KBO gameId (예: 20261014OBLG0). 실제 경기와 연결된 기록만 저장된다. */
	private String gameId;
	private String awayCode;
	private String homeCode;
	private Integer awayScore;
	private Integer homeScore;
	/** WIN / LOSE / null */
	private String result;
	private String tag;

	@Column(length = 1000)
	private String memo;

	private int ticketCost;
	private int transportCost;
	private int foodCost;
	private Integer myScore;
	private Integer opponentScore;

	public int totalCost() {
		return ticketCost + transportCost + foodCost;
	}
}
