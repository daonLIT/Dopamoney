package org.scoula.backend.record;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class RecordController {
	private final RecordService service;

	public RecordController(RecordService service) {
		this.service = service;
	}

	@GetMapping("/summary")
	public Map<String, Object> summary(@RequestAttribute("userId") Long userId) {
		return service.summaryOf(userId);
	}

	@GetMapping
	public List<Map<String, Object>> list(@RequestAttribute("userId") Long userId,
			@RequestParam(required = false) Integer limit) {
		return service.list(userId, limit);
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> create(@RequestAttribute("userId") Long userId,
			@RequestBody RecordService.RecordReq req) {
		return ResponseEntity.status(201).body(service.save(userId, null, req));
	}

	@PutMapping("/{id}")
	public Map<String, Object> update(@RequestAttribute("userId") Long userId, @PathVariable Long id,
			@RequestBody RecordService.RecordReq req) {
		return service.save(userId, id, req);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
		service.delete(userId, id);
		return ResponseEntity.noContent().build();
	}
}
