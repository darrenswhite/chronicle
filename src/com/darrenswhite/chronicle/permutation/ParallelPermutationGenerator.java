package com.darrenswhite.chronicle.permutation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * @author Darren White
 */
public class ParallelPermutationGenerator<T, R> extends PermutationGenerator<T, R> {

	private final ArrayList<Future<R>> queue;
	private final ExecutorService executor;
	private final int capacity;
	private R next = null;

	public ParallelPermutationGenerator(Iterator<T[]> it, Function<T[], R> f, PermutationConsumer<R> consumer, ExecutorService executor, int capacity) {
		super(it, f, consumer);
		this.executor = executor;
		queue = new ArrayList<>(this.capacity = capacity);
	}

	private void consumeQueue() {
		Iterator<Future<R>> it = queue.iterator();

		while (it.hasNext()) {
			Future<R> f = it.next();

			if (f.isDone()) {
				it.remove();

				try {
					consumer.accept(f.get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void run() {
		System.out.println("Starting consumer...");

		if (!consumer.start()) {
			System.err.println("Consumer failed to start.");
			return;
		}

		System.out.println("Consumer started successfully.");

		System.out.println("Iterating permutations...");

		while (it.hasNext() || !queue.isEmpty()) {
			if (queue.size() < capacity) {
				T[] next = it.next();
				queue.add(executor.submit(() -> f.apply(next)));
			}

			consumeQueue();
		}

		while (!queue.isEmpty()) {
			consumeQueue();
		}

		System.out.println("Iteration completed successfully.");

		System.out.println("Shutting down executor service...");

		executor.shutdown();

		while (!executor.isTerminated()) {
			try {
				Thread.sleep(500L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Stopping consumer...");

		consumer.stop();

		System.out.println("Consumer stopped successfully.");
	}
}