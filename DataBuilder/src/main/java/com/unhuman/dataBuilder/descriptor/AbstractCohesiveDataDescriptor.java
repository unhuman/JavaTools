package com.unhuman.dataBuilder.descriptor;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * This class is used as a base class for data that must be cohesively generated
 * Example: matching email + firstname + lastname
 *
 * This makes some expectations that related data is generated together
 * So, when a firstname is generated after another firstname, it will restart the seed.
 */
public abstract class AbstractCohesiveDataDescriptor extends AbstractEntityTypeDescriptor {
    private static final Set<Class> inProcessClasses = new HashSet<>();
    private static Long randomSeed = new Random().nextLong();

    public AbstractCohesiveDataDescriptor(String name) {
        super(name);
    }

    // For Jackson
    protected AbstractCohesiveDataDescriptor() {
        super();
    }

    protected Long getRandomSeed() {
        return randomSeed;
    }

    /** for unit tests */
    protected static void reset() {
        inProcessClasses.clear();
    }

    public int getNextRandom(int maxExclusive) {
        // if we have already seen this class and it's another instance of this class
        // then we reset the seed.
        if (inProcessClasses.contains(this.getClass())) {
            randomSeed = new Random().nextLong();
            inProcessClasses.clear();
        }

        inProcessClasses.add(this.getClass());

        return new Random(randomSeed).nextInt(maxExclusive);
    }
}
