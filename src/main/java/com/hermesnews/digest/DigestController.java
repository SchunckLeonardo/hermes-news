package com.hermesnews.digest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digests")
public class DigestController {

	private final DailyDigestService service;

	public DigestController(DailyDigestService service) {
		this.service = service;
	}

	@PostMapping("/send-daily")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public DailyDigestResult sendDailyDigest() {
		return service.sendDailyDigest();
	}
}
