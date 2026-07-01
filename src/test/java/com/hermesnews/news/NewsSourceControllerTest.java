package com.hermesnews.news;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NewsSourceController.class)
class NewsSourceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NewsSourceService newsSourceService;

	@MockitoBean
	private RssNewsCollector rssNewsCollector;

	@Test
	void listsSourcesWithHealth() throws Exception {
		when(newsSourceService.listSources()).thenReturn(List.of(new NewsSourceResponse(
				null,
				"example.com",
				NewsSourceType.RSS,
				"https://example.com/feed",
				true,
				Instant.parse("2026-07-01T08:00:00Z"),
				Instant.parse("2026-07-01T09:00:00Z"),
				null,
				null,
				0,
				"OK")));

		mockMvc.perform(get("/api/news-sources"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].url").value("https://example.com/feed"))
				.andExpect(jsonPath("$[0].status").value("OK"))
				.andExpect(jsonPath("$[0].enabled").value(true));
	}

	@Test
	void addsRssSource() throws Exception {
		var source = new NewsSource("example.com", NewsSourceType.RSS, "https://example.com/feed");
		when(newsSourceService.addRssSource("https://example.com/feed")).thenReturn(source);

		mockMvc.perform(post("/api/news-sources/rss")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com/feed\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.url").value("https://example.com/feed"));
	}

	@Test
	void testsRssSource() throws Exception {
		when(rssNewsCollector.testSource("https://example.com/feed"))
				.thenReturn(new NewsSourceTestResponse(
						"https://example.com/feed",
						true,
						3,
						"https://example.com/feed",
						"Source returned 3 article(s)."));

		mockMvc.perform(post("/api/news-sources/test")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com/feed\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.articleCount").value(3));
	}

	@Test
	void disablesSource() throws Exception {
		var source = new NewsSource("example.com", NewsSourceType.RSS, "https://example.com/feed");
		source.disable();
		when(newsSourceService.disableSource("https://example.com/feed")).thenReturn(source);

		mockMvc.perform(post("/api/news-sources/disable")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com/feed\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));
	}

	@Test
	void updatesSourceLabel() throws Exception {
		var source = new NewsSource("example.com", NewsSourceType.RSS, "https://example.com/feed");
		source.rename("Example Feed");
		when(newsSourceService.updateLabel("https://example.com/feed", "Example Feed")).thenReturn(source);

		mockMvc.perform(post("/api/news-sources/label")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com/feed\",\"name\":\"Example Feed\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Example Feed"));
	}
}
