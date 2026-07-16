package nlScript.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class Diff {

	public static <K,V> String diff(Map<K,V> a, Map<K,V> b) {
		if(a.equals(b))
			return "";

		StringBuilder sb = new StringBuilder();

		if (a == null || b == null) {
			sb.append("map: ").append(a).append(" vs ").append(b).append("\n");
			return sb.toString();
		}

		Set<K> onlyInA = new TreeSet<>(a.keySet());
		onlyInA.removeAll(b.keySet());
		Set<K> onlyInB = new TreeSet<>(b.keySet());
		onlyInB.removeAll(a.keySet());

		for (K key : onlyInA) {
			sb.append("map: key '").append(key).append("' only in this (value=")
					.append(a.get(key)).append(")\n");
		}
		for (K key : onlyInB) {
			sb.append("map: key '").append(key).append("' only in other (value=")
					.append(b.get(key)).append(")\n");
		}

		Set<K> common = new TreeSet<>(a.keySet());
		common.retainAll(b.keySet());
		for (K key : common) {
			V va = a.get(key);
			V vb = b.get(key);
			if (!Objects.equals(va, vb)) {
				sb.append("map: key '").append(key).append("' differs: ")
						.append(va).append(" vs ").append(vb).append("\n");
			}
		}

		return sb.toString();
	}

	public static <T> String diff(Set<T> a, Set<T> b) {
		if(a.equals(b))
			return "";

		StringBuilder sb = new StringBuilder();

		if (a == null || b == null) {
			sb.append("map: ").append(a).append(" vs ").append(b).append("\n");
			return sb.toString();
		}

		Set<T> onlyInA = new HashSet<>(a);
		onlyInA.removeAll(b);
		Set<T> onlyInB = new HashSet<>(b);
		onlyInB.removeAll(a);

		for (T key : onlyInA) {
			sb.append("map: '").append(key).append("' only in this\n");
		}
		for (T key : onlyInB) {
			sb.append("map: '").append(key).append("' only in other\n");
		}

		return sb.toString();
	}

	public static <T> String diff(List<T> a, List<T> b) {
		if (Objects.equals(a, b)) return "";

		StringBuilder sb = new StringBuilder();

		if (a == null || b == null) {
			sb.append("list: ").append(a).append(" vs ").append(b).append("\n");
			return sb.toString();
		}

		if (a.size() != b.size()) {
			sb.append("list: size differs: ").append(a.size())
					.append(" vs ").append(b.size()).append("\n");
		}

		int minSize = Math.min(a.size(), b.size());
		for (int i = 0; i < minSize; i++) {
			T va = a.get(i);
			T vb = b.get(i);
			if (!Objects.equals(va, vb)) {
				sb.append("list: index ").append(i).append(" differs: '")
						.append(va).append("' vs '").append(vb).append("'\n");
			}
		}

		if (a.size() > minSize) {
			sb.append("list: extra elements in this: ")
					.append(a.subList(minSize, a.size())).append("\n");
		} else if (b.size() > minSize) {
			sb.append("list: extra elements in other: ")
					.append(b.subList(minSize, b.size())).append("\n");
		}
		return sb.toString();
	}
}
