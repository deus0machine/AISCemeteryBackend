package ru.cemeterysystem.config;

import ru.cemeterysystem.models.Location;
import java.beans.PropertyEditorSupport;

/**
 * Property Editor для конвертации строки в объект Location
 * Обрабатывает случаи, когда из формы приходит пустая строка или строка с адресом
 */
public class LocationPropertyEditor extends PropertyEditorSupport {
    
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().isEmpty() || "[]".equals(text)) {
            // Если строка пустая или содержит "[]", устанавливаем null
            setValue(null);
        } else {
            // Если строка не пустая, создаем Location только с адресом
            // Координаты будут установлены отдельно, если необходимо
            Location location = new Location();
            location.setAddress(text.trim());
            setValue(location);
        }
    }
    
    @Override
    public String getAsText() {
        Location location = (Location) getValue();
        if (location == null) {
            return "";
        }
        return location.getAddress() != null ? location.getAddress() : "";
    }
} 