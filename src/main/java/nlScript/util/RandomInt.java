package nlScript.util;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.IntFunction;

public class RandomInt {

	/**
	 * Generates an integral number in the interval [from; to] (both inclusive)
	 * @param from
	 * @param to
	 * @return
	 */
	public static int next(int from, int to) {
		long origin = (long) from;
		long bound = (long) to + 1l;
		return (int) boundedNextLong(origin, bound);
	}

	public static int[] nextRandomDistinctValues(int from, int to, int n) {
		boolean[] chosen = new boolean[to - from + 1];
		int[] ret = new int[n];
		for(int i = 0; i < n; i++) {
			int v;
			do {
				v = next(from, to);
			} while(chosen[v - from]);
			chosen[v] = true;
			ret[i] = v;
		}
		return ret;
	}

	public static <T> void nextRandomDistinctEntries(T[] collection, int n, T[] ret) {
		int[] indices = nextRandomDistinctValues(0, collection.length - 1, n);
		for(int i = 0; i < n; i++) {
			ret[i] = collection[indices[i]];
		}
	}

	public static <T> ArrayList<T> nextRandomDistinctEntries(ArrayList<T> collection, int n) {
		ArrayList<T> ret = new ArrayList<>();
		int[] indices = nextRandomDistinctValues(0, collection.size() - 1, n);
		for(int i = 0; i < n; i++) {
			ret.add(collection.get(indices[i]));
		}
		return ret;
	}

	/* from https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/jdk/internal/util/random/RandomSupport.java */
	private static long boundedNextLong(long origin, long bound) {
		Random rng = new Random();
		long r = rng.nextLong();
		if (origin < bound) {
			// It's not case (1).
			final long n = bound - origin;
			final long m = n - 1;
			if ((n & m) == 0L) {
				// It is case (2): length of range is a power of 2.
				r = (r & m) + origin;
			} else if (n > 0L) {
				// It is case (3): need to reject over-represented candidates.
                /* This loop takes an unlovable form (but it works):
                   because the first candidate is already available,
                   we need a break-in-the-middle construction,
                   which is concisely but cryptically performed
                   within the while-condition of a body-less for loop. */
				for (long u = r >>> 1;            // ensure nonnegative
					 u + m - (r = u % n) < 0L;    // rejection check
					 u = rng.nextLong() >>> 1) // retry
					;
				r += origin;
			}
			else {
				// It is case (4): length of range not representable as long.
				while (r < origin || r >= bound)
					r = rng.nextLong();
			}
		}
		return r;
	}

	public static void main(String[] args) {
		int min = 0; // Integer.MIN_VALUE;
		int max = 5; // Integer.MAX_VALUE;

		int[] histo = new int[6];

		for(int i = 0; i < 3000; i++) {
			int r = RandomInt.next(min, max);
			histo[r]++;
		}
		for(int i = 0; i < histo.length; i++)
			System.out.println(histo[i]);
	}
}
