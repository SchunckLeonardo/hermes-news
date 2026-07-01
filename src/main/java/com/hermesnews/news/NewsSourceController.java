package com.hermesnews.news;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news-sources")
public class NewsSourceController {

	private final NewsSourceService newsSourceService;
	private final RssNewsCollector rssNewsCollector;

	public NewsSourceController(NewsSourceService newsSourceService, RssNewsCollector rssNewsCollector) {
		this.newsSourceService = newsSourceService;
		this.rssNewsCollector = rssNewsCollector;
	}

	@GetMapping
	public List<NewsSourceResponse> listSources() {
		return newsSourceService.listSources();
	}

	@PostMapping("/rss")
	@ResponseStatus(HttpStatus.CREATED)
	public NewsSourceResponse addRssSource(@RequestBody NewsSourceUrlRequest request) {
		return NewsSourceResponse.from(newsSourceService.addRssSource(request.url()));
	}

	@PostMapping("/test")
	public NewsSourceTestResponse testSource(@RequestBody NewsSourceUrlRequest request) {
		return rssNewsCollector.testSource(request.url());
	}

	@PostMapping("/label")
	public NewsSourceResponse updateLabel(@RequestBody NewsSourceLabelRequest request) {
		return NewsSourceResponse.from(newsSourceService.updateLabel(request.url(), request.name()));
	}

	@PostMapping("/enable")
	public NewsSourceResponse enableSource(@RequestBody NewsSourceUrlRequest request) {
		return NewsSourceResponse.from(newsSourceService.enableSource(request.url()));
	}

	@PostMapping("/disable")
	public NewsSourceResponse disableSource(@RequestBody NewsSourceUrlRequest request) {
		return NewsSourceResponse.from(newsSourceService.disableSource(request.url()));
	}
}
