package com.examples.util;

import java.lang.reflect.Array;
import java.util.*;

public class ArrayUtils {
	@SuppressWarnings("unchecked")
	public static <T> List<T> toList(Object... items) {
		
		List<T> list = new ArrayList<T>();
		
		if (items.length == 1 && items[0].getClass().isArray()) {
			int length = Array.getLength(items[0]);
			for (int i = 0; i < length; i++) {
				Object element = Array.get(items[0], i);
				T item = (T)element;
				list.add(item);
			}
		} else {
			for (Object i : items) {
				T item = (T)i;
				list.add(item);
			}
		}
			
		return list;
	}
}
