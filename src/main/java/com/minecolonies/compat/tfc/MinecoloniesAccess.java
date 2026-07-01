package com.minecolonies.compat.tfc;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reflective access to inherited MineColonies fields that Mixin's {@code @Shadow} cannot resolve in
 * this modpack's environment.
 *
 * <p>Against the open-minecolonies build used here, {@code @Shadow} of any <em>inherited</em> member
 * (field or method) fails to attach at APPLY time — the crash is
 * {@code InvalidMixinException: @Shadow field/method ... was not located in the target class} — which
 * aborts MineColonies' building registration and takes the whole load down. Only members declared
 * directly on the target class attach. Public inherited members can still be reached with an ordinary
 * runtime cast (resolved by the JVM, not Mixin); the two fields below are {@code protected} with no
 * accessor, so we read them reflectively by walking the real runtime class hierarchy.
 */
public final class MinecoloniesAccess
{
    private static volatile Field workerField;
    private static volatile Field keepXField;

    /**
     * @return the {@code keepX} map of a MineColonies building (declared on
     * {@code AbstractBuildingContainer}), used to register items the worker must keep.
     */
    @SuppressWarnings("unchecked")
    public static Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> keepX(final Object building)
    {
        try
        {
            if (keepXField == null)
            {
                keepXField = find(building.getClass(), "keepX");
            }
            return (Map<Predicate<ItemStack>, Tuple<Integer, Boolean>>) keepXField.get(building);
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException("MineColonies TFC Compat: could not access building keepX map", e);
        }
    }

    /**
     * @return the citizen entity a worker AI is driving (the {@code worker} field declared on
     * {@code AbstractAISkeleton}).
     */
    public static AbstractEntityCitizen worker(final Object ai)
    {
        try
        {
            if (workerField == null)
            {
                workerField = find(ai.getClass(), "worker");
            }
            return (AbstractEntityCitizen) workerField.get(ai);
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException("MineColonies TFC Compat: could not access AI worker", e);
        }
    }

    /** Locate a field by name, walking up the class hierarchy so inherited fields are found. */
    private static Field find(final Class<?> type, final String name) throws NoSuchFieldException
    {
        for (Class<?> c = type; c != null; c = c.getSuperclass())
        {
            try
            {
                final Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            }
            catch (final NoSuchFieldException ignored)
            {
                // keep walking up
            }
        }
        throw new NoSuchFieldException(name);
    }

    private MinecoloniesAccess() {}
}
