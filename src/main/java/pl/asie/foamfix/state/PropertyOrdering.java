package pl.asie.foamfix.state;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.MathHelper;
import pl.asie.foamfix.util.HashingStrategies;

import java.util.*;

public class PropertyOrdering {
	public static abstract class Entry {
		final Property property;
		final int bitSize;
		final int bits;

		private Entry(Property property) {
			this.property = property;

			this.bitSize = MathHelper.smallestEncompassingPowerOfTwo(property.getValues().size());
			int bits = 0;

			int b = bitSize - 1;
			while (b != 0) {
				bits++;
				b >>= 1;
			}
			this.bits = bits;
		}

		public abstract int get(Object v);

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Entry))
				return false;

			return ((Entry) other).property.equals(property);
		}

		@Override
		public int hashCode() {
			return property.hashCode();
		}
	}

	public static class BooleanEntry extends Entry {
		private BooleanEntry(Property property) {
			super(property);
		}

		@Override
		public int get(Object v) {
			return v == Boolean.TRUE ? 1 : 0;
		}
	}

	public static class ObjectEntry extends Entry {
		private Object2IntMap values;

		private ObjectEntry(Property property, boolean identity) {
			super(property);

			//noinspection unchecked
			this.values = identity ? new Object2IntOpenCustomHashMap(HashingStrategies.FASTUTIL_IDENTITY) : new Object2IntOpenHashMap();
			this.values.defaultReturnValue(-1);
			//noinspection unchecked
			Collection<Object> allowedValues = property.getValues();

			int i = 0;
			for (Object o : allowedValues) {
				this.values.put(o, i++);
			}
		}

		@Override
		public int get(Object v) {
			return values.getInt(v);
		}
	}

	public static class EnumEntrySorted extends Entry {
		private EnumEntrySorted(Property property, int count) {
			super(property);
		}

		@Override
		public int get(Object v) {
			return ((Enum) v).ordinal();
		}

		public static Entry create(EnumProperty entry) {
			Object[] values = entry.getType().getEnumConstants();

			if (entry.getValues().size() == values.length) {
				return new EnumEntrySorted(entry, values.length);
			} else {
				return new ObjectEntry(entry, true);
			}
		}
	}

	public static class IntegerEntrySorted extends Entry {
		private final int minValue, count;

		private IntegerEntrySorted(Property property, int minValue, int count) {
			super(property);

			this.minValue = minValue;
			this.count = count;
		}

		@Override
		public int get(Object v) {
			int vv = ((int) v) - minValue;
			// if vv < 0, it will be rejected anyway
			return vv < count ? vv : -1;
		}
	}

	public static class IntegerEntry extends Entry {
		private Int2IntMap values;

		private IntegerEntry(Property property) {
			super(property);

			this.values = new Int2IntOpenHashMap();
			this.values.defaultReturnValue(-1);
			Collection<Object> allowedValues = property.getValues();

			int i = 0;
			for (Object o : allowedValues) {
				this.values.put((int) o, i++);
			}
		}

		@Override
		public int get(Object v) {
			return values.get(v);
		}

		public static Entry create(IntProperty entry) {
			List<Integer> sorted = Lists.newArrayList(entry.getValues());
			sorted.sort(Comparator.naturalOrder());

			int min = sorted.get(0);
			for (int i = 1; i < sorted.size(); i++) {
				if ((sorted.get(i) - sorted.get(i - 1)) != 1) {
					return new IntegerEntry(entry);
				}
			}

			return new IntegerEntrySorted(entry, min, sorted.size());
		}
	}

	private PropertyOrdering() {

	}

	private static final Map<Property, Entry> entryMap = new IdentityHashMap<>();

	static Entry getEntry(Property property) {
		Entry e = entryMap.get(property);
		if (e == null) {
			if (property instanceof IntProperty) {
				e = IntegerEntry.create((IntProperty) property);
			} else if (property.getClass() == BooleanProperty.class && property.getValues().size() == 2) {
				e = new BooleanEntry(property);
			} else if (property instanceof EnumProperty) {
				e = EnumEntrySorted.create((EnumProperty) property);
			} else {
				e = new ObjectEntry(property, false);
			}
			entryMap.put(property, e);
		}
		return e;
	}
}
