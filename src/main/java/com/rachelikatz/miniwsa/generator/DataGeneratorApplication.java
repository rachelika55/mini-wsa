package com.rachelikatz.miniwsa.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Command-line entry point for the data generator. Writes generated events as
 * one or more JSON-array files that can be POSTed to {@code /v1/events/ingest}.
 *
 * <p>Run with the Maven exec plugin, for example:
 * <pre>
 * ./mvnw -q compile exec:java -Dexec.args="--count 10000 --out generated-events --chunk-size 1000"
 * </pre>
 *
 * <p>Options (all optional):
 * <ul>
 *   <li>{@code --count} total events to generate (default 10000)</li>
 *   <li>{@code --out} output directory (default {@code generated-events})</li>
 *   <li>{@code --chunk-size} events per file / ingest batch (default 1000)</li>
 *   <li>{@code --seed} RNG seed for reproducible traffic shape (default 42)</li>
 *   <li>{@code --span-minutes} time window the events span, ending now (default 1440)</li>
 * </ul>
 */
public final class DataGeneratorApplication {

	private DataGeneratorApplication() {
	}

	public static void main(String[] args) throws IOException {
		Map<String, String> options = parseArgs(args);

		int count = intOption(options, "count", 10_000);
		int chunkSize = intOption(options, "chunk-size", 1_000);
		long seed = longOption(options, "seed", 42L);
		int spanMinutes = intOption(options, "span-minutes", 1_440);
		Path outDir = Path.of(options.getOrDefault("out", "generated-events"));

		if (count < 0) {
			throw new IllegalArgumentException("--count must not be negative");
		}
		if (chunkSize < 1) {
			throw new IllegalArgumentException("--chunk-size must be at least 1");
		}
		if (spanMinutes < 0) {
			throw new IllegalArgumentException("--span-minutes must not be negative");
		}

		Instant endTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		List<GeneratedEvent> events = new DataGenerator(seed)
				.generate(count, Duration.ofMinutes(spanMinutes), endTime);

		JsonMapper mapper = JsonMapper.builder()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.build();

		Files.createDirectories(outDir);
		int fileCount = 0;
		for (int start = 0; start < events.size(); start += chunkSize) {
			List<GeneratedEvent> chunk = events.subList(start, Math.min(start + chunkSize, events.size()));
			Path file = outDir.resolve(String.format("part-%04d.json", ++fileCount));
			mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), chunk);
		}

		System.out.printf("Generated %d events (seed=%d) across %d file(s) in %s/%n",
				events.size(), seed, fileCount, outDir);
		System.out.println("Feed them into a running app with:");
		System.out.printf(
				"  for f in %s/part-*.json; do curl -s -X POST http://localhost:8080/v1/events/ingest "
						+ "-H 'Content-Type: application/json' --data-binary @\"$f\" "
						+ "-o /dev/null -w \"%s -> %%{http_code}\\n\"; done%n",
				outDir, "$f");
	}

	private static Map<String, String> parseArgs(String[] args) {
		Map<String, String> options = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (!arg.startsWith("--")) {
				throw new IllegalArgumentException("Unexpected argument: " + arg);
			}
			String key = arg.substring(2);
			int eq = key.indexOf('=');
			if (eq >= 0) {
				options.put(key.substring(0, eq), key.substring(eq + 1));
			} else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
				options.put(key, args[++i]);
			} else {
				throw new IllegalArgumentException("Missing value for --" + key);
			}
		}
		return options;
	}

	private static int intOption(Map<String, String> options, String key, int defaultValue) {
		String value = options.get(key);
		return value == null ? defaultValue : Integer.parseInt(value);
	}

	private static long longOption(Map<String, String> options, String key, long defaultValue) {
		String value = options.get(key);
		return value == null ? defaultValue : Long.parseLong(value);
	}
}
