package org.github.jamm.strategies;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import org.github.jamm.MemoryLayoutSpecification;

import static org.github.jamm.MathUtils.roundTo;

/**
 * {@code MemoryMeterStrategy} that computes the size of the memory occupied by an object, in a Java 15+ JVM, when the 
 * {@code UseEmptySlotsInSupers} option is disabled.
 * <p>For backward compatibility reason, in 15+, the optimization can be disabled through the {@code UseEmptySlotsInSupers} option.
 * (see <a href="https://bugs.openjdk.org/browse/JDK-8237767">https://bugs.openjdk.org/browse/JDK-8237767</a> and 
 * <a href="https://bugs.openjdk.org/browse/JDK-8239016</a>)
 * Unfortunately, when {@code UseEmptySlotsInSupers} is disabled the layout does not match the pre-15 versions and in some cases like 
 * {@code DirectByteBuffer} the layout produced by the JVM does not even group the fields by super-classes making it impossible for this strategy to reproduce it.</p>
 */
final class DoesNotUseEmptySlotInSuperSpecStrategy extends PreJava15SpecStrategy
{
    public DoesNotUseEmptySlotInSuperSpecStrategy(MemoryLayoutSpecification memoryLayout,
                                                  Class<? extends Annotation> contendedClass,
                                                  Optional<MethodHandle> mayBeContendedValueMH) {
        super(memoryLayout, contendedClass, mayBeContendedValueMH);
    }

    @Override
    protected long alignFieldBlock(long sizeOfDeclaredFields)
    {
        // The logic seems to not use the reference size for the alignment but always use a padding of 4.
        return roundTo(sizeOfDeclaredFields, 4);
    }

    @Override
    protected boolean hasSuperClassGap(long size, long sizeOfDeclaredField, long sizeTakenBy8BytesFields)
    {
        return hasGapSmallerThan8Bytes(size) // We can only have a gap if it is smaller than 8 bytes. If we have a gap, due to field block alignment it will always be 4 bytes 
                && (sizeTakenBy8BytesFields > 0 // At least one of the fields need to be larger than 4 bytes to have a gap (so 8 bytes) 
                    || memoryLayout.getReferenceSize() == 8) // or the reference size should be equals to 8 (not sure why)
                && (size != memoryLayout.getObjectHeaderSize() // The 4 byte gap after the header can always be used
                    || hasOnly8BytesFields(sizeOfDeclaredField, sizeTakenBy8BytesFields)); // unless we only have 8 bytes fields
    }

}
