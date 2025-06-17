package ru.cemeterysystem.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ru.cemeterysystem.models.User;

@Getter
public class EditorResignedEvent extends ApplicationEvent {
    private final Long memorialId;
    private final User resignedEditor;
    private final User owner;

    public EditorResignedEvent(Object source, Long memorialId, User resignedEditor, User owner) {
        super(source);
        this.memorialId = memorialId;
        this.resignedEditor = resignedEditor;
        this.owner = owner;
    }
} 