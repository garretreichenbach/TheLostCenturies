package com.thelostcenturies.core.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Wraps a density function and adds {@code bias * weight}, where weight is 1 within {@code innerRadius}
 * blocks of the world center, fades smoothly to 0 at {@code outerRadius}, and is 0 beyond.
 *
 * <p>Runtime-only: it's injected into the overworld's climate sampler (see {@link DryZone}) and never
 * read from or written to a datapack, so its codec refuses to (de)serialize.
 */
public record RadialBias(DensityFunction input, double bias, double centerX, double centerZ,
                         double innerRadius, double outerRadius) implements DensityFunction {

    private static final KeyDispatchDataCodec<RadialBias> CODEC = KeyDispatchDataCodec.of(
            MapCodec.<RadialBias>unit(() -> {
                throw new UnsupportedOperationException("lostcenturies RadialBias is runtime-only");
            }));

    /** 1 inside innerRadius, smoothstep down to 0 at outerRadius. */
    public double weight(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d <= innerRadius) {
            return 1.0;
        }
        if (d >= outerRadius) {
            return 0.0;
        }
        double t = (d - innerRadius) / (outerRadius - innerRadius);
        return 1.0 - t * t * (3.0 - 2.0 * t);
    }

    @Override
    public double compute(FunctionContext ctx) {
        return input.compute(ctx) + bias * weight(ctx.blockX(), ctx.blockZ());
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new RadialBias(input.mapAll(visitor), bias, centerX, centerZ, innerRadius, outerRadius));
    }

    @Override
    public double minValue() {
        return input.minValue() + Math.min(0.0, bias);
    }

    @Override
    public double maxValue() {
        return input.maxValue() + Math.max(0.0, bias);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
