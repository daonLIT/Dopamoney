package org.scoula.backend.global;

import java.time.LocalDate;
import java.util.Random;

import org.scoula.backend.record.GameRecord;
import org.scoula.backend.record.GameRecordRepository;
import org.scoula.backend.schedule.ScheduleService;
import org.scoula.backend.user.User;
import org.scoula.backend.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 리더보드가 비지 않도록 예시 유저/기록을 한 번만 넣고, 일정 캐시를 미리 받는다. */
@Component
public class SeedData implements ApplicationRunner {
	private static final String[][] SEEDS = { { "9회말또역전", "LOT" }, { "직관요정", "LG" }, { "치킨값은내가", "HAN" },
			{ "오늘도직관", "HAN" }, { "잠실러", "DOO" }, { "대구불패", "SAM" }, { "광주의밤", "KIA" }, { "문학전설", "SSG" },
			{ "수원킹", "KT" }, { "창원맨", "NC" }, { "고척돔주민", "KIW" }, { "패배요정", "LOT" }, { "승리의부적", "LG" },
			{ "마지막이닝", "HAN" }, { "외야석단골", "KIA" } };

	private final UserRepository users;
	private final GameRecordRepository records;
	private final ScheduleService schedule;

	public SeedData(UserRepository users, GameRecordRepository records, ScheduleService schedule) {
		this.users = users;
		this.records = records;
		this.schedule = schedule;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (users.count() == 0) seed();
		Thread t = new Thread(schedule::preload, "schedule-preload");
		t.setDaemon(true);
		t.start();
	}

	private void seed() {
		Random rnd = new Random(7);
		String hash = PasswordUtil.encode("seedpass1234");
		Team[] teams = Team.values();
		String[] tags = { "LOSE", "REVERSE_LOSS", "WIN", "BIG_WIN", "VERIFIED" };
		for (int i = 0; i < SEEDS.length; i++) {
			User u = new User();
			u.setLoginId("seed_user" + (i + 1));
			u.setPasswordHash(hash);
			u.setNickname(SEEDS[i][0]);
			u.setTeamCode(SEEDS[i][1]);
			users.save(u);
			int visits = 3 + rnd.nextInt(20);
			for (int v = 0; v < visits; v++) {
				GameRecord r = new GameRecord();
				r.setUserId(u.getId());
				r.setGameDate(LocalDate.of(2026, 4, 1).plusDays(rnd.nextInt(180)));
				Team opp;
				do {
					opp = teams[rnd.nextInt(teams.length)];
				} while (opp.code.equals(SEEDS[i][1]));
				r.setOpponentCode(opp.code);
				String tag = tags[rnd.nextInt(tags.length)];
				r.setTag(tag);
				r.setResult(switch (tag) {
					case "WIN", "BIG_WIN" -> "WIN";
					case "LOSE", "REVERSE_LOSS" -> "LOSE";
					default -> rnd.nextBoolean() ? "WIN" : "LOSE";
				});
				r.setMemo("예시 기록 " + (v + 1));
				r.setTicketCost(15000 + rnd.nextInt(5) * 5000);
				r.setTransportCost(2000 + rnd.nextInt(5) * 1000);
				r.setFoodCost(8000 + rnd.nextInt(6) * 4000);
				records.save(r);
			}
		}
	}
}
