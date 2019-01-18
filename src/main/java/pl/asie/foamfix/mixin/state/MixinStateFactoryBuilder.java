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

package pl.asie.foamfix.mixin.state;

import com.google.common.collect.Maps;
import net.minecraft.state.AbstractPropertyContainer;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.asie.foamfix.state.FoamyStateFactory;

import java.util.Map;

@Mixin(StateFactory.Builder.class)
public class MixinStateFactoryBuilder {
	@Shadow
	private Object baseObject;
	@Shadow
	private Map<String, Property<?>> propertyMap;

	@Inject(at = @At("HEAD"), method = "build", cancellable = true)
	public void beforeBuild(StateFactory.Factory factory, CallbackInfoReturnable<StateFactory<?, ?>> info) {
		if (FoamyStateFactory.hasFactory(baseObject)) {
			//noinspection unchecked
			info.setReturnValue(new FoamyStateFactory(baseObject, factory, propertyMap));
			info.cancel();
		}
	}
}
