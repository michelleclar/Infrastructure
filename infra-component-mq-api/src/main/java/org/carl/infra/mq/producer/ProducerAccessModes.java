package org.carl.infra.mq.producer;

/** Provider-neutral producer access capabilities. */
public final class ProducerAccessModes {

    /** Multiple producers may publish to the same topic. */
    public static final ProducerAccessMode SHARED = new NamedProducerAccessMode("SHARED");

    private ProducerAccessModes() {}

    private record NamedProducerAccessMode(String name) implements ProducerAccessMode {
        @Override
        public String toString() {
            return name;
        }
    }
}
