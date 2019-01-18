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

package pl.asie.foamfix.mixin.client;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(BasicBakedModel.class)
public class MixinBasicBakedModel {
	/**
	 * This saves a good 9*7*8=504 bytes per model, in the best case, which isn't bad at all - and it doesn't hurt!
	 */
	@Inject(method = "<init>", at = @At("RETURN"))
	public void construct(List<BakedQuad> list_1, Map<Direction, List<BakedQuad>> map_1, boolean boolean_1, boolean boolean_2, Sprite sprite_1, ModelTransformation modelTransformation_1, ModelItemPropertyOverrideList modelItemPropertyOverrideList_1, CallbackInfo info) {
		if (list_1 instanceof ArrayList) {
			((ArrayList<BakedQuad>) list_1).trimToSize();
		}

		for (List<BakedQuad> l : map_1.values()) {
			if (l instanceof ArrayList) {
				((ArrayList<BakedQuad>) l).trimToSize();
			}
		}
	}
}