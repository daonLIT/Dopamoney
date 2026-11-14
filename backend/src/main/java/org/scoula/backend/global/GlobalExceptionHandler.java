package org.scoula.backend.global;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static ResponseEntity<Map<String, String>> res(int s, String m) {
		return ResponseEntity.status(s).body(Map.of("message", m));
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, String>> api(ApiException e) {
		return res(e.status, e.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> badBody(HttpMessageNotReadableException e) {
		return res(400, "요청 형식이 올바르지 않아요");
	}

	@ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<Map<String, String>> badParam(Exception e) {
		return res(400, "요청이 올바르지 않아요");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, String>> method(Exception e) {
		return res(405, "지원하지 않는 요청이에요");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> notFound(Exception e) {
		return res(404, "찾을 수 없어요");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> other(Exception e) {
		e.printStackTrace();
		return res(500, "서버 오류가 발생했어요");
	}
}
