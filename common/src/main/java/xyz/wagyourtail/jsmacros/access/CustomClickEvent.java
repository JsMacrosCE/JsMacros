package xyz.wagyourtail.jsmacros.access;

public class CustomClickEvent implements ICustomClickEvent {
    private final Runnable event;

    public CustomClickEvent(Runnable event) {
        this.event = event;
    }

    @Override
    public int hashCode() {
        return event.hashCode();
    }

    @Override
    public Runnable getEvent() {
        return event;
    }

    @Override
    public Object toPlatformClickEvent() {
        // Platform-specific implementations should override this
        throw new UnsupportedOperationException("Platform-specific implementation required");
    }

    @Override
    public String getActionType() {
        return "CUSTOM";
    }
}
