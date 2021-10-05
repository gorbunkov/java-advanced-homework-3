package homework.server.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class SumCommand {

    private static final Logger log = LoggerFactory.getLogger(SumCommand.class);

    private int n;

    public SumCommand(int n) {
        this.n = n;
    }

    public long sum() {
        log.info("SUM command started");
        return ForkJoinPool.commonPool().invoke(new SumRecursiveTask(1, n));
    }

    private static class SumRecursiveTask extends RecursiveTask<Long> {

        private static final int THRESHOLD = 1000;
        private final long start;
        private final long end;

        public SumRecursiveTask(long start, long end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            long length = end - start;
            long result = 0;
            if (length < THRESHOLD) {
                result = computeDirectly();
            } else {
                SumRecursiveTask leftTask = new SumRecursiveTask(start, start + length / 2);
                SumRecursiveTask rightTask = new SumRecursiveTask(end - length / 2, end);
                ForkJoinTask.invokeAll(leftTask, rightTask);
                result += leftTask.join();
                result += rightTask.join();
            }
            return result;
        }

        private long computeDirectly() {
            long sum = 0;
            for (long i = start; i <= end; i++) {
                sum += i;
            }
            return sum;
        }
    }

}
