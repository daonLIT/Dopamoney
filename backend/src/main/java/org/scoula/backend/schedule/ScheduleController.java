package org.scoula.backend.schedule;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import org.scoula.backend.global.ApiException;
import org.scoula.backend.user.User;
import org.scoula.backend.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
	private final ScheduleService schedule;
	private final UserRepository users;

	public ScheduleController(ScheduleService schedule, UserRepository users) {
		this.schedule = schedule;
		this.users = users;
	}

	@GetMapping
	public Map<String, Object> day(@RequestParam(required = false) String date,
			@RequestAttribute(value = "userId", required = false) Long userId) {
		LocalDate d;
		try {
			d = date == null || date.isBlank() ? ScheduleService.today() : LocalDate.parse(date);
		} catch (Exception e) {
			throw new ApiException(400, "날짜 형식은 yyyy-MM-dd예요");
		}
		String team = null;
		if (userId != null) team = users.findById(userId).map(User::getTeamCode).orElse(null);
		return schedule.day(d, team);
	}

	@GetMapping("/month")
	public Map<String, Object> month(@RequestParam(required = false) String month) {
		YearMonth ym;
		try {
			ym = month == null || month.isBlank() ? YearMonth.now(ScheduleService.KST) : YearMonth.parse(month);
		} catch (Exception e) {
			throw new ApiException(400, "월 형식은 yyyy-MM이에요");
		}
		return schedule.month(ym);
	}
}
