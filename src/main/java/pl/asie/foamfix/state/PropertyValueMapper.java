/*
 * Copyright (C) 2016, 2017, 2018, 2019 Adrian Siekierka
 *
 * This file is part of FoamFix.
 *
 * FoamFix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoamFix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FoamFix.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the Minecraft game engine, the Mojang Launchwrapper,
 * the Mojang AuthLib and the Minecraft Realms library (and/or modified
 * versions of said software), containing parts covered by the terms of
 * their respective licenses, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 */

package pl.asie.foamfix.state;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.state.PropertyContainer;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntegerProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.MathHelper;
import pl.asie.foamfix.util.HashingStrategies;

import java.util.*;

public class PropertyValueMapper<C extends PropertyContainer<C>> {
	private static final Comparator<? super Property<?>> COMPARATOR_BIT_FITNESS = (Comparator<Property<?>>) (first, second) -> {
		int diff1 = getPropertyEntry(first).bitSize - first.getValues().size();
		int diff2 = getPropertyEntry(second).bitSize - second.getValues().size();
		// We want to put properties with higher diff-values last,
		// so that the array is as small as possible.
		if (diff1 == diff2) {
			return first.getName().compareTo(second.getName());
		} else {
			return diff1 - diff2;
		}
	};

	public static abstract class Entry {
		private final Property property;
		private final int bitSize;
		private final int bits;

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
			Object[] values = entry.getValueClass().getEnumConstants();

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

		public static Entry create(IntegerProperty entry) {
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

	private static final Map<Property, Entry> entryMap = new IdentityHashMap<>();

	private final Entry[] entryList;
	private final Object2IntMap<String> entryPositionMap;
	private final PropertyContainer[] stateMap;

	public PropertyValueMapper(Collection<Property<?>> properties) {
		entryList = new Entry[properties.size()];
		List<Property<?>> propertiesSortedFitness = Lists.newArrayList(properties);
		propertiesSortedFitness.sort(COMPARATOR_BIT_FITNESS);
		int i = 0;
		for (Property p : propertiesSortedFitness) {
			entryList[i++] = getPropertyEntry(p);
		}

		entryPositionMap = new Object2IntOpenHashMap<>();
		entryPositionMap.defaultReturnValue(-1);

		int bitPos = 0;
		Entry lastEntry = null;
		for (Entry ee : entryList) {
			entryPositionMap.put(ee.property.getName(), bitPos);
			bitPos += ee.bits;
			lastEntry = ee;
		}

		if (lastEntry == null) {
			stateMap = new PropertyContainer[1 << bitPos];
		} else {
			stateMap = new PropertyContainer[(1 << (bitPos - lastEntry.bits)) * lastEntry.property.getValues().size()];
		}
	}

	protected static Entry getPropertyEntry(Property property) {
		Entry e = entryMap.get(property);
		if (e == null) {
			if (property instanceof IntegerProperty) {
				e = IntegerEntry.create((IntegerProperty) property);
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

	public int generateValue(C state) {
		int bitPos = 0;
		int value = 0;
		for (Entry e : entryList) {
			value |= e.get(state.get(e.property)) << bitPos;
			bitPos += e.bits;
		}

		stateMap[value] = state;
		return value;
	}

	public <T extends Comparable<T>, V extends T> C withProperty(int value, Property<T> property, V propertyValue) {
		int bitPos = entryPositionMap.get(property.getName());
		if (bitPos >= 0) {
			Entry e = getPropertyEntry(property);
			int nv = e.get(propertyValue);
			if (nv < 0) return null;

			int bitMask = (e.bitSize - 1);
			value = (value & (~(bitMask << bitPos)) | (nv << bitPos));

			//noinspection unchecked
			return (C) stateMap[value];
		}

		return null;
	}

	public C getPropertyByValue(int value) {
		//noinspection unchecked
		return (C) stateMap[value];
	}

	public <T extends Comparable<T>, V extends T> int withPropertyValue(int value, Property<T> property, V propertyValue) {
		int bitPos = entryPositionMap.getInt(property.getName());
		if (bitPos >= 0) {
			Entry e = getPropertyEntry(property);
			int nv = e.get(propertyValue);
			if (nv < 0) return -1;

			int bitMask = (e.bitSize - 1);
			value = (value & (~(bitMask << bitPos)) | (nv << bitPos));

			return value;
		}

		return -1;
	}

}
