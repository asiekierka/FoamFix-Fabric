package pl.asie.foamfix.state;

import net.minecraft.state.State;
import net.minecraft.state.property.Property;

public interface PropertyValueMapper<C extends State<C>> {
	<T extends Comparable<T>, V extends T> C with(int value, Property<T> property, V propertyValue);
	int generateValue(C state);
}