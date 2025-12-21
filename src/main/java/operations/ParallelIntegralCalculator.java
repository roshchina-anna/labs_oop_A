package operations;

import exceptions.InterpolationException;
import functions.Point;
import functions.TabulatedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelIntegralCalculator {
    private static final Logger logger = LoggerFactory.getLogger(ParallelIntegralCalculator.class);

    public double integrate(TabulatedFunction function, double from, double to, int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("Количество потоков должно быть больше нуля");
        }
        Point[] points = TabulatedFunctionOperationService.asPoints(function);
        if (points.length < 2) {
            throw new IllegalArgumentException("Для интегрирования требуется минимум две точки");
        }
        double minX = points[0].x;
        double maxX = points[points.length - 1].x;
        if (from < minX || to > maxX) {
            throw new IllegalArgumentException("Интервал интегрирования выходит за пределы функции");
        }
        if (from == to) {
            return 0.0;
        }

        List<Segment> segments = buildSegments(points, from, to);
        threads = Math.min(threads, segments.size());
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Double>> futures = new ArrayList<>();
            int chunkSize = (int) Math.ceil((double) segments.size() / threads);
            for (int i = 0; i < segments.size(); i += chunkSize) {
                final int start = i;
                final int end = Math.min(i + chunkSize, segments.size());
                futures.add(executorService.submit(sumTask(segments.subList(start, end))));
            }
            double total = 0.0;
            for (Future<Double> future : futures) {
                try {
                    total += future.get();
                } catch (ExecutionException e) {
                    throw new IllegalArgumentException("Не удалось вычислить интеграл", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Вычисление интеграла было прервано", e);
                }
            }
            logger.info("Calculated integral on [{}; {}] using {} threads", from, to, threads);
            return total;
        } finally {
            executorService.shutdownNow();
        }
    }

    private Callable<Double> sumTask(List<Segment> segments) {
        return () -> segments.stream()
                .mapToDouble(seg -> 0.5 * (seg.end - seg.start) * (seg.startValue + seg.endValue))
                .sum();
    }

    private List<Segment> buildSegments(Point[] points, double from, double to) {
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < points.length - 1; i++) {
            Point left = points[i];
            Point right = points[i + 1];
            double start = Math.max(from, left.x);
            double end = Math.min(to, right.x);
            if (start >= end) {
                continue;
            }
            double startValue = interpolate(left, right, start);
            double endValue = interpolate(left, right, end);
            segments.add(new Segment(start, end, startValue, endValue));
        }
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Интервал интегрирования некорректен или не пересекается с функцией");
        }
        return segments;
    }

    private double interpolate(Point left, Point right, double x) {
        double delta = right.x - left.x;
        if (delta == 0) {
            throw new InterpolationException("Невозможно интерполировать при нулевом интервале по оси x");
        }
        double t = (x - left.x) / delta;
        return left.y + t * (right.y - left.y);
    }

    private record Segment(double start, double end, double startValue, double endValue) { }
}