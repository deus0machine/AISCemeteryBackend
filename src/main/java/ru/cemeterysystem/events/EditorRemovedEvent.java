package ru.cemeterysystem.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ru.cemeterysystem.models.User;

@Getter
public class EditorRemovedEvent extends ApplicationEvent {
    private final Long memorialId;
    private final User removedEditor;
    private final User owner;

    public EditorRemovedEvent(Object source, Long memorialId, User removedEditor, User owner) {
        super(source);
        this.memorialId = memorialId;
        this.removedEditor = removedEditor;
        this.owner = owner;
    }
} 