package com.example.docgen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI entry point.
 *
 *   mvn -q compile exec:java -Dexec.args="samples/loan-payload.json out"
 *   mvn -q compile exec:java -Dexec.args="samples/loan-payload.json out --signature-fields"
 *   mvn -q compile exec:java -Dexec.args="samples/loan-payload.json out --bench 20"
 */
public final class DocGenPoc {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: DocGenPoc <payload.json> [outDir] [--signature-fields] [--bench N]");
            System.exit(1);
        }
        Path payloadFile = Path.of(args[0]);
        Path outRoot = Path.of(args.length > 1 && !args[1].startsWith("--") ? args[1] : "out");
        boolean sigFields = false;
        int bench = 0;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--signature-fields")) sigFields = true;
            if (args[i].equals("--bench") && i + 1 < args.length) bench = Integer.parseInt(args[i + 1]);
        }

        byte[] payload = Files.readAllBytes(payloadFile);
        DocGenService service = new DocGenService();
        DocGenService.Outcome outcome = service.generate(payload, outRoot, sigFields);

        System.out.println();
        System.out.println(outcome.success() ? "GENERATED" : "REJECTED");
        System.out.println("  output : " + outcome.outDir().toAbsolutePath());
        outcome.audit().forEach((k, v) -> System.out.println("  " + k + " = " + v));

        if (bench > 0 && outcome.success()) {
            // First call above warmed up the JVM/fonts; measure steady state.
            List<Long> times = new ArrayList<>();
            for (int i = 0; i < bench; i++) {
                long t = System.nanoTime();
                service.generate(payload, outRoot.resolve("bench"), sigFields);
                times.add((System.nanoTime() - t) / 1_000_000);
            }
            times.sort(Long::compare);
            double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
            System.out.printf("%nBench x%d (single thread): avg %.0f ms, p50 %d ms, max %d ms%n",
                    bench, avg, times.get(times.size() / 2), times.get(times.size() - 1));
        }
        System.exit(outcome.success() ? 0 : 2);
    }
}
